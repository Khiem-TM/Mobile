import { TrainingSession } from '../entities/training-session.entity';
import { TrainingSessionItem } from '../entities/training-session-item.entity';

export interface ITrainingSessionsRepository {
  createSession(data: Partial<TrainingSession>): Promise<TrainingSession>;
  findByUser(userId: string, limit: number): Promise<TrainingSession[]>;
  findByDateRange(userId: string, from: string, to: string): Promise<TrainingSession[]>;
  findById(id: string): Promise<TrainingSession | null>;
  updateTotals(id: string, totalDurationMinutes: number, totalCaloriesBurned: number): Promise<void>;
  updateSession(id: string, data: Partial<TrainingSession>): Promise<TrainingSession>;
  deleteSession(id: string): Promise<void>;
  sumCaloriesForDate(userId: string, date: string): Promise<number>;
  addItem(data: Partial<TrainingSessionItem>): Promise<TrainingSessionItem>;
  findItemById(id: string): Promise<TrainingSessionItem | null>;
  updateItem(id: string, data: Partial<TrainingSessionItem>): Promise<TrainingSessionItem>;
  deleteItem(id: string): Promise<void>;
  findItemsBySession(sessionId: string): Promise<TrainingSessionItem[]>;
}
