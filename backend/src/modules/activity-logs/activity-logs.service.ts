import { Injectable, Inject } from '@nestjs/common';
import { ACTIVITY_LOGS_REPOSITORY } from './activity-logs.constants';
import type { IActivityLogsRepository } from './repositories/activity-logs.repository.interface';
import { LogStepsDto } from './dto/log-steps.dto';
import { LogCaloriesBurnedDto } from './dto/log-calories-burned.dto';
import { LogWaterDto } from './dto/log-water.dto';

@Injectable()
export class ActivityLogsService {
  constructor(
    @Inject(ACTIVITY_LOGS_REPOSITORY)
    private readonly repository: IActivityLogsRepository,
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
    return this.repository.upsertWater(userId, dto.logDate, dto.waterMl);
  }

  // Hàm trả về thông tin log user theo ngày
  async getByDate(userId: string, date: string) {
    const log = await this.repository.findByDate(userId, date);
    if (!log) {
      return {
        userId,
        logDate: date,
        steps: 0,
        caloriesBurned: 0,
        activeMinutes: 0,
        waterMl: 0,
        exerciseNotes: null,
      };
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
