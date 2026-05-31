import { Inject, Injectable, Logger } from '@nestjs/common';
import { NotificationsService } from '../../user/services/notifications.service';
import { NotificationType } from '../../user/entities/notification.entity';
import { DeviceTokenService } from './device-token.service';
import { PUSH_SENDER } from './push-sender.service';
import type { PushSender } from './push-sender.service';

interface DispatchOptions {
  /** Chỉ tạo 1 in-app/ngày (dùng cho nhắc nhở định kỳ). */
  oncePerDay?: boolean;
}

/**
 * Điểm gom thông báo: lưu in-app (tái dùng NotificationsService) + đẩy push.
 * Lỗi push KHÔNG chặn việc lưu in-app.
 */
@Injectable()
export class NotificationDispatchService {
  private readonly logger = new Logger(NotificationDispatchService.name);

  constructor(
    private readonly notificationsService: NotificationsService,
    private readonly deviceTokenService: DeviceTokenService,
    @Inject(PUSH_SENDER) private readonly pushSender: PushSender,
  ) {}

  async dispatch(
    userId: string,
    type: NotificationType,
    title: string,
    body: string,
    data: Record<string, string>,
    opts: DispatchOptions = {},
  ): Promise<void> {
    // 1) Lưu in-app
    if (opts.oncePerDay) {
      await this.notificationsService.createOncePerDay(userId, type, title, body);
    } else {
      await this.notificationsService.create(userId, type, title, body);
    }

    // 2) Đẩy push (best-effort)
    try {
      const tokens = await this.deviceTokenService.getTokensForUser(userId);
      if (tokens.length) {
        await this.pushSender.send(tokens, { title, body, data });
      }
    } catch (e) {
      this.logger.warn(`Push thất bại cho user ${userId}: ${(e as Error).message}`);
    }
  }
}
