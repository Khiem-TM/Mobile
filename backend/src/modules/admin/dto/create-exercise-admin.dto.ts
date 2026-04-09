import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsUrl,
  Min,
} from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';
import { TrainingIntensity } from '../../../common/enums/training-intensity.enum';

export class CreateExerciseAdminDto {
  @ApiProperty()
  @IsString()
  name!: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  description?: string;

  @ApiProperty({ enum: MuscleGroup })
  @IsEnum(MuscleGroup)
  primaryMuscleGroup!: MuscleGroup;

  @ApiProperty({ enum: TrainingIntensity })
  @IsEnum(TrainingIntensity)
  intensity!: TrainingIntensity;

  @ApiProperty({ required: false, default: 0 })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  metValue?: number;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  instructions?: string;

  @ApiProperty({ required: false })
  @IsUrl()
  @IsOptional()
  videoUrl?: string;
}
