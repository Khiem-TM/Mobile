import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, In, Repository } from 'typeorm';
import { Blog } from '../entities/blog.entity';
import { BlogBlock } from '../entities/blog-block.entity';
import { BlogLike } from '../entities/blog-like.entity';
import { BlogComment } from '../entities/blog-comment.entity';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { CreateBlogDto } from '../dto/create-blog.dto';
import { UpdateBlogDto } from '../dto/update-blog.dto';
import { RejectBlogDto } from '../dto/reject-blog.dto';
import { CreateBlogBlockDto } from '../dto/create-blog-block.dto';
import { CreateCommentDto } from '../dto/create-comment.dto';
import { BatchBlogActionDto, BatchRejectBlogDto } from '../dto/batch-blog.dto';

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
  ) {}

  // ─── Public ────────────────────────────────────────────────────────────────

  async getApprovedBlogs(page = 1, limit = 20, search?: string, tag?: string) {
    const qb = this.blogRepo
      .createQueryBuilder('blog')
      .leftJoinAndSelect('blog.authorUser', 'author')
      .where('blog.status = :status', { status: 'approved' })
      .orderBy('blog.createdAt', 'DESC');

    if (search) {
      // Feature 3: search by title OR author display_name
      qb.andWhere(
        '(LOWER(blog.title) LIKE :search OR LOWER(author.display_name) LIKE :search)',
        { search: `%${search.toLowerCase()}%` },
      );
    }

    if (tag) {
      // Feature 2: tag filter — PostgreSQL simple-array stored as comma-separated text
      qb.andWhere(":tag = ANY(string_to_array(blog.tags, ','))", { tag });
    }

    const [items, total] = await qb
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return { items, total, page, limit };
  }

  async getOneBlog(id: string) {
    const blog = await this.blogRepo.findOne({
      where: { id, status: 'approved' },
      relations: ['blocks', 'authorUser'],
      order: { blocks: { order: 'ASC' } },
    });
    if (!blog) throw new NotFoundException('Blog not found');

    // Feature 6: atomic view count increment (fire-and-forget)
    void this.blogRepo.increment({ id }, 'viewCount', 1);

    return blog;
  }

  // Feature 2: list all unique tags from approved blogs
  async getAllTags(): Promise<string[]> {
    const rows = await this.blogRepo
      .createQueryBuilder('blog')
      .select('blog.tags', 'tags')
      .where('blog.status = :s', { s: 'approved' })
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
    return Array.from(tagSet).sort();
  }

  // ─── User ──────────────────────────────────────────────────────────────────

  async createUserBlog(userId: string, dto: CreateBlogDto) {
    return this.dataSource.transaction(async (manager) => {
      // Feature 4: draft mode
      const status = dto.status === 'draft' ? 'draft' : 'pending';

      const blog = manager.create(Blog, {
        title: dto.title,
        author_id: userId,
        status,
        // Feature 2: normalize empty array → null
        tags: dto.tags?.length ? dto.tags : null,
      });

      if (dto.thumbnailBase64) {
        const result = await this.cloudinaryService.uploadBase64(
          dto.thumbnailBase64,
          'blog-thumbnails',
        );
        blog.thumbnailUrl = result.url;
        blog.thumbnailPublicId = result.publicId;
      }

      const saved = await manager.save(Blog, blog);

      if (dto.blocks?.length) {
        const blocks = await this.buildBlocks(saved.id, dto.blocks);
        await manager.save(BlogBlock, blocks);
      }

      return this.findWithBlocks(saved.id);
    });
  }

  async updateUserBlog(userId: string, blogId: string, dto: UpdateBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId } });
    if (!blog) throw new NotFoundException('Blog not found');
    if (blog.author_id !== userId) throw new ForbiddenException('Not your blog');

    return this.dataSource.transaction(async (manager) => {
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
      }

      if (dto.title) blog.title = dto.title;

      // Feature 2: update tags if provided
      if (dto.tags !== undefined) {
        blog.tags = dto.tags.length ? dto.tags : null;
      }

      // Feature 4: draft/pending status logic
      if (blog.status === 'draft') {
        // Only switch to pending when user explicitly publishes
        if (dto.status === 'pending') {
          blog.status = 'pending';
          blog.rejectionReason = null;
        }
        // Content-only edits keep the blog as draft
      } else {
        // approved/rejected/pending → any content change re-submits for review
        blog.status = 'pending';
        blog.rejectionReason = null;
      }

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

      return this.findWithBlocks(blogId);
    });
  }

  async deleteUserBlog(userId: string, blogId: string) {
    const blog = await this.blogRepo.findOne({
      where: { id: blogId },
      relations: ['blocks'],
    });
    if (!blog) throw new NotFoundException('Blog not found');
    if (blog.author_id !== userId) throw new ForbiddenException('Not your blog');

    await this.cleanupBlogAssets(blog);
    await this.blogRepo.delete(blogId);
  }

  async getUserBlogs(userId: string, page = 1, limit = 20) {
    const [items, total] = await this.blogRepo.findAndCount({
      where: { author_id: userId },
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async toggleLike(userId: string, blogId: string) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId, status: 'approved' } });
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

    return { liked: !existing };
  }

  async isLiked(userId: string, blogId: string) {
    const like = await this.likeRepo.findOne({ where: { user_id: userId, blog_id: blogId } });
    return { liked: !!like };
  }

  // ─── Comments (Feature 1) ──────────────────────────────────────────────────

  async getComments(blogId: string, page = 1, limit = 20) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId, status: 'approved' } });
    if (!blog) throw new NotFoundException('Blog not found');

    const [items, total] = await this.commentRepo.findAndCount({
      where: { blog_id: blogId },
      relations: ['authorUser'],
      order: { createdAt: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async addComment(userId: string, blogId: string, dto: CreateCommentDto) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId, status: 'approved' } });
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

    return this.commentRepo.findOne({
      where: { id: comment.id },
      relations: ['authorUser'],
    });
  }

  async deleteOwnComment(userId: string, commentId: string) {
    const comment = await this.commentRepo.findOne({ where: { id: commentId } });
    if (!comment) throw new NotFoundException('Comment not found');
    if (comment.author_id !== userId) throw new ForbiddenException('Not your comment');

    await this.dataSource.transaction(async (manager) => {
      await manager.delete(BlogComment, { id: commentId });
      await manager.decrement(Blog, { id: comment.blog_id }, 'commentCount', 1);
    });
  }

  async adminDeleteComment(commentId: string) {
    const comment = await this.commentRepo.findOne({ where: { id: commentId } });
    if (!comment) throw new NotFoundException('Comment not found');

    await this.dataSource.transaction(async (manager) => {
      await manager.delete(BlogComment, { id: commentId });
      await manager.decrement(Blog, { id: comment.blog_id }, 'commentCount', 1);
    });
  }

  // ─── Admin ─────────────────────────────────────────────────────────────────

  async adminGetBlogs(page = 1, limit = 20, status?: string, tag?: string) {
    const qb = this.blogRepo
      .createQueryBuilder('blog')
      .leftJoinAndSelect('blog.authorUser', 'author')
      .orderBy('blog.createdAt', 'DESC');

    if (status) {
      qb.andWhere('blog.status = :status', { status });
    }

    if (tag) {
      qb.andWhere(":tag = ANY(string_to_array(blog.tags, ','))", { tag });
    }

    const [items, total] = await qb
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return { items, total, page, limit };
  }

  async adminCreateBlog(dto: CreateBlogDto) {
    return this.dataSource.transaction(async (manager) => {
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
      }

      const saved = await manager.save(Blog, blog);

      if (dto.blocks?.length) {
        const blocks = await this.buildBlocks(saved.id, dto.blocks);
        await manager.save(BlogBlock, blocks);
      }

      return this.findWithBlocks(saved.id);
    });
  }

  async adminUpdateBlog(id: string, dto: UpdateBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id }, relations: ['blocks'] });
    if (!blog) throw new NotFoundException('Blog not found');

    return this.dataSource.transaction(async (manager) => {
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
      }

      if (dto.title) blog.title = dto.title;
      if (dto.tags !== undefined) {
        blog.tags = dto.tags.length ? dto.tags : null;
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

      return this.findWithBlocks(id);
    });
  }

  async adminApproveBlog(id: string) {
    const blog = await this.blogRepo.findOne({ where: { id } });
    if (!blog) throw new NotFoundException('Blog not found');
    blog.status = 'approved';
    blog.rejectionReason = null;
    return this.blogRepo.save(blog);
  }

  async adminRejectBlog(id: string, dto: RejectBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id } });
    if (!blog) throw new NotFoundException('Blog not found');
    blog.status = 'rejected';
    blog.rejectionReason = dto.reason ?? null;
    return this.blogRepo.save(blog);
  }

  async adminDeleteBlog(id: string) {
    const blog = await this.blogRepo.findOne({ where: { id }, relations: ['blocks'] });
    if (!blog) throw new NotFoundException('Blog not found');
    await this.cleanupBlogAssets(blog);
    await this.blogRepo.delete(id);
  }

  async getPendingCount() {
    return this.blogRepo.count({ where: { status: 'pending' } });
  }

  // Feature 5: batch approve/reject
  async adminBatchApprove(dto: BatchBlogActionDto) {
    await this.blogRepo.update(
      { id: In(dto.ids) },
      { status: 'approved', rejectionReason: null },
    );
    return { updated: dto.ids.length };
  }

  async adminBatchReject(dto: BatchRejectBlogDto) {
    await this.blogRepo.update(
      { id: In(dto.ids) },
      { status: 'rejected', rejectionReason: dto.reason ?? null },
    );
    return { updated: dto.ids.length };
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

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
      where: { id },
      relations: ['blocks', 'authorUser'],
      order: { blocks: { order: 'ASC' } },
    });
  }
}
