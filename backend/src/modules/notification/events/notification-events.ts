/**
 * Hợp đồng event giữa Blog (producer) và Notification (consumer) qua Kafka.
 * Blog chỉ phát event; Notification module áp dụng rule + lưu in-app + đẩy push.
 */
export const BLOG_NOTIFICATIONS_TOPIC = 'blog.notifications';

export enum BlogNotificationType {
  LIKE = 'blog.like.created',
  COMMENT = 'blog.comment.created',
}

export interface BlogNotificationEvent {
  /** uuid duy nhất 1 lần phát — dùng cho idempotency phía consumer */
  eventId: string;
  type: BlogNotificationType;
  occurredAt: string;
  blogId: string;
  blogTitle: string;
  /** người nhận thông báo = tác giả blog */
  recipientUserId: string;
  /** người thực hiện hành động (like/comment) */
  actorId: string;
  actorName: string;
}
