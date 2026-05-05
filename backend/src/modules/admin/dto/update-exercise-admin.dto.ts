import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsUrl,
  IsArray,
  IsBoolean,
  Min,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';
import { TrainingIntensity } from '../../../common/enums/training-intensity.enum';

export class UpdateExerciseAdminDto {
  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  name?: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  description?: string;

  @ApiPropertyOptional({ enum: MuscleGroup })
  @IsEnum(MuscleGroup)
  @IsOptional()
  primaryMuscleGroup?: MuscleGroup;

  @ApiPropertyOptional({ enum: TrainingIntensity })
  @IsEnum(TrainingIntensity)
  @IsOptional()
  intensity?: TrainingIntensity;

  @ApiPropertyOptional()
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  metValue?: number;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  instructions?: string;

  @ApiPropertyOptional()
  @IsUrl()
  @IsOptional()
  videoUrl?: string;

  @ApiPropertyOptional({ type: [String], example: ['biceps', 'core'] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  secondaryMuscleGroups?: string[];

  @ApiPropertyOptional({
    example: 'bodyweight',
    enum: ['bodyweight', 'dumbbell', 'barbell', 'cable', 'machine', 'resistance_band', 'kettlebell', 'other'],
  })
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
}
