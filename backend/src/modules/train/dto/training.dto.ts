import {
  IsString,
  IsOptional,
  IsEnum,
  IsUUID,
  IsNumber,
  IsArray,
  ValidateNested,
  Min,
  IsDateString,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';

export class ExerciseQueryDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsEnum(MuscleGroup)
  muscleGroup?: MuscleGroup;
}

export class WorkoutDetailInputDto {
  @ApiProperty({ example: 'uuid-of-exercise' })
  @IsUUID()
  exerciseId!: string;

  @ApiProperty({ example: 30 })
  @IsNumber()
  @Min(1)
  durationMinutes!: number;

  @ApiPropertyOptional({ example: 80 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  weightKg?: number;

  @ApiPropertyOptional({ example: 3 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  sets?: number;

  @ApiPropertyOptional({ example: 12 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  repsPerSet?: number;

  @ApiPropertyOptional({ example: 0 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  orderIndex?: number;

  @ApiPropertyOptional({ example: 'Felt strong today' })
  @IsOptional()
  @IsString()
  notes?: string;
}

export class CreateWorkoutSessionDto {
  @ApiProperty({ example: '2026-04-27' })
  @IsDateString()
  sessionDate!: string;

  @ApiPropertyOptional({ example: 'Morning Push Day' })
  @IsOptional()
  @IsString()
  sessionName?: string;

  @ApiPropertyOptional({ example: 'Great energy today' })
  @IsOptional()
  @IsString()
  notes?: string;

  @ApiProperty({ type: [WorkoutDetailInputDto] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => WorkoutDetailInputDto)
  details!: WorkoutDetailInputDto[];
}

export class AddWorkoutDetailDto extends WorkoutDetailInputDto {}
