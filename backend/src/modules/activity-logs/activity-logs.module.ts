import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ActivityLog } from './entities/activity-log.entity';
import { ActivityLogsService } from './activity-logs.service';
import { ActivityLogsController } from './activity-logs.controller';
import { ActivityLogsRepository } from './repositories/activity-logs.repository';
import { ACTIVITY_LOGS_REPOSITORY } from './activity-logs.constants';
import { NotificationsModule } from '../notifications/notifications.module';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([ActivityLog]), NotificationsModule, UsersModule],
  controllers: [ActivityLogsController],
  providers: [
    ActivityLogsService,
    {
      provide: ACTIVITY_LOGS_REPOSITORY,
      useClass: ActivityLogsRepository,
    },
  ],
  exports: [ActivityLogsService],
})
export class ActivityLogsModule {}
