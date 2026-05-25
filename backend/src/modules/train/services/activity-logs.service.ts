import { Injectable, Inject, forwardRef } from '@nestjs/common';
import { ACTIVITY_LOGS_REPOSITORY } from '../train.constants';
import type { IActivityLogsRepository } from '../repositories/activity-logs.repository.interface';
import { LogStepsDto } from '../dto/log-steps.dto';
import { LogWaterDto } from '../dto/log-water.dto';
import { NotificationsService } from '../../user/services/notifications.service';
import { NotificationType } from '../../user/entities/notification.entity';
import { UsersService } from '../../user/services/users.service';
import { DashboardService } from '../../user/services/dashboard.service';

@Injectable()
export class ActivityLogsService {
  constructor(
    @Inject(ACTIVITY_LOGS_REPOSITORY)
    private readonly repository: IActivityLogsRepository,
    @Inject(forwardRef(() => NotificationsService))
    private readonly notificationsService: NotificationsService,
    @Inject(forwardRef(() => UsersService))
    private readonly usersService: UsersService,
    @Inject(forwardRef(() => DashboardService))
    private readonly dashboardService: DashboardService,
  ) {}

  async logSteps(userId: string, dto: LogStepsDto) {
    await this.repository.upsertSteps(userId, dto.logDate, dto.steps);
    void this.dashboardService.invalidateDailyCache(userId, dto.logDate);
    return this.getByDate(userId, dto.logDate);
  }

  async logWater(userId: string, dto: LogWaterDto) {
    const log = await this.repository.upsertWater(userId, dto.logDate, dto.waterMl);

    const profile = await this.usersService.getHealthProfile(userId);
    const goalMl = profile?.waterGoalMl ?? 2000;
    if (log.waterMl >= goalMl) {
      await this.notificationsService.createOncePerDay(
        userId,
        NotificationType.SYSTEM,
        'Mục tiêu nước hôm nay! 💧',
        `Bạn đã uống đủ ${goalMl}ml nước hôm nay. Tuyệt vời!`,
      );
    }

    void this.dashboardService.invalidateDailyCache(userId, dto.logDate);
    return this.getByDate(userId, dto.logDate);
  }

  async logNote(userId: string, logDate: string, note: string) {
    await this.repository.upsertNote(userId, logDate, note);
    void this.dashboardService.invalidateDailyCache(userId, logDate);
    return this.getByDate(userId, logDate);
  }

  async logSleep(userId: string, logDate: string, sleepHours: number) {
    await this.repository.upsertSleep(userId, logDate, sleepHours);
    void this.dashboardService.invalidateDailyCache(userId, logDate);
    return this.getByDate(userId, logDate);
  }

  async logMood(userId: string, logDate: string, mood: string) {
    await this.repository.upsertMood(userId, logDate, mood);
    void this.dashboardService.invalidateDailyCache(userId, logDate);
    return this.getByDate(userId, logDate);
  }

  async getByDate(userId: string, date: string) {
    const log = await this.repository.findByDate(userId, date);
    if (!log) {
      return { userId, logDate: date, steps: 0, waterMl: 0, note: null, sleepHours: null, mood: null };
    }
    return log;
  }

  async getRange(userId: string, fromDate: string, toDate: string) {
    return this.repository.findRange(userId, fromDate, toDate);
  }

  async getTodaySummary(userId: string) {
    const today = new Date().toISOString().split('T')[0];
    return this.getByDate(userId, today);
  }
}
