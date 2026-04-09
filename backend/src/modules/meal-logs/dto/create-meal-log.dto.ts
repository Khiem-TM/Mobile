import {
  IsString,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsIn,
  IsDateString,
  IsUUID,
  Min,
  IsEnum,
} from 'class-validator';
import { Type, Transform } from 'class-transformer';

export class CreateMealLogItemDto {
  @IsUUID()
  food_id!: string;

  @Type(() => Number)
  @IsNumber()
  @Min(0.01)
  quantity!: number;

  @IsString()
  @IsNotEmpty()
  serving_unit!: string;

  @IsString()
  @IsIn(['manual', 'ai_scan', 'barcode', 'history', 'favorite'])
  @IsOptional()
  source?: 'manual' | 'ai_scan' | 'barcode' | 'history' | 'favorite';
}

import { MealType } from '../../../common/enums/meal-type.enum';

export class CreateMealLogDto {
  @IsOptional()
  @IsDateString()
  log_date?: string;

  @Transform(({ value }) => (typeof value === 'string' ? value.toUpperCase() : value))
  @IsEnum(MealType)
  meal_type!: MealType;

  @IsString()
  @IsOptional()
  notes?: string;

  @IsOptional()
  items?: CreateMealLogItemDto[];
}
