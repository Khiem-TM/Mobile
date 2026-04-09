import {
  IsString,
  IsOptional,
  IsNumber,
  IsBoolean,
  IsEnum,
  Min,
} from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';

export enum FoodType {
  INGREDIENT = 'ingredient',
  DISH = 'dish',
  PRODUCT = 'product',
}

export class CreateFoodAdminDto {
  @ApiProperty()
  @IsString()
  name!: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  nameEn?: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  brand?: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  category?: string;

  @ApiProperty({ enum: FoodType, required: false })
  @IsEnum(FoodType)
  @IsOptional()
  foodType?: FoodType;

  @ApiProperty({ required: false, default: 100 })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  servingSizeG?: number;

  @ApiProperty()
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  caloriesPer100g!: number;

  @ApiProperty({ required: false, default: 0 })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  proteinPer100g?: number;

  @ApiProperty({ required: false, default: 0 })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  fatPer100g?: number;

  @ApiProperty({ required: false, default: 0 })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  carbsPer100g?: number;

  @ApiProperty({ required: false })
  @IsBoolean()
  @IsOptional()
  isVerified?: boolean;
}
