import { Exercise } from '../entities/exercise.entity';
import { WorkoutSession } from '../entities/workout-session.entity';
import { TrainingGoal } from '../entities/training-goal.entity';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';

export interface IExercisesRepository {
  findAll(query: { name?: string, muscleGroup?: MuscleGroup }): Promise<Exercise[]>;
  findById(id: string): Promise<Exercise | null>;
  updateAvtImage(id: string, imageAvtUrl: string | null, imageAvtPublicId: string | null): Promise<Exercise>;
  addImageToGallery(id: string, imageUrl: string, imagePublicId: string): Promise<Exercise>;
  removeImageFromGallery(id: string, imagePublicId: string): Promise<Exercise>;
}

export interface IWorkoutSessionsRepository {
  save(session: Partial<WorkoutSession>): Promise<WorkoutSession>;
  findByUser(userId: string, limit: number): Promise<WorkoutSession[]>;
  findByDateRange(userId: string, from: string, to: string): Promise<WorkoutSession[]>;
  findById(id: string): Promise<WorkoutSession | null>;
  update(id: string, data: Partial<WorkoutSession>): Promise<WorkoutSession>;
  delete(id: string): Promise<void>;
}

export interface ITrainingGoalsRepository {
  save(goal: Partial<TrainingGoal>): Promise<TrainingGoal>;
  findByUser(userId: string): Promise<TrainingGoal[]>;
  findById(id: string): Promise<TrainingGoal | null>;
  updateProgress(goalId: string, progress: number): Promise<void>;
  update(id: string, data: Partial<TrainingGoal>): Promise<TrainingGoal>;
  delete(id: string): Promise<void>;
}
