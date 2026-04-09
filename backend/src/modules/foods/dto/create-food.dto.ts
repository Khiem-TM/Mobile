import { IsString, IsOptional, IsNumber, IsBoolean, IsArray, IsUrl, Min, Max, IsIn } from 'class-validator';

export class CreateFoodDto {
  @IsString()
  name: string;

  @IsString()
  @IsOptional()
  name_en?: string;

  @IsString()
  @IsOptional()
  brand?: string;

  @IsString()
  @IsOptional()
  category?: string;

  @IsIn(['ingredient', 'dish', 'product'])
  @IsOptional()
  food_type?: 'ingredient' | 'dish' | 'product';

  @IsNumber()
  @Min(0)
  serving_size_g: number;

  @IsString()
  @IsOptional()
  serving_unit?: string;

  @IsNumber()
  @Min(0)
  calories_per_100g: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  protein_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  fat_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  carbs_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  fiber_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  sugar_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  sodium_per_100g?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  cholesterol_per_100g?: number;

  @IsArray()
  @IsString({ each: true })
  @IsUrl({}, { each: true })
  @IsOptional()
  image_urls?: string[];
}
