import { Logger, Provider } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { FIREBASE_APP } from '../providers/firebase-admin.provider';
import { DeviceTokenService } from './device-token.service';

/** DI token cho cài đặt PushSender đang dùng (Firebase hoặc Stub). */
export const PUSH_SENDER = 'PUSH_SENDER';

export interface PushPayload {
  title: string;
  body: string;
  /** data dùng để điều hướng deep-link phía mobile (route, id, ...). KHÔNG chứa PII. */
  data: Record<string, string>;
}

export interface PushSender {
  send(tokens: string[], payload: PushPayload): Promise<void>;
}

/** Gửi FCM thật qua Firebase Admin; tự dọn token chết theo error code FCM. */
export class FirebasePushSender implements PushSender {
  private readonly logger = new Logger(FirebasePushSender.name);

  constructor(
    private readonly app: admin.app.App,
    private readonly deviceTokenService: DeviceTokenService,
  ) {}

  async send(tokens: string[], payload: PushPayload): Promise<void> {
    if (!tokens.length) return;
    const message: admin.messaging.MulticastMessage = {
      tokens,
      notification: { title: payload.title, body: payload.body },
      data: payload.data,
      android: { priority: 'high' },
    };

    const res = await this.app.messaging().sendEachForMulticast(message);
    if (res.failureCount > 0) {
      const dead: string[] = [];
      res.responses.forEach((r, i) => {
        if (!r.success) {
          const code = r.error?.code ?? '';
          if (
            code === 'messaging/registration-token-not-registered' ||
            code === 'messaging/invalid-argument' ||
            code === 'messaging/invalid-registration-token'
          ) {
            dead.push(tokens[i]);
          } else {
            this.logger.warn(`FCM send error: ${code}`);
          }
        }
      });
      if (dead.length) await this.deviceTokenService.pruneTokens(dead);
    }
    this.logger.debug(
      `FCM sent: ok=${res.successCount} fail=${res.failureCount}`,
    );
  }
}

/** Không có service-account: chỉ log payload (dev / chưa nạp Firebase key). */
export class StubPushSender implements PushSender {
  private readonly logger = new Logger(StubPushSender.name);

  async send(tokens: string[], payload: PushPayload): Promise<void> {
    this.logger.log(
      `[STUB push] tokens=${tokens.length} title="${payload.title}" data=${JSON.stringify(
        payload.data,
      )}`,
    );
  }
}

/** Chọn cài đặt PushSender theo việc có/không Firebase app. */
export const pushSenderProvider: Provider = {
  provide: PUSH_SENDER,
  inject: [FIREBASE_APP, DeviceTokenService],
  useFactory: (
    app: admin.app.App | null,
    deviceTokenService: DeviceTokenService,
  ): PushSender =>
    app
      ? new FirebasePushSender(app, deviceTokenService)
      : new StubPushSender(),
};
