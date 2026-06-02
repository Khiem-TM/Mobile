import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, IsNull, Repository } from 'typeorm';
import { Blog } from '../entities/blog.entity';
import { BlogBlock } from '../entities/blog-block.entity';
import { BlogLike } from '../entities/blog-like.entity';
import { BlogComment } from '../entities/blog-comment.entity';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { CreateBlogDto } from '../dto/create-blog.dto';
import { UpdateBlogDto } from '../dto/update-blog.dto';
import { CreateBlogBlockDto } from '../dto/create-blog-block.dto';
import { CreateCommentDto } from '../dto/create-comment.dto';
import { RedisService } from '../../support/redis/redis.service';
import { randomUUID } from 'crypto';
import { UsersService } from '../../user/services/users.service';
import {
  AdminAuditContext,
  AuditLogService,
} from '../../admin/services/audit-log.service';
import { BlogEventPublisher } from './blog-event.publisher';
import {
  BLOG_NOTIFICATIONS_TOPIC,
  BlogNotificationEvent,
  BlogNotificationType,
} from '../../notification/events/notification-events';

const TTL = {
  BLOG_LIST: 120,  // 2 min — approved blog list
  BLOG_ONE: 300,   // 5 min — individual blog detail
  BLOG_TAGS: 1800, // 30 min — tags rarely change
};

@Injectable()
export class BlogService {
  constructor(
    @InjectRepository(Blog)
    private readonly blogRepo: Repository<Blog>,

    @InjectRepository(BlogBlock)
    private readonly blockRepo: Repository<BlogBlock>,

    @InjectRepository(BlogLike)
    private readonly likeRepo: Repository<BlogLike>,

    @InjectRepository(BlogComment)
    private readonly commentRepo: Repository<BlogComment>,

    private readonly dataSource: DataSource,
    private readonly cloudinaryService: CloudinaryService,
    private readonly redisService: RedisService,
    private readonly usersService: UsersService,
    private readonly blogEventPublisher: BlogEventPublisher,
    private readonly auditLogService: AuditLogService,
  ) {}

  /** Phát event thông báo khi có tương tác mới (like/comment) tới TÁC GIẢ blog. */
  private async publishBlogInteraction(
    type: BlogNotificationType,
    blog: Blog,
    actorId: string,
  ): Promise<void> {
    // Bỏ qua nếu blog không có tác giả (admin blog) hoặc tự tương tác bài mình.
    if (!blog.author_id || blog.author_id === actorId) return;
    let actorName = 'Một người dùng';
    try {
      const actor = await this.usersService.findById(actorId);
      if (actor?.display_name) actorName = actor.display_name;
    } catch {
      // tên không lấy được -> dùng mặc định, không chặn luồng
    }
    const event: BlogNotificationEvent = {
      eventId: randomUUID(),
      type,
      occurredAt: new Date().toISOString(),
      blogId: blog.id,
      blogTitle: blog.title,
      recipientUserId: blog.author_id,
      actorId,
      actorName,
    };
    this.blogEventPublisher.emit(BLOG_NOTIFICATIONS_TOPIC, event);
  }

  // ─── Public ────────────────────────────────────────────────────────────────

  async getApprovedBlogs(page = 1, limit = 20, search?: string, tag?: string) {
    const key = `cache:blogs:list:${page}:${limit}:${search ?? ''}:${tag ?? ''}`;
    const cached = await this.redisService.getJson<{ items: Blog[]; total: number; page: number; limit: number }>(key);
    if (cached) return cached;

    const qb = this.blogRepo
      .createQueryBuilder('blog')
      .leftJoinAndSelect('blog.authorUser', 'author')
      .where('blog.status = :status', { status: 'approved' })
      .andWhere('blog.deleted_at IS NULL')
      .orderBy('blog.createdAt', 'DESC');

    if (search) {
      qb.andWhere(
        '(LOWER(blog.title) LIKE :search OR LOWER(author.display_name) LIKE :search)',
        { search: `%${search.toLowerCase()}%` },
      );
    }

    if (tag) {
      qb.andWhere(":tag = ANY(string_to_array(blog.tags, ','))", { tag });
    }

    const [items, total] = await qb
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    const result = { items, total, page, limit };
    await this.redisService.setJson(key, result, TTL.BLOG_LIST);
    return result;
  }

  async getOneBlog(id: string) {
    const key = `cache:blogs:one:${id}`;
    const cached = await this.redisService.getJson<Blog>(key);
    if (cached && cached.status === 'approved' && !cached.deletedAt) {
      // Still increment view count even when serving from cache
      void this.blogRepo.increment({ id }, 'viewCount', 1);
      return cached;
    }

    const blog = await this.blogRepo.findOne({
      where: { id, status: 'approved', deletedAt: IsNull() },
      relations: ['blocks', 'authorUser'],
      order: { blocks: { order: 'ASC' } },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    void this.blogRepo.increment({ id }, 'viewCount', 1);
    await this.redisService.setJson(key, blog, TTL.BLOG_ONE);
    return blog;
  }

  // Feature 2: list all unique tags from approved blogs
  async getAllTags(): Promise<string[]> {
    const key = 'cache:blogs:tags';
    const cached = await this.redisService.getJson<string[]>(key);
    if (cached) return cached;

    const rows = await this.blogRepo
      .createQueryBuilder('blog')
      .select('blog.tags', 'tags')
      .where('blog.status = :s', { s: 'approved' })
      .andWhere('blog.deleted_at IS NULL')
      .andWhere('blog.tags IS NOT NULL')
      .andWhere("blog.tags != ''")
      .getRawMany<{ tags: string }>();

    const tagSet = new Set<string>();
    for (const row of rows) {
      row.tags.split(',').forEach((t) => {
        const v = t.trim();
        if (v) tagSet.add(v);
      });
    }
    const tags = Array.from(tagSet).sort();
    await this.redisService.setJson(key, tags, TTL.BLOG_TAGS);
    return tags;
  }

  // Called after any user/admin blog mutation to keep public cache consistent.
  private async invalidateBlogListCache(): Promise<void> {
    await Promise.all([
      this.redisService.delByPattern('cache:blogs:list:*'),
      this.redisService.del('cache:blogs:tags'),
    ]);
  }

  // ─── User ──────────────────────────────────────────────────────────────────

  async createUserBlog(userId: string, dto: CreateBlogDto) {
    const savedId = await this.dataSource.transaction(async (manager) => {
      const status = dto.status === 'draft' ? 'draft' : 'pending';

      const blog = manager.create(Blog, {
        title: dto.title,
        author_id: userId,
        status,
        tags: dto.tags?.length ? dto.tags : null,
      });

      if (dto.thumbnailBase64) {
        const result = await this.cloudinaryService.uploadBase64(
          dto.thumbnailBase64,
          'blog-thumbnails',
        );
        blog.thumbnailUrl = result.url;
        blog.thumbnailPublicId = result.publicId;
      } else if (dto.thumbnailUrl) {
        blog.thumbnailUrl = dto.thumbnailUrl;
      }

      const saved = await manager.save(Blog, blog);

      if (dto.blocks?.length) {
        const blocks = await this.buildBlocks(saved.id, dto.blocks);
        await manager.save(BlogBlock, blocks);
      }

      return saved.id;
    });
    await this.invalidateBlogListCache();
    return this.findWithBlocks(savedId);
  }

  async updateUserBlog(userId: string, blogId: string, dto: UpdateBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId, deletedAt: IsNull() } });
    if (!blog) throw new NotFoundException('Blog not found');
    if (blog.author_id !== userId) throw new ForbiddenException('Not your blog');

    await this.dataSource.transaction(async (manager) => {
      if (dto.thumbnailBase64) {
        if (blog.thumbnailPublicId) {
          await this.cloudinaryService.deleteFile(blog.thumbnailPublicId);
        }
        const result = await this.cloudinaryService.uploadBase64(
          dto.thumbnailBase64,
          'blog-thumbnails',
        );
        blog.thumbnailUrl = result.url;
        blog.thumbnailPublicId = result.publicId;
      } else if (dto.thumbnailUrl !== undefined) {
        if (blog.thumbnailPublicId) {
          await this.cloudinaryService.deleteFile(blog.thumbnailPublicId);
          blog.thumbnailPublicId = null;
        }
        blog.thumbnailUrl = dto.thumbnailUrl || null;
      }

      if (dto.title) blog.title = dto.title;

      if (dto.tags !== undefined) {
        blog.tags = dto.tags.length ? dto.tags : null;
      }

      const keepAsDraft = (dto.status as string) === 'draft';
      blog.status = keepAsDraft ? 'draft' : 'pending';
      blog.rejectionReason = null;

      await manager.save(Blog, blog);

      if (dto.blocks !== undefined) {
        const existing = await manager.find(BlogBlock, { where: { blog_id: blogId } });
        for (const block of existing) {
          if (block.imagePublicId) {
            await this.cloudinaryService.deleteFile(block.imagePublicId);
          }
        }
        await manager.delete(BlogBlock, { blog_id: blogId });

        if (dto.blocks.length) {
          const blocks = await this.buildBlocks(blogId, dto.blocks);
          await manager.save(BlogBlock, blocks);
        }
      }
    });
    await this.invalidateBlogListCache();
    await this.redisService.del(`cache:blogs:one:${blogId}`);
    return this.findWithBlocks(blogId);
  }

  async deleteUserBlog(userId: string, blogId: string) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId, deletedAt: IsNull() },
      relations: ['blocks'],
    });
    if (!blog) throw new NotFoundException('Blog not found');
    if (blog.author_id !== userId) throw new ForbiddenException('Not your blog');

    blog.deletedAt = new Date();
    await this.blogRepo.save(blog);
    await this.invalidateBlogListCache();
    await this.redisService.del(`cache:blogs:one:${blogId}`);
  }

  async getUserBlogs(userId: string, page = 1, limit = 20) {
    const [items, total] = await this.blogRepo.findAndCount({
      where: { author_id: userId, deletedAt: IsNull() },
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async toggleLike(userId: string, blogId: string) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId, status: 'approved', deletedAt: IsNull() },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    const existing = await this.likeRepo.findOne({
      where: { user_id: userId, blog_id: blogId },
    });

    await this.dataSource.transaction(async (manager) => {
      if (existing) {
        await manager.delete(BlogLike, { user_id: userId, blog_id: blogId });
        await manager.decrement(Blog, { id: blogId }, 'likesCount', 1);
      } else {
        await manager.save(BlogLike, manager.create(BlogLike, { user_id: userId, blog_id: blogId }));
        await manager.increment(Blog, { id: blogId }, 'likesCount', 1);
      }
    });

    // Chỉ thông báo khi tạo lượt thích mới (không thông báo khi bỏ thích).
    if (!existing) {
      await this.publishBlogInteraction(BlogNotificationType.LIKE, blog, userId);
    }

    return { liked: !existing };
  }

  async isLiked(userId: string, blogId: string) {
    const like = await this.likeRepo.findOne({ where: { user_id: userId, blog_id: blogId } });
    return { liked: !!like };
  }

  // ─── Comments (Feature 1) ──────────────────────────────────────────────────

  async getComments(blogId: string, page = 1, limit = 20) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId, status: 'approved', deletedAt: IsNull() },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    const [items, total] = await this.commentRepo.findAndCount({
      where: { blog_id: blogId, deletedAt: IsNull() },
      relations: ['authorUser'],
      order: { createdAt: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async addComment(userId: string, blogId: string, dto: CreateCommentDto) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId, status: 'approved', deletedAt: IsNull() },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    const comment = this.commentRepo.create({
      blog_id: blogId,
      author_id: userId,
      content: dto.content,
    });

    await this.dataSource.transaction(async (manager) => {
      await manager.save(BlogComment, comment);
      await manager.increment(Blog, { id: blogId }, 'commentCount', 1);
    });

    await this.publishBlogInteraction(BlogNotificationType.COMMENT, blog, userId);

    return this.commentRepo.findOne({
      where: { id: comment.id },
      relations: ['authorUser'],
    });
  }

  async deleteOwnComment(userId: string, commentId: string) {
    const comment = await this.commentRepo.findOne({
      where: { id: commentId, deletedAt: IsNull() },
    });
    if (!comment) throw new NotFoundException('Comment not found');
    if (comment.author_id !== userId) throw new ForbiddenException('Not your comment');

    await this.dataSource.transaction(async (manager) => {
      await manager.update(BlogComment, { id: commentId }, { deletedAt: new Date() });
      await manager.decrement(Blog, { id: comment.blog_id }, 'commentCount', 1);
    });
  }

  async adminDeleteComment(
    commentId: string,
    context?: AdminAuditContext,
    expectedBlogId?: string,
  ) {
    const operation = async () => {
      const comment = await this.commentRepo.findOne({
        where: { id: commentId, deletedAt: IsNull() },
      });
      if (!comment || (expectedBlogId && comment.blog_id !== expectedBlogId)) {
        throw new NotFoundException('Comment not found');
      }

      await this.dataSource.transaction(async (manager) => {
        await manager.update(BlogComment, { id: commentId }, { deletedAt: new Date() });
        await manager.decrement(Blog, { id: comment.blog_id }, 'commentCount', 1);
      });
      await this.invalidateBlogListCache();
      await this.redisService.del(`cache:blogs:one:${comment.blog_id}`);
      return {
        id: comment.id,
        blogId: comment.blog_id,
        authorId: comment.author_id,
      };
    };

    if (context) {
      return this.auditMutation(
        context,
        'admin.blog_comment.delete',
        'blog_comment',
        commentId,
        { blogId: expectedBlogId ?? null },
        operation,
      );
    }

    await operation();
  }

  // ─── Admin ─────────────────────────────────────────────────────────────────

  async adminGetBlogs(
    page = 1,
    limit = 20,
    filters: {
      status?: string;
      tag?: string;
      search?: string;
      authorId?: string;
      createdFrom?: string;
      createdTo?: string;
    } = {},
  ) {
    const qb = this.blogRepo
      .createQueryBuilder('blog')
      .leftJoinAndSelect('blog.authorUser', 'author')
      .where('blog.deleted_at IS NULL')
      .orderBy('blog.createdAt', 'DESC');

    if (filters.status) {
      qb.andWhere('blog.status = :status', { status: filters.status });
    }

    if (filters.search) {
      qb.andWhere(
        '(LOWER(blog.title) LIKE :search OR LOWER(author.display_name) LIKE :search)',
        { search: `%${filters.search.toLowerCase()}%` },
      );
    }

    if (filters.authorId) {
      qb.andWhere('blog.author_id = :authorId', { authorId: filters.authorId });
    }

    if (filters.tag) {
      qb.andWhere(":tag = ANY(string_to_array(blog.tags, ','))", { tag: filters.tag });
    }

    if (filters.createdFrom) {
      qb.andWhere('blog.createdAt >= :createdFrom', {
        createdFrom: filters.createdFrom,
      });
    }
    if (filters.createdTo) {
      qb.andWhere('blog.createdAt <= :createdTo', {
        createdTo: `${filters.createdTo} 23:59:59`,
      });
    }

    const safeLimit = Math.min(Math.max(Number(limit) || 20, 1), 100);
    const [items, total] = await qb
      .skip((page - 1) * safeLimit)
      .take(safeLimit)
      .getManyAndCount();

    return { items, total, page, limit: safeLimit };
  }

  async adminGetBlogById(id: string) {
    const blog = await this.blogRepo.findOne({
      where: { id, deletedAt: IsNull() },
      relations: ['blocks', 'authorUser'],
      order: { blocks: { order: 'ASC' } },
    });
    if (!blog) throw new NotFoundException('Blog not found');
    return blog;
  }

  async adminGetBlogComments(blogId: string, page = 1, limit = 20) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId, deletedAt: IsNull() },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    const safeLimit = this.clampLimit(limit);
    const [items, total] = await this.commentRepo.findAndCount({
      where: { blog_id: blogId, deletedAt: IsNull() },
      relations: ['authorUser'],
      order: { createdAt: 'ASC' },
      skip: (page - 1) * safeLimit,
      take: safeLimit,
    });
    return { items, total, page, limit: safeLimit };
  }

  async adminCreateBlog(dto: CreateBlogDto, context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.create', 'blog', null, {
      title: dto.title,
    }, async () => {
      const savedId = await this.dataSource.transaction(async (manager) => {
        const blog = manager.create(Blog, {
          title: dto.title,
          author_id: null,
          status: 'approved',
          tags: dto.tags?.length ? dto.tags : null,
        });

        if (dto.thumbnailBase64) {
          const result = await this.cloudinaryService.uploadBase64(
            dto.thumbnailBase64,
            'blog-thumbnails',
          );
          blog.thumbnailUrl = result.url;
          blog.thumbnailPublicId = result.publicId;
        } else if (dto.thumbnailUrl) {
          blog.thumbnailUrl = dto.thumbnailUrl;
        }

        const saved = await manager.save(Blog, blog);

        if (dto.blocks?.length) {
          const blocks = await this.buildBlocks(saved.id, dto.blocks);
          await manager.save(BlogBlock, blocks);
        }

        return saved.id;
      });
      await this.invalidateBlogListCache();
      return this.findWithBlocks(savedId);
    });
  }

  async adminUpdateBlog(id: string, dto: UpdateBlogDto, context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.update', 'blog', id, {
      fields: Object.keys(dto),
    }, async () => {
      const blog = await this.blogRepo.findOne({
        where: { id, deletedAt: IsNull() },
        relations: ['blocks'],
      });
      if (!blog) throw new NotFoundException('Blog not found');

      await this.dataSource.transaction(async (manager) => {
        if (dto.thumbnailBase64) {
          if (blog.thumbnailPublicId) {
            await this.cloudinaryService.deleteFile(blog.thumbnailPublicId);
          }
          const result = await this.cloudinaryService.uploadBase64(
            dto.thumbnailBase64,
            'blog-thumbnails',
          );
          blog.thumbnailUrl = result.url;
          blog.thumbnailPublicId = result.publicId;
        } else if (dto.thumbnailUrl !== undefined) {
          if (blog.thumbnailPublicId) {
            await this.cloudinaryService.deleteFile(blog.thumbnailPublicId);
            blog.thumbnailPublicId = null;
          }
          blog.thumbnailUrl = dto.thumbnailUrl || null;
        }

        if (dto.title) blog.title = dto.title;
        if (dto.tags !== undefined) {
          blog.tags = dto.tags.length ? dto.tags : null;
        }
        if (dto.status !== undefined) {
          blog.status = dto.status;
          blog.rejectionReason = null;
        }
        await manager.save(Blog, blog);

        if (dto.blocks !== undefined) {
          for (const block of blog.blocks) {
            if (block.imagePublicId) {
              await this.cloudinaryService.deleteFile(block.imagePublicId);
            }
          }
          await manager.delete(BlogBlock, { blog_id: id });

          if (dto.blocks.length) {
            const blocks = await this.buildBlocks(id, dto.blocks);
            await manager.save(BlogBlock, blocks);
          }
        }
      });
      await this.invalidateBlogListCache();
      await this.redisService.del(`cache:blogs:one:${id}`);
      return this.findWithBlocks(id);
    });
  }

  async adminApproveBlog(id: string, context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.approve', 'blog', id, null, async () => {
      const blog = await this.blogRepo.findOne({ where: { id, deletedAt: IsNull() } });
      if (!blog) throw new NotFoundException('Blog not found');
      blog.status = 'approved';
      blog.rejectionReason = null;
      const saved = await this.blogRepo.save(blog);
      await this.invalidateBlogListCache();
      await this.redisService.del(`cache:blogs:one:${id}`);
      return saved;
    });
  }

  async adminRejectBlog(id: string, reason?: string, context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.reject', 'blog', id, {
      reason: reason ?? null,
    }, async () => {
      const blog = await this.blogRepo.findOne({ where: { id, deletedAt: IsNull() } });
      if (!blog) throw new NotFoundException('Blog not found');
      blog.status = 'rejected';
      blog.rejectionReason = reason ?? null;
      const saved = await this.blogRepo.save(blog);
      await this.invalidateBlogListCache();
      await this.redisService.del(`cache:blogs:one:${id}`);
      return saved;
    });
  }

  async adminBatchApproveBlogs(ids: string[], context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.batch_approve', 'blog', null, {
      ids,
    }, async () => {
      await this.blogRepo
        .createQueryBuilder()
        .update(Blog)
        .set({ status: 'approved', rejectionReason: null })
        .where('id IN (:...ids)', { ids })
        .andWhere('deleted_at IS NULL')
        .execute();
      await this.invalidateBlogListCache();
      await Promise.all(ids.map((id) => this.redisService.del(`cache:blogs:one:${id}`)));
      return { updated: ids.length };
    });
  }

  async adminBatchRejectBlogs(ids: string[], reason?: string, context?: AdminAuditContext) {
    return this.auditMutation(context, 'admin.blog.batch_reject', 'blog', null, {
      ids,
      reason: reason ?? null,
    }, async () => {
      await this.blogRepo
        .createQueryBuilder()
        .update(Blog)
        .set({ status: 'rejected', rejectionReason: reason ?? null })
        .where('id IN (:...ids)', { ids })
        .andWhere('deleted_at IS NULL')
        .execute();
      await this.invalidateBlogListCache();
      await Promise.all(ids.map((id) => this.redisService.del(`cache:blogs:one:${id}`)));
      return { updated: ids.length };
    });
  }

  async adminDeleteBlog(id: string, context?: AdminAuditContext) {
    await this.auditMutation(context, 'admin.blog.delete', 'blog', id, null, async () => {
      const blog = await this.blogRepo.findOne({ where: { id, deletedAt: IsNull() } });
      if (!blog) throw new NotFoundException('Blog not found');
      blog.deletedAt = new Date();
      await this.blogRepo.save(blog);
      await this.invalidateBlogListCache();
      await this.redisService.del(`cache:blogs:one:${id}`);
    });
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

  private async auditMutation<T>(
    context: AdminAuditContext | undefined,
    action: string,
    targetType: string,
    targetId: string | null,
    metadata: Record<string, unknown> | null,
    operation: () => Promise<T>,
  ): Promise<T> {
    try {
      const result = await operation();
      await this.auditLogService.recordFromContext(context, {
        action,
        targetType,
        targetId: this.resolveTargetId(targetId, result),
        metadata,
      });
      return result;
    } catch (error) {
      await this.auditLogService.recordFromContext(context, {
        action,
        targetType,
        targetId,
        status: 'failure',
        metadata,
        errorMessage: error instanceof Error ? error.message : 'Admin blog action failed',
      });
      throw error;
    }
  }

  private resolveTargetId(targetId: string | null, result: unknown): string | null {
    if (targetId) return targetId;
    if (result && typeof result === 'object' && 'id' in result) {
      const id = (result as { id?: unknown }).id;
      return typeof id === 'string' ? id : null;
    }
    return null;
  }

  private clampLimit(limit: number): number {
    return Math.min(Math.max(Number(limit) || 20, 1), 100);
  }

  private async buildBlocks(blogId: string, dtos: CreateBlogBlockDto[]): Promise<BlogBlock[]> {
    const blocks: BlogBlock[] = [];
    for (const dto of dtos) {
      const block = new BlogBlock();
      block.blog_id = blogId;
      block.order = dto.order;
      block.type = dto.type;

      if (dto.type === 'text') {
        block.textContent = dto.text_content ?? null;
      } else {
        if (dto.image_base64) {
          const result = await this.cloudinaryService.uploadBase64(
            dto.image_base64,
            'blog-blocks',
          );
          block.imageUrl = result.url;
          block.imagePublicId = result.publicId;
        } else if (dto.image_url) {
          block.imageUrl = dto.image_url;
        }
      }
      blocks.push(block);
    }
    return blocks;
  }

  private async cleanupBlogAssets(blog: Blog) {
    if (blog.thumbnailPublicId) {
      await this.cloudinaryService.deleteFile(blog.thumbnailPublicId);
    }
    for (const block of blog.blocks ?? []) {
      if (block.imagePublicId) {
        await this.cloudinaryService.deleteFile(block.imagePublicId);
      }
    }
  }

  private async findWithBlocks(id: string) {
    return this.blogRepo.findOne({
      where: { id, deletedAt: IsNull() },
      relations: ['blocks', 'authorUser'],
      order: { blocks: { order: 'ASC' } },
    });
  }
}
