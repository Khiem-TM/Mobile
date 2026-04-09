import { Injectable, Inject, Logger } from '@nestjs/common';
import { STREAKS_REPOSITORY } from './streaks.constants';
import type { IStreaksRepository } from './repositories/streaks.repository.interface';
import { Streak } from './entities/streak.entity';
import { StreakType } from '../../common/enums/streak-type.enum';

@Injectable()
export class StreaksService {
  private readonly logger = new Logger(StreaksService.name);

  constructor(
    @Inject(STREAKS_REPOSITORY)
    private readonly repository: IStreaksRepository,
  ) {}

  async getStreaks(userId: string): Promise<Streak[]> {
    return this.repository.findByUser(userId);
  }

  async updateActivity(userId: string, type: StreakType, activityDate: string) {
    const streak = await this.repository.findOrCreate(userId, type);
    const lastDate = streak.last_activity_date;
    const today = activityDate;

    if (lastDate === today) {
      return streak;
    }

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().split('T')[0];

    let newCurrent = streak.current_streak;
    if (lastDate === yesterdayStr) {
      newCurrent += 1;
    } else {
      newCurrent = 1;
    }

    const newLongest = Math.max(streak.longest_streak, newCurrent);

    return this.repository.updateStreak(streak.id, newCurrent, newLongest, today);
  }

  async resetExpiredStreaks() {
    this.logger.log('Starting nightly streaks expiration check...');
    const types: StreakType[] = [StreakType.LOGIN, StreakType.CALORIE_GOAL, StreakType.WORKOUT];

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().split('T')[0];

    for (const type of types) {
      const allStreaks = await this.repository.findAllByType(type);
      for (const streak of allStreaks) {
        if (
          streak.last_activity_date &&
          streak.last_activity_date < yesterdayStr &&
          streak.current_streak > 0
        ) {
          this.logger.log(
            `Resetting ${type} streak for user ${streak.user_id} (Last activity: ${streak.last_activity_date})`,
          );
          await this.repository.updateStreak(
            streak.id,
            0,
            streak.longest_streak,
            streak.last_activity_date,
          );
        }
      }
    }
  }
}
