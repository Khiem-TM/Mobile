import { IsOptional, IsString, IsEnum, IsInt, Min, Max } from 'class-validator';
import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { ExerciseType } from '../enums/exercise-type.enum';
import { DifficultyLevel } from '../enums/difficulty-level.enum';

export class ExerciseQueryDto {
  @ApiPropertyOptional({ example: 'bench press' })
  @IsOptional()
  @IsString()
  name?: string;

  @ApiPropertyOptional({ enum: ExerciseType })
  @IsOptional()
  @IsEnum(ExerciseType)
  exerciseType?: ExerciseType;

  @ApiPropertyOptional({
    enum: ExerciseType,
    description: 'Backward-compatible alias for exerciseType',
  })
  @IsOptional()
  @IsEnum(ExerciseType)
  type?: ExerciseType;

  @ApiPropertyOptional({ example: 'chest' })
  @IsOptional()
  @IsString()
  category?: string;

  @ApiPropertyOptional({ example: 'chest' })
  @IsOptional()
  @IsString()
  muscleGroup?: string;

  @ApiPropertyOptional({ enum: DifficultyLevel })
  @IsOptional()
  @IsEnum(DifficultyLevel)
  difficultyLevel?: DifficultyLevel;

  @ApiPropertyOptional({ example: 1 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Type(() => Number)
  page?: number;

  @ApiPropertyOptional({ example: 20 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(100)
  @Type(() => Number)
  limit?: number;
}
