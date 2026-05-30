import { Exercise } from '../entities/exercise.entity';
import { ExerciseType } from '../enums/exercise-type.enum';

export interface ExerciseQuery {
  name?: string;
  exerciseType?: ExerciseType;
  type?: ExerciseType;
  category?: string;
  muscleGroup?: string;
  difficultyLevel?: string;
  page?: number;
  limit?: number;
}

export interface IExercisesRepository {
  findAll(query: ExerciseQuery): Promise<Exercise[]>;
  findById(id: string): Promise<Exercise | null>;
  findByIds(ids: string[]): Promise<Exercise[]>;
  findPopular(limit: number): Promise<Exercise[]>;
  updateAvtImage(id: string, imageAvtUrl: string | null, imageAvtPublicId: string | null): Promise<Exercise>;
  addImageToGallery(id: string, imageUrl: string, imagePublicId: string): Promise<Exercise>;
  removeImageFromGallery(id: string, imagePublicId: string): Promise<Exercise>;
}
