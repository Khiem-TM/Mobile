import {
  IsOptional,
  IsString,
  IsNumber,
  IsDateString,
  IsArray,
  IsEnum,
  IsIn,
  Min,
  Max,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { FoodAllergyType } from '../../../common/enums/food-allergy.enum';
import { ActivityLevel } from '../../../common/enums/activity-level.enum';
import { GoalType } from '../../../common/enums/goal-type.enum';

export class UpdateHealthProfileDto {
  @ApiPropertyOptional({ example: '1995-06-15' })
  @IsOptional()
  @IsDateString()
  birthDate?: string;

  @ApiPropertyOptional({ example: 'male', enum: ['male', 'female', 'other'] })
  @IsOptional()
  @IsIn(['male', 'female', 'other'])
  gender?: string;

  @ApiPropertyOptional({ example: 170 })
  @IsOptional()
  @IsNumber()
  @Min(50)
  @Max(300)
  heightCm?: number;

  @ApiPropertyOptional({ example: 65 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(500)
  initialWeightKg?: number;

  @ApiPropertyOptional({
    example: 'moderately_active',
    enum: [
      'sedentary',
      'lightly_active',
      'moderately_active',
      'very_active',
      'extra_active',
    ],
  })
  @IsOptional()
  @IsEnum(ActivityLevel)
  activityLevel?: ActivityLevel;

  @ApiPropertyOptional({ example: 'balanced' })
  @IsOptional()
  @IsString()
  dietType?: string;

  @ApiPropertyOptional({ type: [String], enum: FoodAllergyType })
  @IsOptional()
  @IsArray()
  @IsEnum(FoodAllergyType, { each: true })
  foodAllergies?: FoodAllergyType[];

  @ApiPropertyOptional({ example: 60 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(500)
  weightGoalKg?: number;

  @ApiPropertyOptional({ example: 2000 })
  @IsOptional()
  @IsNumber()
  @Min(500)
  @Max(10000)
  waterGoalMl?: number;

  @ApiPropertyOptional({ example: 10000 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(100000)
  stepGoal?: number;

  @ApiPropertyOptional({ example: 2000 })
  @IsOptional()
  @IsNumber()
  @Min(500)
  @Max(10000)
  caloriesGoal?: number;

  @ApiPropertyOptional({
    example: 'lose_weight',
    enum: GoalType,
  })
  @IsOptional()
  @IsEnum(GoalType)
  goalType?: GoalType;

  @ApiPropertyOptional({ example: 65 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(500)
  targetWeightKg?: number;

  @ApiPropertyOptional({ example: 1800 })
  @IsOptional()
  @IsNumber()
  @Min(500)
  @Max(10000)
  dailyCaloriesGoal?: number;

  @ApiPropertyOptional({ example: 150 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  proteinGoalG?: number;

  @ApiPropertyOptional({ example: 65 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  fatGoalG?: number;

  @ApiPropertyOptional({ example: 200 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  carbsGoalG?: number;

  @ApiPropertyOptional({ example: 0.5 })
  @IsOptional()
  @IsNumber()
  @Min(-2)
  @Max(2)
  weeklyRateKg?: number;

  @ApiPropertyOptional({ example: '2026-01-01' })
  @IsOptional()
  @IsDateString()
  goalStartDate?: string;

  @ApiPropertyOptional({ example: '2026-12-31' })
  @IsOptional()
  @IsDateString()
  goalDeadline?: string;

  @ApiPropertyOptional({
    example: 'active',
    enum: ['active', 'completed', 'paused'],
  })
  @IsOptional()
  @IsString()
  goalStatus?: string;
}
