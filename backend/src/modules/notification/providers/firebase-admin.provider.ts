import { Logger, Provider } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { existsSync, readFileSync } from 'fs';
import { isAbsolute, resolve } from 'path';
import * as admin from 'firebase-admin';

/** DI token cho Firebase Admin app (hoặc null nếu chưa cấu hình service-account). */
export const FIREBASE_APP = 'FIREBASE_APP';

const logger = new Logger('FirebaseAdmin');

/**
 * Khởi tạo Firebase Admin từ service-account JSON.
 * Thiếu file/đường dẫn -> trả về null để PushSender rơi về StubPushSender (log payload),
 * giúp backend chạy được khi chưa nạp key thật.
 */
export const firebaseAppProvider: Provider = {
  provide: FIREBASE_APP,
  inject: [ConfigService],
  useFactory: (config: ConfigService): admin.app.App | null => {
    const path = config.get<string>('FIREBASE_SERVICE_ACCOUNT_PATH');
    if (!path) {
      logger.warn(
        'FIREBASE_SERVICE_ACCOUNT_PATH trống -> dùng StubPushSender (không gửi push thật).',
      );
      return null;
    }
    const abs = isAbsolute(path) ? path : resolve(process.cwd(), path);
    if (!existsSync(abs)) {
      logger.warn(
        `Không thấy service-account tại ${abs} -> dùng StubPushSender.`,
      );
      return null;
    }
    try {
      const serviceAccount = JSON.parse(readFileSync(abs, 'utf8'));
      const app = admin.apps.length
        ? admin.app()
        : admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
          });
      logger.log('Firebase Admin đã khởi tạo (FCM v1).');
      return app;
    } catch (e) {
      logger.error(
        `Khởi tạo Firebase Admin thất bại: ${(e as Error).message} -> dùng StubPushSender.`,
      );
      return null;
    }
  },
};
