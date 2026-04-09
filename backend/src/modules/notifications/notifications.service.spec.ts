import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException, ForbiddenException } from '@nestjs/common';
import { NotificationsService } from './notifications.service';
import { Notification, NotificationType } from './entities/notification.entity';

const mockNotification = {
  id: 'notif-uuid',
  userId: 'user-uuid',
  type: NotificationType.SYSTEM,
  title: 'Test Title',
  body: 'Test body',
  isRead: false,
  createdAt: new Date(),
};

const makeRepo = () => ({
  find: jest.fn(),
  findOne: jest.fn(),
  save: jest.fn(),
  create: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  count: jest.fn(),
});

describe('NotificationsService', () => {
  let service: NotificationsService;
  let repo: ReturnType<typeof makeRepo>;

  beforeEach(async () => {
    repo = makeRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        NotificationsService,
        { provide: getRepositoryToken(Notification), useValue: repo },
      ],
    }).compile();

    service = module.get(NotificationsService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getForUser ───────────────────────────────────────────────────────────

  describe('getForUser', () => {
    it('should return all notifications for user', async () => {
      repo.find.mockResolvedValue([mockNotification]);
      const result = await service.getForUser('user-uuid');
      expect(result).toHaveLength(1);
      expect(repo.find).toHaveBeenCalledWith(
        expect.objectContaining({ where: { userId: 'user-uuid' } }),
      );
    });

    it('should filter only unread when unreadOnly=true', async () => {
      repo.find.mockResolvedValue([mockNotification]);
      await service.getForUser('user-uuid', true);
      expect(repo.find).toHaveBeenCalledWith(
        expect.objectContaining({ where: { userId: 'user-uuid', isRead: false } }),
      );
    });
  });

  // ─── markAsRead ───────────────────────────────────────────────────────────

  describe('markAsRead', () => {
    it('should mark notification as read', async () => {
      repo.findOne.mockResolvedValue({ ...mockNotification });
      repo.save.mockResolvedValue({ ...mockNotification, isRead: true });

      const result = await service.markAsRead('user-uuid', 'notif-uuid');
      expect(result.isRead).toBe(true);
    });

    it('should throw NotFoundException if notification not found', async () => {
      repo.findOne.mockResolvedValue(null);
      await expect(service.markAsRead('user-uuid', 'bad-id')).rejects.toThrow(NotFoundException);
    });

    it('should throw ForbiddenException if notification belongs to another user', async () => {
      repo.findOne.mockResolvedValue({ ...mockNotification, userId: 'other-uuid' });
      await expect(service.markAsRead('user-uuid', 'notif-uuid')).rejects.toThrow(ForbiddenException);
    });
  });

  // ─── markAllAsRead ────────────────────────────────────────────────────────

  describe('markAllAsRead', () => {
    it('should update all unread notifications and return count', async () => {
      repo.update.mockResolvedValue({ affected: 3 });
      const result = await service.markAllAsRead('user-uuid');
      expect(result.updated).toBe(3);
      expect(repo.update).toHaveBeenCalledWith(
        { userId: 'user-uuid', isRead: false },
        { isRead: true },
      );
    });
  });

  // ─── deleteNotification ───────────────────────────────────────────────────

  describe('deleteNotification', () => {
    it('should delete notification if user owns it', async () => {
      repo.findOne.mockResolvedValue(mockNotification);
      repo.delete.mockResolvedValue({});
      await service.deleteNotification('user-uuid', 'notif-uuid');
      expect(repo.delete).toHaveBeenCalledWith('notif-uuid');
    });

    it('should throw NotFoundException if not found', async () => {
      repo.findOne.mockResolvedValue(null);
      await expect(service.deleteNotification('user-uuid', 'bad-id')).rejects.toThrow(NotFoundException);
    });

    it('should throw ForbiddenException if not owned', async () => {
      repo.findOne.mockResolvedValue({ ...mockNotification, userId: 'other-uuid' });
      await expect(service.deleteNotification('user-uuid', 'notif-uuid')).rejects.toThrow(ForbiddenException);
    });
  });

  // ─── create ──────────────────────────────────────────────────────────────

  describe('create', () => {
    it('should create and save notification', async () => {
      repo.create.mockReturnValue(mockNotification);
      repo.save.mockResolvedValue(mockNotification);

      const result = await service.create('user-uuid', NotificationType.STREAK, 'Title', 'Body');
      expect(result.type).toBe(NotificationType.SYSTEM);
      expect(repo.create).toHaveBeenCalledWith(
        expect.objectContaining({ userId: 'user-uuid', type: NotificationType.STREAK }),
      );
    });
  });

  // ─── getUnreadCount ───────────────────────────────────────────────────────

  describe('getUnreadCount', () => {
    it('should return count of unread notifications', async () => {
      repo.count.mockResolvedValue(5);
      const result = await service.getUnreadCount('user-uuid');
      expect(result).toBe(5);
      expect(repo.count).toHaveBeenCalledWith({ where: { userId: 'user-uuid', isRead: false } });
    });
  });
});
