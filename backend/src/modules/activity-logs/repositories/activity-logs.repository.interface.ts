import { ActivityLog } from '../entities/activity-log.entity';

export interface IActivityLogsRepository {
  upsertSteps(
    userId: string,
    date: string,
    steps: number,
  ): Promise<ActivityLog>;
  upsertCaloriesBurned(
    userId: string,
    date: string,
    calories: number,
    minutes: number,
    notes?: string,
  ): Promise<ActivityLog>;
  upsertWater(
    userId: string,
    date: string,
    waterMl: number,
  ): Promise<ActivityLog>;
  findByDate(userId: string, date: string): Promise<ActivityLog | null>;
  findRange(
    userId: string,
    fromDate: string,
    toDate: string,
  ): Promise<ActivityLog[]>;
}
