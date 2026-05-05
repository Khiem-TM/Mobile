import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsNumber,
  IsOptional,
  IsString,
  IsUUID,
  Min,
  ValidateNested,
} from 'class-validator';

export class CreateIngredientDto {
  @ApiPropertyOptional({ description: 'ID of an existing food in the database' })
  @IsUUID()
  @IsOptional()
  ingredient_food_id?: string;

  @ApiPropertyOptional({ description: 'Manual ingredient name when not in DB' })
  @IsString()
  @IsOptional()
  ingredient_name?: string;

  @ApiProperty({ description: 'Amount used in grams', example: 150 })
  @IsNumber()
  @Min(0)
  quantity_g!: number;

  @ApiPropertyOptional({ description: 'Flat kcal for this quantity (used when ingredient_food_id is absent)' })
  @IsNumber()
  @IsOptional()
  calories_override?: number;

  @ApiPropertyOptional()
  @IsNumber()
  @IsOptional()
  protein_override?: number;

  @ApiPropertyOptional()
  @IsNumber()
  @IsOptional()
  fat_override?: number;

  @ApiPropertyOptional()
  @IsNumber()
  @IsOptional()
  carbs_override?: number;
}

export class SetIngredientsDto {
  @ApiProperty({ type: [CreateIngredientDto] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CreateIngredientDto)
  ingredients!: CreateIngredientDto[];

  @ApiPropertyOptional({
    description: 'Auto-compute and save nutrition totals from ingredients. Default: true',
    default: true,
  })
  @IsBoolean()
  @IsOptional()
  auto_compute_nutrition?: boolean;
}
