import { ActivityLog } from '../entities/activity-log.entity';

export interface IActivityLogsRepository {
  upsertSteps(userId: string, date: string, steps: number): Promise<ActivityLog>;
  upsertWater(userId: string, date: string, waterMl: number): Promise<ActivityLog>;
  upsertNote(userId: string, date: string, note: string): Promise<ActivityLog>;
  upsertSleep(userId: string, date: string, sleepHours: number): Promise<ActivityLog>;
  upsertMood(userId: string, date: string, mood: string): Promise<ActivityLog>;
  findByDate(userId: string, date: string): Promise<ActivityLog | null>;
  findRange(userId: string, fromDate: string, toDate: string): Promise<ActivityLog[]>;
}
