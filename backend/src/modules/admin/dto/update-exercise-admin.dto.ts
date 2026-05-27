import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsUrl,
  IsArray,
  IsBoolean,
  IsInt,
  Min,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { ExerciseType } from '../../train/enums/exercise-type.enum';
import { DifficultyLevel } from '../../train/enums/difficulty-level.enum';
import { IntensityLevel } from '../../train/enums/intensity-level.enum';

export class UpdateExerciseAdminDto {
  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  name?: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  description?: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  instructions?: string;

  @ApiPropertyOptional({ enum: ExerciseType })
  @IsEnum(ExerciseType)
  @IsOptional()
  exerciseType?: ExerciseType;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  category?: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  muscleGroup?: string;

  @ApiPropertyOptional({ enum: DifficultyLevel })
  @IsEnum(DifficultyLevel)
  @IsOptional()
  difficultyLevel?: DifficultyLevel;

  @ApiPropertyOptional()
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  metValue?: number;

  @ApiPropertyOptional()
  @IsUrl()
  @IsOptional()
  videoUrl?: string;

  @ApiPropertyOptional()
  @IsUrl()
  @IsOptional()
  imageAvtUrl?: string;

  @ApiPropertyOptional({ type: [String] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  secondaryMuscleGroups?: string[];

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  equipment?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  formTips?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  isActive?: boolean;

  // GYM-specific
  @ApiPropertyOptional()
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultSets?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultReps?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  @Min(0)
  defaultWeightKg?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  targetMuscleGroup?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsInt()
  @Min(0)
  restTimeSeconds?: number;

  // SPORT-specific
  @ApiPropertyOptional()
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultDurationMinutes?: number;

  @ApiPropertyOptional({ enum: IntensityLevel })
  @IsOptional()
  @IsEnum(IntensityLevel)
  defaultIntensityLevel?: IntensityLevel;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  movementType?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  @Min(0)
  estimatedCaloriesPerMinute?: number;
}
