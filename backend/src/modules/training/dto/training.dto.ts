import { IsString, IsOptional, IsEnum, IsUUID, IsNumber, Min, IsDateString } from 'class-validator';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';
import { TrainingGoalType } from '../../../common/enums/training-goal-type.enum';

export class ExerciseQueryDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsEnum(MuscleGroup)
  muscleGroup?: MuscleGroup;
}

export class LogWorkoutDto {
  @IsUUID()
  exerciseId!: string;

  @IsDateString()
  sessionDate!: string;

  @IsNumber()
  @Min(1)
  durationMinutes!: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  weightKg?: number;

  @IsNumber()
  @Min(1)
  @IsOptional()
  sets?: number;

  @IsNumber()
  @Min(1)
  @IsOptional()
  repsPerSet?: number;

  @IsOptional()
  @IsString()
  notes?: string;
}

export class CreateTrainingGoalDto {
  @IsEnum(TrainingGoalType)
  goalType!: TrainingGoalType;

  @IsNumber()
  @Min(1)
  targetValue!: number;

  @IsDateString()
  startDate!: string;

  @IsDateString()
  deadline!: string;
}
