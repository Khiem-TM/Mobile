import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityLog } from '../entities/activity-log.entity';
import { IActivityLogsRepository } from './activity-logs.repository.interface';

@Injectable()
export class ActivityLogsRepository implements IActivityLogsRepository {
  constructor(
    @InjectRepository(ActivityLog)
    private readonly repo: Repository<ActivityLog>, // repository của TypeORM để query DB/ insert, update, delete, find, v.v.
  ) {}

  async upsertSteps(
    userId: string,
    date: string,
    steps: number,
  ): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, steps })
      .orUpdate(['steps', 'updated_at'], ['user_id', 'log_date'])
      .execute();

    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertCaloriesBurned(
    userId: string,
    date: string,
    calories: number,
    minutes: number,
    notes?: string,
  ): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({
        userId,
        logDate: date,
        caloriesBurned: calories,
        activeMinutes: minutes,
        exerciseNotes: notes,
      })
      .orUpdate(
        ['calories_burned', 'active_minutes', 'exercise_notes', 'updated_at'],
        ['user_id', 'log_date'],
      )
      .execute();

    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertWater(
    userId: string,
    date: string,
    waterMl: number,
  ): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, waterMl })
      .orUpdate(['water_ml', 'updated_at'], ['user_id', 'log_date'])
      .execute();

    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async findByDate(userId: string, date: string): Promise<ActivityLog | null> {
    return this.repo.findOne({ where: { userId, logDate: date } });
  }

  async upsertWorkoutCalories(userId: string, date: string, calories: number): Promise<void> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, caloriesBurned: calories })
      .orUpdate(['calories_burned', 'updated_at'], ['user_id', 'log_date'])
      .execute();
  }

  async findRange(
    userId: string,
    fromDate: string,
    toDate: string,
  ): Promise<ActivityLog[]> {
    return this.repo
      .createQueryBuilder('al')
      .where('al.userId = :userId', { userId })
      .andWhere('al.logDate BETWEEN :fromDate AND :toDate', {
        fromDate,
        toDate,
      })
      .orderBy('al.logDate', 'ASC')
      .getMany();
  }
}
