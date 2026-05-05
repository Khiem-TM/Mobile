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
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';
import { TrainingIntensity } from '../../../common/enums/training-intensity.enum';

export class CreateExerciseAdminDto {
  @ApiProperty()
  @IsString()
  name!: string;

  @ApiPropertyOptional()
  @IsString()
  @IsOptional()
  description?: string;

  @ApiProperty({ enum: MuscleGroup })
  @IsEnum(MuscleGroup)
  primaryMuscleGroup!: MuscleGroup;

  @ApiProperty({ enum: TrainingIntensity })
  @IsEnum(TrainingIntensity)
  intensity!: TrainingIntensity;

  @ApiPropertyOptional({ default: 0 })
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

  @ApiPropertyOptional({ example: 'Keep your back straight throughout the movement.' })
  @IsOptional()
  @IsString()
  formTips?: string;

  @ApiPropertyOptional({ example: true })
  @IsOptional()
  @IsBoolean()
  isActive?: boolean;
}
