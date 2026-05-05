import { ApiProperty } from '@nestjs/swagger';

export class AiDetectedItem {
  @ApiProperty({ example: 'cơm trắng' })
  ai_food_name: string;

  @ApiProperty({ example: 150 })
  estimated_weight_g: number;
}

export class AiScanResultDto {
  @ApiProperty({ example: 'cơm trắng' })
  ai_food_name: string;

  @ApiProperty({ example: 150 })
  estimated_weight_g: number;

  @ApiProperty({ description: 'Matching foods found in database' })
  matched_foods: Array<{
    id: string;
    name: string;
    name_en: string | null;
    calories_per_100g: number;
    protein_per_100g: number;
    fat_per_100g: number;
    carbs_per_100g: number;
    image_urls: string[] | null;
  }>;
}
