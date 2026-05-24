import {
  IsArray,
  IsDateString,
  IsEnum,
  IsIn,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  Min,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ActivityLevel } from '../../../common/enums/activity-level.enum';
import { GoalType } from '../../../common/enums/goal-type.enum';
import { FoodAllergyType } from '../../../common/enums/food-allergy.enum';

export class OnboardingDto {
  @ApiProperty({ example: '1995-06-15' })
  @IsDateString()
  birthDate!: string;

  @ApiProperty({ example: 'male', enum: ['male', 'female', 'other'] })
  @IsIn(['male', 'female', 'other'])
  gender!: string;

  @ApiProperty({ example: 170 })
  @Type(() => Number)
  @IsNumber()
  @Min(50)
  @Max(300)
  heightCm!: number;

  @ApiProperty({ example: 65 })
  @Type(() => Number)
  @IsNumber()
  @Min(20)
  @Max(500)
  initialWeightKg!: number;

  @ApiProperty({ enum: ActivityLevel, example: ActivityLevel.MODERATELY_ACTIVE })
  @IsEnum(ActivityLevel)
  activityLevel!: ActivityLevel;

  @ApiProperty({ enum: GoalType, example: GoalType.LOSE_WEIGHT })
  @IsEnum(GoalType)
  goalType!: GoalType;

  @ApiPropertyOptional({ example: 60 })
  @Type(() => Number)
  @IsNumber()
  @Min(10)
  @Max(500)
  @IsOptional()
  targetWeightKg?: number;

  @ApiPropertyOptional({ example: 'balanced' })
  @IsString()
  @IsOptional()
  dietType?: string;

  @ApiPropertyOptional({ type: [String], enum: FoodAllergyType })
  @IsArray()
  @IsEnum(FoodAllergyType, { each: true })
  @IsOptional()
  foodAllergies?: FoodAllergyType[];
}
