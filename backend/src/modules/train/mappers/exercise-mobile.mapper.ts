import { Exercise } from '../entities/exercise.entity';

export interface ExerciseMobileDto {
  id: string;
  name: string;
  description: string | null;
  instructions: string | null;
  exerciseType: string;
  category: string | null;
  muscleGroup: string | null;
  primaryMuscleGroup: string;
  difficultyLevel: string;
  metValue: number;
  videoUrl: string | null;
  thumbnailUrl: string | null;
  imageAvtUrl: string | null;
  imageUrl: string[] | null;
  favoritesCount: number;
  isFavorite: boolean;
  secondaryMuscleGroups: string[] | null;
  equipment: string | null;
  formTips: string | null;
  defaultSets: number | null;
  defaultReps: number | null;
  defaultWeightKg: number | null;
  targetMuscleGroup: string | null;
  restTimeSeconds: number | null;
  defaultDurationMinutes: number | null;
  defaultIntensityLevel: string | null;
  movementType: string | null;
  estimatedCaloriesPerMinute: number | null;
  caloriesPerMin: number;
  lottieAsset: string | null;
}

export function toExerciseMobileDto(
  exercise: Exercise,
  isFavorite = false,
): ExerciseMobileDto {
  const muscleGroup = exercise.muscleGroup ?? exercise.targetMuscleGroup ?? '';
  const estimatedCaloriesPerMinute =
    exercise.estimatedCaloriesPerMinute === null
      ? null
      : Number(exercise.estimatedCaloriesPerMinute);

  return {
    id: exercise.id,
    name: exercise.name,
    description: exercise.description ?? null,
    instructions: exercise.instructions ?? null,
    exerciseType: exercise.exerciseType,
    category: exercise.category ?? null,
    muscleGroup,
    primaryMuscleGroup: muscleGroup,
    difficultyLevel: exercise.difficultyLevel,
    metValue: Number(exercise.metValue ?? 0),
    videoUrl: exercise.videoUrl ?? null,
    thumbnailUrl: exercise.imageAvtUrl ?? null,
    imageAvtUrl: exercise.imageAvtUrl ?? null,
    imageUrl: exercise.imageUrl ?? null,
    favoritesCount: exercise.favoritesCount ?? 0,
    isFavorite,
    secondaryMuscleGroups: exercise.secondaryMuscleGroups ?? null,
    equipment: exercise.equipment ?? null,
    formTips: exercise.formTips ?? null,
    defaultSets: exercise.defaultSets ?? null,
    defaultReps: exercise.defaultReps ?? null,
    defaultWeightKg:
      exercise.defaultWeightKg === null ? null : Number(exercise.defaultWeightKg),
    targetMuscleGroup: exercise.targetMuscleGroup ?? null,
    restTimeSeconds: exercise.restTimeSeconds ?? null,
    defaultDurationMinutes: exercise.defaultDurationMinutes ?? null,
    defaultIntensityLevel: exercise.defaultIntensityLevel ?? null,
    movementType: exercise.movementType ?? null,
    estimatedCaloriesPerMinute,
    caloriesPerMin: estimatedCaloriesPerMinute ?? 0,
    lottieAsset: exercise.lottieAsset ?? null,
  };
}
