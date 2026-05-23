import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, Repository } from 'typeorm';
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
  ) {}

  // ─── Public ────────────────────────────────────────────────────────────────

  async getApprovedBlogs(page = 1, limit = 20, search?: string, tag?: string) {
    const key = `cache:blogs:list:${page}:${limit}:${search ?? ''}:${tag ?? ''}`;
    const cached = await this.redisService.getJson<{ items: Blog[]; total: number; page: number; limit: number }>(key);
    if (cached) return cached;

    const qb = this.blogRepo
      .createQueryBuilder('blog')
      .leftJoinAndSelect('blog.authorUser', 'author')
      .where('blog.status = :status', { status: 'approved' })
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
    if (cached) {
      // Still increment view count even when serving from cache
      void this.blogRepo.increment({ id }, 'viewCount', 1);
      return cached;
    }

    const blog = await this.blogRepo.findOne({
      where: { id, status: 'approved' },
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
      const status = dto.status === 'draft' ? 'draft' : 'approved';

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
    if (dto.status !== 'draft') {
      await this.invalidateBlogListCache();
    }
    return this.findWithBlocks(savedId);
  }

  async updateUserBlog(userId: string, blogId: string, dto: UpdateBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id: blogId } });
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
      blog.status = keepAsDraft ? 'draft' : 'approved';
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
      where: { id: blogId },
      relations: ['blocks'],
    });
    if (!blog) throw new NotFoundException('Blog not found');
    if (blog.author_id !== userId) throw new ForbiddenException('Not your blog');

    await this.cleanupBlogAssets(blog);
    await this.blogRepo.delete(blogId);
    await this.invalidateBlogListCache();
    await this.redisService.del(`cache:blogs:one:${blogId}`);
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
    if (status && !['approved', 'draft'].includes(status)) {
      return { items: [], total: 0, page, limit };
    }

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
  }

  async adminUpdateBlog(id: string, dto: UpdateBlogDto) {
    const blog = await this.blogRepo.findOne({ where: { id }, relations: ['blocks'] });
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
  }

  async adminDeleteBlog(id: string) {
    const blog = await this.blogRepo.findOne({ where: { id }, relations: ['blocks'] });
    if (!blog) throw new NotFoundException('Blog not found');
    await this.cleanupBlogAssets(blog);
    await this.blogRepo.delete(id);
    await this.invalidateBlogListCache();
    await this.redisService.del(`cache:blogs:one:${id}`);
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
