import {
  IsDateString,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

export class UpsertBodyMetricDto {
  @IsOptional()
  @IsDateString()
  recordedAt?: string;

  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg?: number;

  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(70)
  bodyFatPct?: number;

  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  waistCm?: number;

  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  hipCm?: number;

  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  chestCm?: number;

  @IsOptional()
  @IsNumber()
  @Min(20)
  @Max(300)
  neckCm?: number;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  notes?: string;
}
