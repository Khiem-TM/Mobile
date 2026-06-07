import { ForbiddenException, NotFoundException } from '@nestjs/common';
import { IsNull } from 'typeorm';
import { BlogComment } from '../entities/blog-comment.entity';
import { BlogService } from './blog.service';

describe('BlogService updateOwnComment', () => {
  const createService = (manager: Record<string, jest.Mock>) => {
    const dataSource = {
      transaction: jest.fn(async (operation) => operation(manager)),
    };

    const service = new BlogService(
      {} as never,
      {} as never,
      {} as never,
      {} as never,
      dataSource as never,
      {} as never,
      {} as never,
      {} as never,
      {} as never,
      {} as never,
    );

    return { service, dataSource };
  };

  it('updates and returns the comment inside one locked transaction', async () => {
    const comment = {
      id: 'comment-1',
      blog_id: 'blog-1',
      author_id: 'user-1',
      content: 'Old content',
    };
    const updated = { ...comment, content: 'Updated content' };
    const manager = {
      findOne: jest.fn().mockResolvedValueOnce(comment).mockResolvedValueOnce(updated),
      save: jest.fn().mockResolvedValue(updated),
    };
    const { service, dataSource } = createService(manager);

    await expect(
      service.updateOwnComment('user-1', 'blog-1', 'comment-1', {
        content: 'Updated content',
      }),
    ).resolves.toEqual(updated);

    expect(dataSource.transaction).toHaveBeenCalledTimes(1);
    expect(manager.findOne).toHaveBeenNthCalledWith(1, BlogComment, {
      where: {
        id: 'comment-1',
        blog_id: 'blog-1',
        deletedAt: IsNull(),
      },
      lock: { mode: 'pessimistic_write' },
    });
    expect(manager.save).toHaveBeenCalledWith(
      BlogComment,
      expect.objectContaining({ content: 'Updated content' }),
    );
  });

  it('rejects a user who does not own the comment', async () => {
    const manager = {
      findOne: jest.fn().mockResolvedValue({
        id: 'comment-1',
        blog_id: 'blog-1',
        author_id: 'another-user',
        content: 'Content',
      }),
      save: jest.fn(),
    };
    const { service } = createService(manager);

    await expect(
      service.updateOwnComment('user-1', 'blog-1', 'comment-1', {
        content: 'Updated content',
      }),
    ).rejects.toBeInstanceOf(ForbiddenException);
    expect(manager.save).not.toHaveBeenCalled();
  });

  it('does not expose comments from another blog or deleted comments', async () => {
    const manager = {
      findOne: jest.fn().mockResolvedValue(null),
      save: jest.fn(),
    };
    const { service } = createService(manager);

    await expect(
      service.updateOwnComment('user-1', 'wrong-blog', 'comment-1', {
        content: 'Updated content',
      }),
    ).rejects.toBeInstanceOf(NotFoundException);
    expect(manager.save).not.toHaveBeenCalled();
  });
});
