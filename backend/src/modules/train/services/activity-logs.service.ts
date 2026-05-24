import { Injectable, Inject, forwardRef } from '@nestjs/common';
import { ACTIVITY_LOGS_REPOSITORY } from '../train.constants';
import type { IActivityLogsRepository } from '../repositories/activity-logs.repository.interface';
import { LogStepsDto } from '../dto/log-steps.dto';
import { LogCaloriesBurnedDto } from '../dto/log-calories-burned.dto';
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

  async logCaloriesBurned(userId: string, dto: LogCaloriesBurnedDto) {
    await this.repository.upsertCaloriesBurned(
      userId,
      dto.logDate,
      dto.caloriesBurned,
      dto.activeMinutes,
      dto.exerciseNotes,
    );
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

  async getByDate(userId: string, date: string) {
    const log = await this.repository.findByDate(userId, date);
    if (!log) {
      return {
        userId,
        logDate: date,
        steps: 0,
        caloriesBurned: 0,
        manualCaloriesBurned: 0,
        workoutCaloriesBurned: 0,
        activeMinutes: 0,
        waterMl: 0,
        exerciseNotes: null,
      };
    }
    return this.withTotalCalories(log);
  }

  async getRange(userId: string, fromDate: string, toDate: string) {
    const logs = await this.repository.findRange(userId, fromDate, toDate);
    return logs.map((log) => this.withTotalCalories(log));
  }

  async getTodaySummary(userId: string) {
    const today = new Date().toISOString().split('T')[0];
    return this.getByDate(userId, today);
  }

  async setWorkoutCalories(userId: string, date: string, calories: number): Promise<void> {
    await this.repository.upsertWorkoutCalories(userId, date, calories);
    void this.dashboardService.invalidateDailyCache(userId, date);
  }

  private withTotalCalories(log: any) {
    const manualCaloriesBurned = Number(log.caloriesBurned ?? 0);
    const workoutCaloriesBurned = Number(log.workoutCaloriesBurned ?? 0);
    return {
      ...log,
      manualCaloriesBurned,
      workoutCaloriesBurned,
      caloriesBurned: Number((manualCaloriesBurned + workoutCaloriesBurned).toFixed(2)),
    };
  }
}
