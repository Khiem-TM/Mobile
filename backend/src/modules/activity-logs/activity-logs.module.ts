import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ActivityLog } from './entities/activity-log.entity';
import { ActivityLogsService } from './activity-logs.service';
import { ActivityLogsController } from './activity-logs.controller';
import { ActivityLogsRepository } from './repositories/activity-logs.repository';
import { ACTIVITY_LOGS_REPOSITORY } from './activity-logs.constants';

@Module({
  imports: [TypeOrmModule.forFeature([ActivityLog])],
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
