import { Injectable, Inject, Logger } from '@nestjs/common';
import { STREAKS_REPOSITORY } from '../user.constants';
import type { IStreaksRepository } from '../repositories/streaks.repository.interface';
import { Streak } from '../entities/streak.entity';
import { StreakType } from '../../../common/enums/streak-type.enum';
import { NotificationsService } from './notifications.service';
import { NotificationType } from '../entities/notification.entity';
import { RedisService } from '../../support/redis/redis.service';

const STREAK_MILESTONES = [3, 7, 14, 30];
// TTL slightly over 24h to handle timezone edge cases
const STREAK_DEDUP_TTL = 90_000;

@Injectable()
export class StreaksService {
  private readonly logger = new Logger(StreaksService.name);

  constructor(
    @Inject(STREAKS_REPOSITORY)
    private readonly repository: IStreaksRepository,
    private readonly notificationsService: NotificationsService,
    private readonly redisService: RedisService,
  ) {}

  async getStreaks(userId: string): Promise<Streak[]> {
    const streaks = await this.repository.findByUser(userId);
    return streaks.map((streak) => this.formatStreak(streak)) as Streak[];
  }

  private formatStreak(streak: Streak) {
    return {
      ...streak,
      currentStreak: streak.current_streak,
      maxStreak: streak.longest_streak,
      lastActivityDate: streak.last_activity_date,
    };
  }

  private toDayNumber(date: string): number {
    const [year, month, day] = date.split('-').map(Number);
    return Math.floor(Date.UTC(year, month - 1, day) / 86_400_000);
  }

  async recomputeFromActivityDates(
    userId: string,
    type: StreakType,
    activityDates: string[],
  ) {
    const streak = await this.repository.findOrCreate(userId, type);
    const dates = [...new Set(activityDates)].sort();

    if (!dates.length) {
      const updated = await this.repository.updateStreak(streak.id, 0, 0, null);
      return this.formatStreak(updated);
    }

    let longest = 1;
    let runLength = 1;
    for (let i = 1; i < dates.length; i++) {
      const diff = this.toDayNumber(dates[i]) - this.toDayNumber(dates[i - 1]);
      runLength = diff === 1 ? runLength + 1 : 1;
      longest = Math.max(longest, runLength);
    }

    const lastDate = dates[dates.length - 1];
    const updated = await this.repository.updateStreak(streak.id, runLength, longest, lastDate);
    return this.formatStreak(updated);
  }

  async updateActivity(userId: string, type: StreakType, activityDate: string) {
    // Skip DB round-trip if already processed today (idempotency guard)
    const dedupKey = `streak:done:${userId}:${type}:${activityDate}`;
    const isFirst = await this.redisService.setnx(dedupKey, '1', STREAK_DEDUP_TTL);
    if (!isFirst) return;

    const streak = await this.repository.findOrCreate(userId, type);
    const lastDate = streak.last_activity_date;
    const today = activityDate;

    if (lastDate === today) {
      return this.formatStreak(streak);
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

    const updated = await this.repository.updateStreak(streak.id, newCurrent, newLongest, today);

    if (STREAK_MILESTONES.includes(newCurrent)) {
      await this.notificationsService.create(
        userId,
        NotificationType.STREAK,
        `Streak ${newCurrent} ngay! 🔥`,
        `Ban da duy tri streak ${type} lien tuc ${newCurrent} ngay. Tuyet voi!`,
      );
    }

    return this.formatStreak(updated);
  }

  async resetExpiredStreaks() {
    this.logger.log('Starting nightly streaks expiration check...');
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().split('T')[0];
    const updated = await this.repository.resetExpiredBefore(yesterdayStr);
    this.logger.log(`Reset ${updated} expired streaks.`);
  }
}
