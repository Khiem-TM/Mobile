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
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { ExerciseType } from '../../train/enums/exercise-type.enum';
import { DifficultyLevel } from '../../train/enums/difficulty-level.enum';
import { IntensityLevel } from '../../train/enums/intensity-level.enum';

export class CreateExerciseAdminDto {
  @ApiProperty()
  @IsString()
  name!: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  description?: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  instructions?: string;

  @ApiProperty({ enum: ExerciseType })
  @IsEnum(ExerciseType)
  exerciseType!: ExerciseType;

  @ApiPropertyOptional({ example: 'chest' })
  @IsString()
  @IsOptional()
  category?: string;

  @ApiPropertyOptional({ example: 'chest' })
  @IsString()
  @IsOptional()
  muscleGroup?: string;

  @ApiPropertyOptional({ enum: DifficultyLevel, default: DifficultyLevel.BEGINNER })
  @IsEnum(DifficultyLevel)
  @IsOptional()
  difficultyLevel?: DifficultyLevel;

  @ApiPropertyOptional({ default: 0 })
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

  @ApiPropertyOptional({ type: [String], example: ['biceps', 'core'] })
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

  @ApiPropertyOptional({ example: true })
  @IsOptional()
  @IsBoolean()
  isActive?: boolean;

  // GYM-specific
  @ApiPropertyOptional({ example: 3 })
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultSets?: number;

  @ApiPropertyOptional({ example: 10 })
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultReps?: number;

  @ApiPropertyOptional({ example: 20 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  defaultWeightKg?: number;

  @ApiPropertyOptional({ example: 'chest' })
  @IsOptional()
  @IsString()
  targetMuscleGroup?: string;

  @ApiPropertyOptional({ example: 60 })
  @IsOptional()
  @IsInt()
  @Min(0)
  restTimeSeconds?: number;

  // SPORT-specific
  @ApiPropertyOptional({ example: 30 })
  @IsOptional()
  @IsInt()
  @Min(1)
  defaultDurationMinutes?: number;

  @ApiPropertyOptional({ enum: IntensityLevel })
  @IsOptional()
  @IsEnum(IntensityLevel)
  defaultIntensityLevel?: IntensityLevel;

  @ApiPropertyOptional({ example: 'running' })
  @IsOptional()
  @IsString()
  movementType?: string;

  @ApiPropertyOptional({ example: 8.5 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  estimatedCaloriesPerMinute?: number;

  @ApiPropertyOptional({ description: 'Cloudinary URL of a .lottie animation' })
  @IsOptional()
  @IsUrl()
  lottieAsset?: string;
}
