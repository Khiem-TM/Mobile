import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import {
  Notification,
  NotificationType,
} from '../entities/notification.entity';

@Injectable()
export class NotificationsService {
  constructor(
    @InjectRepository(Notification)
    private readonly repo: Repository<Notification>,
  ) {}

  async getForUser(
    userId: string,
    unreadOnly = false,
  ): Promise<Notification[]> {
    const where: any = { userId };
    if (unreadOnly) where.isRead = false;
    return this.repo.find({
      where,
      order: { createdAt: 'DESC' },
      take: 50,
    });
  }

  async markAsRead(
    userId: string,
    notificationId: string,
  ): Promise<Notification> {
    const notification = await this.repo.findOne({
      where: { id: notificationId },
    });
    if (!notification) throw new NotFoundException('Notification not found');
    if (notification.userId !== userId)
      throw new ForbiddenException('Access denied');
    notification.isRead = true;
    return this.repo.save(notification);
  }

  async markAllAsRead(userId: string): Promise<{ updated: number }> {
    const result = await this.repo.update(
      { userId, isRead: false },
      { isRead: true },
    );
    return { updated: result.affected ?? 0 };
  }

  async deleteNotification(
    userId: string,
    notificationId: string,
  ): Promise<void> {
    const notification = await this.repo.findOne({
      where: { id: notificationId },
    });
    if (!notification) throw new NotFoundException('Notification not found');
    if (notification.userId !== userId)
      throw new ForbiddenException('Access denied');
    await this.repo.delete(notificationId);
  }

  async create(
    userId: string,
    type: NotificationType,
    title: string,
    body: string,
  ): Promise<Notification> {
    const notification = this.repo.create({ userId, type, title, body });
    return this.repo.save(notification);
  }

  async getUnreadCount(userId: string): Promise<number> {
    return this.repo.count({ where: { userId, isRead: false } });
  }

  async createMany(
    userIds: string[],
    type: NotificationType,
    title: string,
    body: string,
  ): Promise<void> {
    const notifications = userIds.map((userId) =>
      this.repo.create({ userId, type, title, body }),
    );
    await this.repo.save(notifications);
  }

  async createOncePerDay(
    userId: string,
    type: NotificationType,
    title: string,
    body: string,
  ): Promise<void> {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const existing = await this.repo
      .createQueryBuilder('notification')
      .where('notification.userId = :userId', { userId })
      .andWhere('notification.type = :type', { type })
      .andWhere('notification.title = :title', { title })
      .andWhere('DATE(notification.createdAt) = DATE(NOW())')
      .getOne();

    if (!existing) {
      await this.repo.save(this.repo.create({ userId, type, title, body }));
    }
  }
}
