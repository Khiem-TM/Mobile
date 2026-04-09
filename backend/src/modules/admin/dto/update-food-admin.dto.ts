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
import { FoodType } from './create-food-admin.dto';

export class UpdateFoodAdminDto {
  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  name?: string;

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

  @ApiProperty({ required: false })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  servingSizeG?: number;

  @ApiProperty({ required: false })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  caloriesPer100g?: number;

  @ApiProperty({ required: false })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  proteinPer100g?: number;

  @ApiProperty({ required: false })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  fatPer100g?: number;

  @ApiProperty({ required: false })
  @IsNumber()
  @Min(0)
  @Type(() => Number)
  @IsOptional()
  carbsPer100g?: number;

  @ApiProperty({ required: false })
  @IsBoolean()
  @IsOptional()
  isVerified?: boolean;

  @ApiProperty({ required: false })
  @IsBoolean()
  @IsOptional()
  isActive?: boolean;
}
