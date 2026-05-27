import {
  IsDateString,
  IsIn,
  IsNumber,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  MaxLength,
  Min,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpsertBodyMetricDto {
  @ApiPropertyOptional({ example: '2026-05-25' })
  @IsOptional()
  @IsDateString()
  measuredAt?: string;

  @ApiPropertyOptional({ example: 70.5, description: 'Weight in kg (20–500)' })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg?: number;

  @ApiPropertyOptional({ example: 165.0 })
  @IsOptional()
  @IsNumber()
  @Min(50)
  @Max(300)
  heightCm?: number;

  @ApiPropertyOptional({ example: 18.5, description: 'Body fat percentage (1–70)' })
  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(70)
  bodyFatPercent?: number;

  @ApiPropertyOptional({ example: 72.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  waistCm?: number;

  @ApiPropertyOptional({ example: 95.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  hipCm?: number;

  @ApiPropertyOptional({ example: 90.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  chestCm?: number;

  @ApiPropertyOptional({ example: 35.0 })
  @IsOptional()
  @IsNumber()
  @Min(5)
  @Max(100)
  neckCm?: number;

  @ApiPropertyOptional({ example: 32.0 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(100)
  armCm?: number;

  @ApiPropertyOptional({ example: 'Feeling good today' })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  notes?: string;
}

export class BasicBodyMetricDto {
  @ApiPropertyOptional({ example: '2026-05-25' })
  @IsOptional()
  @IsDateString()
  measuredAt?: string;

  @ApiPropertyOptional({ example: 70.5, description: 'Weight in kg (20-500)' })
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg!: number;

  @ApiPropertyOptional({ example: 165.0 })
  @IsNumber()
  @Min(50)
  @Max(300)
  heightCm!: number;
}

export class AdvancedBodyMetricDto {
  @ApiPropertyOptional({ example: '2026-05-25' })
  @IsOptional()
  @IsDateString()
  measuredAt?: string;

  @ApiPropertyOptional({ example: 18.5, description: 'Body fat percentage (1-70)' })
  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(70)
  bodyFatPercent?: number;

  @ApiPropertyOptional({ example: 72.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  waistCm?: number;

  @ApiPropertyOptional({ example: 95.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  hipCm?: number;

  @ApiPropertyOptional({ example: 90.0 })
  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  chestCm?: number;

  @ApiPropertyOptional({ example: 35.0 })
  @IsOptional()
  @IsNumber()
  @Min(5)
  @Max(100)
  neckCm?: number;

  @ApiPropertyOptional({ example: 32.0 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(100)
  armCm?: number;

  @ApiPropertyOptional({ example: 'Feeling good today' })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  notes?: string;
}

export class UploadBodyProgressPhotoDto {
  @ApiPropertyOptional({ enum: ['front', 'back', 'side'], default: 'front' })
  @IsOptional()
  @IsString()
  @IsIn(['front', 'back', 'side'])
  photoType?: string;

  @ApiPropertyOptional({ example: 'uuid-of-body-metric' })
  @IsOptional()
  @IsUUID()
  bodyMetricId?: string;
}
