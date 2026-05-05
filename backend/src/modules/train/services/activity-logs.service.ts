import { Injectable, Inject, forwardRef } from '@nestjs/common';
import { ACTIVITY_LOGS_REPOSITORY } from '../train.constants';
import type { IActivityLogsRepository } from '../repositories/activity-logs.repository.interface';
import { LogStepsDto } from '../dto/log-steps.dto';
import { LogCaloriesBurnedDto } from '../dto/log-calories-burned.dto';
import { LogWaterDto } from '../dto/log-water.dto';
import { NotificationsService } from '../../user/services/notifications.service';
import { NotificationType } from '../../user/entities/notification.entity';
import { UsersService } from '../../user/services/users.service';

@Injectable()
export class ActivityLogsService {
  constructor(
    @Inject(ACTIVITY_LOGS_REPOSITORY)
    private readonly repository: IActivityLogsRepository,
    @Inject(forwardRef(() => NotificationsService))
    private readonly notificationsService: NotificationsService,
    @Inject(forwardRef(() => UsersService))
    private readonly usersService: UsersService,
  ) {}

  async logSteps(userId: string, dto: LogStepsDto) {
    return this.repository.upsertSteps(userId, dto.logDate, dto.steps);
  }

  async logCaloriesBurned(userId: string, dto: LogCaloriesBurnedDto) {
    return this.repository.upsertCaloriesBurned(
      userId,
      dto.logDate,
      dto.caloriesBurned,
      dto.activeMinutes,
      dto.exerciseNotes,
    );
  }

  async logWater(userId: string, dto: LogWaterDto) {
    const log = await this.repository.upsertWater(userId, dto.logDate, dto.waterMl);

    const profile = await this.usersService.getHealthProfile(userId);
    const goalMl = profile?.waterGoalMl ?? 2000;
    if (log.waterMl >= goalMl) {
      await this.notificationsService.create(
        userId,
        NotificationType.GOAL_PROGRESS,
        'Da dat muc tieu nuoc uong! 💧',
        `Ban da uong ${log.waterMl}ml — dat muc tieu ${goalMl}ml hom nay!`,
      );
    }

    return log;
  }

  async getByDate(userId: string, date: string) {
    const log = await this.repository.findByDate(userId, date);
    if (!log) {
      return { userId, logDate: date, steps: 0, caloriesBurned: 0, activeMinutes: 0, waterMl: 0, exerciseNotes: null };
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

  async setWorkoutCalories(userId: string, date: string, calories: number): Promise<void> {
    await this.repository.upsertWorkoutCalories(userId, date, calories);
  }
}
