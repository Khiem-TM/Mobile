import { NotFoundException } from '@nestjs/common';
import { BlogService } from './blog.service';
import { Blog } from '../entities/blog.entity';

describe('BlogService moderation', () => {
  function createService() {
    const blogRepo = {
      findOne: jest.fn(),
      findAndCount: jest.fn(),
    };
    const blockRepo = {};
    const likeRepo = {};
    const commentRepo = {
      findOne: jest.fn(),
      findAndCount: jest.fn(),
    };
    const manager = {
      create: jest.fn((_: unknown, data: Record<string, unknown>) => data),
      save: jest.fn((entity: unknown, data?: Record<string, unknown>) => {
        if (entity === Blog) return Promise.resolve({ id: 'blog-id', ...data });
        return Promise.resolve(data ?? entity);
      }),
      update: jest.fn().mockResolvedValue({ affected: 1 }),
      decrement: jest.fn().mockResolvedValue({ affected: 1 }),
    };
    const dataSource = {
      transaction: jest.fn(async (callback: (manager: typeof manager) => Promise<unknown>) =>
        callback(manager),
      ),
    };
    const redisService = {
      getJson: jest.fn(),
      setJson: jest.fn(),
      del: jest.fn(),
      delByPattern: jest.fn(),
    };
    const auditLogService = { recordFromContext: jest.fn() };
    const service = new BlogService(
      blogRepo as any,
      blockRepo as any,
      likeRepo as any,
      commentRepo as any,
      dataSource as any,
      { uploadBase64: jest.fn(), deleteFile: jest.fn() } as any,
      redisService as any,
      { findById: jest.fn() } as any,
      { emit: jest.fn() } as any,
      auditLogService as any,
    );

    return { service, blogRepo, commentRepo, dataSource, manager, redisService, auditLogService };
  }

  it('creates user blogs as pending unless saved as draft', async () => {
    const { service, blogRepo, manager } = createService();
    blogRepo.findOne.mockResolvedValue({ id: 'blog-id', status: 'pending' });

    await service.createUserBlog('user-id', {
      title: 'Community post',
      status: 'approved',
    });

    expect(manager.create).toHaveBeenCalledWith(
      Blog,
      expect.objectContaining({
        author_id: 'user-id',
        status: 'pending',
      }),
    );
  });

  it('returns admin blog detail without requiring approved status', async () => {
    const { service, blogRepo } = createService();
    blogRepo.findOne.mockResolvedValue({ id: 'blog-id', status: 'pending' });

    const result = await service.adminGetBlogById('blog-id');

    expect(result).toEqual({ id: 'blog-id', status: 'pending' });
    expect(blogRepo.findOne).toHaveBeenCalledWith(
      expect.objectContaining({
        relations: ['blocks', 'authorUser'],
      }),
    );
  });

  it('lists non-deleted comments for admin moderation', async () => {
    const { service, blogRepo, commentRepo } = createService();
    blogRepo.findOne.mockResolvedValue({ id: 'blog-id' });
    commentRepo.findAndCount.mockResolvedValue([[{ id: 'comment-id' }], 1]);

    const result = await service.adminGetBlogComments('blog-id', 1, 20);

    expect(result).toEqual({
      items: [{ id: 'comment-id' }],
      total: 1,
      page: 1,
      limit: 20,
    });
    expect(commentRepo.findAndCount).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { blog_id: 'blog-id', deletedAt: expect.any(Object) },
        relations: ['authorUser'],
      }),
    );
  });

  it('soft-deletes admin comments and records audit logs', async () => {
    const { service, commentRepo, manager, redisService, auditLogService } = createService();
    commentRepo.findOne.mockResolvedValue({
      id: 'comment-id',
      blog_id: 'blog-id',
      author_id: 'author-id',
    });

    await service.adminDeleteComment(
      'comment-id',
      { actorUserId: 'admin-id', actorEmail: 'admin@test.com' },
      'blog-id',
    );

    expect(manager.update).toHaveBeenCalled();
    expect(manager.decrement).toHaveBeenCalledWith(Blog, { id: 'blog-id' }, 'commentCount', 1);
    expect(redisService.delByPattern).toHaveBeenCalledWith('cache:blogs:list:*');
    expect(redisService.del).toHaveBeenCalledWith('cache:blogs:one:blog-id');
    expect(auditLogService.recordFromContext).toHaveBeenCalledWith(
      expect.objectContaining({ actorUserId: 'admin-id' }),
      expect.objectContaining({
        action: 'admin.blog_comment.delete',
        targetType: 'blog_comment',
        targetId: 'comment-id',
      }),
    );
  });

  it('rejects comment deletion when the comment does not belong to the blog', async () => {
    const { service, commentRepo } = createService();
    commentRepo.findOne.mockResolvedValue({ id: 'comment-id', blog_id: 'other-blog' });

    await expect(service.adminDeleteComment('comment-id', undefined, 'blog-id')).rejects.toBeInstanceOf(
      NotFoundException,
    );
  });
});
