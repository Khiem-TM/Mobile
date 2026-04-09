import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Streak } from '../entities/streak.entity';
import { IStreaksRepository } from './streaks.repository.interface';
import { StreakType } from '../../../common/enums/streak-type.enum';

@Injectable()
export class StreaksRepository implements IStreaksRepository {
  constructor(
    @InjectRepository(Streak)
    private readonly repo: Repository<Streak>,
  ) {}

  async findOrCreate(userId: string, type: StreakType): Promise<Streak> {
    let streak = await this.repo.findOne({
      where: { user_id: userId, streak_type: type },
    });

    if (!streak) {
      streak = this.repo.create({
        user_id: userId,
        streak_type: type,
        current_streak: 0,
        longest_streak: 0,
      });
      streak = await this.repo.save(streak);
    }

    return streak;
  }

  async findByUser(userId: string): Promise<Streak[]> {
    return this.repo.find({ where: { user_id: userId } });
  }

  async updateStreak(
    streakId: string,
    current: number,
    longest: number,
    lastDate: string,
  ): Promise<Streak> {
    await this.repo.update(streakId, {
      current_streak: current,
      longest_streak: longest,
      last_activity_date: lastDate,
    });
    return this.repo.findOne({ where: { id: streakId } }) as Promise<Streak>;
  }

  async findAllByType(type: StreakType): Promise<Streak[]> {
    return this.repo.find({ where: { streak_type: type } });
  }
}
