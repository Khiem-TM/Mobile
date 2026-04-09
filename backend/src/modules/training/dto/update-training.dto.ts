import {
  IsOptional,
  IsEnum,
  IsNumber,
  IsDateString,
  IsString,
  Min,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { TrainingGoalStatus } from '../../../common/enums/training-goal-status.enum';

export class UpdateTrainingGoalDto {
  @ApiPropertyOptional({ example: 75 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  targetValue?: number;

  @ApiPropertyOptional({ example: '2024-06-30' })
  @IsOptional()
  @IsDateString()
  deadline?: string;

  @ApiPropertyOptional({ enum: TrainingGoalStatus })
  @IsOptional()
  @IsEnum(TrainingGoalStatus)
  status?: TrainingGoalStatus;
}

export class UpdateWorkoutSessionDto {
  @ApiPropertyOptional({ example: 45 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  durationMinutes?: number;

  @ApiPropertyOptional({ example: 80 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  weightKg?: number;

  @ApiPropertyOptional({ example: 4 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  sets?: number;

  @ApiPropertyOptional({ example: 10 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  repsPerSet?: number;

  @ApiPropertyOptional({ example: 'Felt strong today' })
  @IsOptional()
  @IsString()
  notes?: string;
}
