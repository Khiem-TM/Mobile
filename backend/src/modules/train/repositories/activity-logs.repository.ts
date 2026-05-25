import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityLog } from '../entities/activity-log.entity';
import { IActivityLogsRepository } from './activity-logs.repository.interface';

@Injectable()
export class ActivityLogsRepository implements IActivityLogsRepository {
  constructor(
    @InjectRepository(ActivityLog)
    private readonly repo: Repository<ActivityLog>,
  ) {}

  async upsertSteps(userId: string, date: string, steps: number): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, steps })
      .orUpdate(['steps', 'updated_at'], ['user_id', 'log_date'])
      .execute();
    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertWater(userId: string, date: string, waterMl: number): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, waterMl })
      .orUpdate(['water_ml', 'updated_at'], ['user_id', 'log_date'])
      .execute();
    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertNote(userId: string, date: string, note: string): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, note })
      .orUpdate(['note', 'updated_at'], ['user_id', 'log_date'])
      .execute();
    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertSleep(userId: string, date: string, sleepHours: number): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, sleepHours })
      .orUpdate(['sleep_hours', 'updated_at'], ['user_id', 'log_date'])
      .execute();
    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async upsertMood(userId: string, date: string, mood: string): Promise<ActivityLog> {
    await this.repo
      .createQueryBuilder()
      .insert()
      .into(ActivityLog)
      .values({ userId, logDate: date, mood })
      .orUpdate(['mood', 'updated_at'], ['user_id', 'log_date'])
      .execute();
    return this.findByDate(userId, date) as Promise<ActivityLog>;
  }

  async findByDate(userId: string, date: string): Promise<ActivityLog | null> {
    return this.repo.findOne({ where: { userId, logDate: date } });
  }

  async findRange(userId: string, fromDate: string, toDate: string): Promise<ActivityLog[]> {
    return this.repo
      .createQueryBuilder('al')
      .where('al.userId = :userId', { userId })
      .andWhere('al.logDate BETWEEN :fromDate AND :toDate', { fromDate, toDate })
      .orderBy('al.logDate', 'ASC')
      .getMany();
  }
}
