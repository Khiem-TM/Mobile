import { Streak } from '../entities/streak.entity';
import { StreakType } from '../../../common/enums/streak-type.enum';

export interface IStreaksRepository {
  findOrCreate(userId: string, type: StreakType): Promise<Streak>;
  findByUser(userId: string): Promise<Streak[]>;
  updateStreak(streakId: string, current: number, longest: number, lastDate: string | null): Promise<Streak>;
  findAllByType(type: StreakType): Promise<Streak[]>;
  resetExpiredBefore(yesterday: string): Promise<number>;
}
