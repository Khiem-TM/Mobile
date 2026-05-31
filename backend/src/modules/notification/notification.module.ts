import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserModule } from '../user/user.module';
import { SupportModule } from '../support/support.module';
// entities (forFeature có thể lặp ở nhiều module; entity vẫn autoLoad toàn cục)
import { DeviceToken } from './entities/device-token.entity';
import { User } from '../user/entities/user.entity';
import { MealLog } from '../food/entities/meal-log.entity';
import { ActivityLog } from '../train/entities/activity-log.entity';
import { TrainingSession } from '../train/entities/training-session.entity';
// controllers
import { DeviceTokenController } from './controllers/device-token.controller';
import { BlogNotificationConsumer } from './consumers/blog-notification.consumer';
// services / providers
import { DeviceTokenService } from './services/device-token.service';
import { NotificationDispatchService } from './services/notification-dispatch.service';
import { InactivityReminderScheduler } from './schedulers/inactivity-reminder.scheduler';
import { firebaseAppProvider } from './providers/firebase-admin.provider';
import { pushSenderProvider } from './services/push-sender.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      DeviceToken,
      User,
      MealLog,
      ActivityLog,
      TrainingSession,
    ]),
    UserModule, // re-use NotificationsService (lưu in-app)
    SupportModule, // RedisService (idempotency)
  ],
  controllers: [DeviceTokenController, BlogNotificationConsumer],
  providers: [
    DeviceTokenService,
    NotificationDispatchService,
    InactivityReminderScheduler,
    firebaseAppProvider,
    pushSenderProvider,
  ],
  exports: [DeviceTokenService, NotificationDispatchService],
})
export class NotificationModule {}
