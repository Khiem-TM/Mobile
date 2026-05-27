import {
  IsString,
  IsOptional,
  IsEnum,
  IsNumber,
  Min,
  IsInt,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { IntensityLevel } from '../enums/intensity-level.enum';

export class UpdateTrainingSessionItemDto {
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
