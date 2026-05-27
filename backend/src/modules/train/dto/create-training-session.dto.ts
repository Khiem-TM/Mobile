import {
  IsString,
  IsOptional,
  IsDateString,
  IsArray,
  ValidateNested,
  IsEnum,
  IsUUID,
  IsNumber,
  Min,
  IsInt,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ExerciseType } from '../enums/exercise-type.enum';
import { IntensityLevel } from '../enums/intensity-level.enum';

export class CreateSessionItemDto {
  @ApiProperty({ example: 'uuid-of-exercise' })
  @IsUUID()
  exerciseId!: string;

  @ApiProperty({ enum: ExerciseType })
  @IsEnum(ExerciseType)
  exerciseType!: ExerciseType;

  @ApiPropertyOptional({ example: 0 })
  @IsOptional()
  @IsInt()
  @Min(0)
  orderIndex?: number;

  @ApiPropertyOptional({ example: 30 })
  @IsOptional()
  @IsInt()
  @Min(0)
  durationMinutes?: number;

  @ApiPropertyOptional({ example: 'Great set' })
  @IsOptional()
  @IsString()
  note?: string;

  // GYM specific
  @ApiPropertyOptional({ example: 4 })
  @IsOptional()
  @IsInt()
  @Min(1)
  sets?: number;

  @ApiPropertyOptional({ example: 10 })
  @IsOptional()
  @IsInt()
  @Min(1)
  reps?: number;

  @ApiPropertyOptional({ example: 60 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  weightKg?: number;

  @ApiPropertyOptional({ example: 90 })
  @IsOptional()
  @IsInt()
  @Min(0)
  restTimeSeconds?: number;

  // SPORT specific
  @ApiPropertyOptional({ enum: IntensityLevel })
  @IsOptional()
  @IsEnum(IntensityLevel)
  intensityLevel?: IntensityLevel;

  @ApiPropertyOptional({ example: 5.0 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  distanceKm?: number;

  // CARDIO only — triggers auto-computation of durationMinutes and caloriesBurned
  @ApiPropertyOptional({ example: 10.5, description: 'Average speed km/h (CARDIO type only)' })
  @IsOptional()
  @IsNumber()
  @Min(0.1)
  avgSpeedKmh?: number;

  @ApiPropertyOptional({ example: '5:30' })
  @IsOptional()
  @IsString()
  pace?: string;
}

export class CreateTrainingSessionDto {
  @ApiProperty({ example: '2026-05-25' })
  @IsDateString()
  sessionDate!: string;

  @ApiPropertyOptional({ example: 'Push Day' })
  @IsOptional()
  @IsString()
  title?: string;

  @ApiPropertyOptional({ example: 'Great energy today' })
  @IsOptional()
  @IsString()
  note?: string;

  @ApiProperty({ type: [CreateSessionItemDto] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CreateSessionItemDto)
  items!: CreateSessionItemDto[];
}
