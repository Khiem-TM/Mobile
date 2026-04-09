import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { Streak } from './entities/streak.entity';
import { StreaksService } from './streaks.service';
import { StreaksController } from './streaks.controller';
import { StreaksScheduler } from './streaks.scheduler';
import { StreaksRepository } from './repositories/streaks.repository';
import { STREAKS_REPOSITORY } from './streaks.constants';
import { NotificationsModule } from '../notifications/notifications.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Streak]),
    ScheduleModule.forRoot(),
    NotificationsModule,
  ],
  controllers: [StreaksController],
  providers: [
    StreaksService,
    StreaksScheduler,
    {
      provide: STREAKS_REPOSITORY,
      useClass: StreaksRepository,
    },
  ],
  exports: [StreaksService],
})
export class StreaksModule {}
