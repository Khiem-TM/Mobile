import { Exercise } from '../entities/exercise.entity';
import { WorkoutSession } from '../entities/workout-session.entity';
import { WorkoutSessionDetail } from '../entities/workout-session-detail.entity';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';

export interface IExercisesRepository {
  findAll(query: { name?: string, muscleGroup?: MuscleGroup }): Promise<Exercise[]>;
  findById(id: string): Promise<Exercise | null>;
  updateAvtImage(id: string, imageAvtUrl: string | null, imageAvtPublicId: string | null): Promise<Exercise>;
  addImageToGallery(id: string, imageUrl: string, imagePublicId: string): Promise<Exercise>;
  removeImageFromGallery(id: string, imagePublicId: string): Promise<Exercise>;
}

export interface IWorkoutSessionsRepository {
  createSession(data: Partial<WorkoutSession>): Promise<WorkoutSession>;
  findByUser(userId: string, limit: number): Promise<WorkoutSession[]>;
  findByDateRange(userId: string, from: string, to: string): Promise<WorkoutSession[]>;
  findById(id: string): Promise<WorkoutSession | null>;
  updateTotals(id: string, totalDurationMinutes: number, totalCaloriesBurned: number): Promise<void>;
  updateSession(id: string, data: Partial<WorkoutSession>): Promise<WorkoutSession>;
  deleteSession(id: string): Promise<void>;
  sumCaloriesForDate(userId: string, date: string): Promise<number>;
  addDetail(data: Partial<WorkoutSessionDetail>): Promise<WorkoutSessionDetail>;
  findDetailById(detailId: string): Promise<WorkoutSessionDetail | null>;
  deleteDetail(detailId: string): Promise<void>;
  findDetailsBySession(sessionId: string): Promise<WorkoutSessionDetail[]>;
}
