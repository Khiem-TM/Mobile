import { Controller, Logger } from '@nestjs/common';
import { EventPattern, Payload } from '@nestjs/microservices';
import { RedisService } from '../../support/redis/redis.service';
import { NotificationType } from '../../user/entities/notification.entity';
import { NotificationDispatchService } from '../services/notification-dispatch.service';
import {
  BLOG_NOTIFICATIONS_TOPIC,
  BlogNotificationType,
} from '../events/notification-events';
import type { BlogNotificationEvent } from '../events/notification-events';

/**
 * Consumer Kafka cho event tương tác Blog (like/comment).
 * Áp dụng idempotency theo eventId (Redis SETNX) rồi gom qua NotificationDispatchService.
 */
@Controller()
export class BlogNotificationConsumer {
  private readonly logger = new Logger(BlogNotificationConsumer.name);
  private static readonly DEDUP_TTL = 60 * 60 * 24; // 24h

  constructor(
    private readonly dispatch: NotificationDispatchService,
    private readonly redisService: RedisService,
  ) {}

  @EventPattern(BLOG_NOTIFICATIONS_TOPIC)
  async handle(@Payload() event: BlogNotificationEvent): Promise<void> {
    if (!event?.eventId || !event.recipientUserId) return;

    // Idempotency: chỉ xử lý lần đầu cho mỗi eventId.
    const fresh = await this.redisService
      .getClient()
      .set(
        `notif:evt:${event.eventId}`,
        '1',
        'EX',
        BlogNotificationConsumer.DEDUP_TTL,
        'NX',
      );
    if (fresh === null) {
      this.logger.debug(`Bỏ qua event trùng ${event.eventId}`);
      return;
    }

    const { title, body } = this.buildContent(event);
    await this.dispatch.dispatch(
      event.recipientUserId,
      NotificationType.SYSTEM,
      title,
      body,
      { route: 'blog_detail', id: event.blogId, type: event.type },
    );
    this.logger.log(
      `Đã xử lý ${event.type} blog=${event.blogId} -> user=${event.recipientUserId}`,
    );
  }

  private buildContent(event: BlogNotificationEvent): {
    title: string;
    body: string;
  } {
    const blog = event.blogTitle ? `"${event.blogTitle}"` : 'bài viết của bạn';
    if (event.type === BlogNotificationType.LIKE) {
      return {
        title: 'Có lượt thích mới',
        body: `${event.actorName} đã thích ${blog}.`,
      };
    }
    return {
      title: 'Có bình luận mới',
      body: `${event.actorName} đã bình luận về ${blog}.`,
    };
  }
}
