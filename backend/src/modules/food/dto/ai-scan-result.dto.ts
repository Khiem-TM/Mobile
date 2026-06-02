import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class AiDetectedItem {
  @ApiProperty({ example: 'cơm trắng' })
  ai_food_name: string;

  @ApiProperty({ example: 100 })
  estimated_weight_g: number;
}

export class EstimatedNutritionDto {
  @ApiProperty({ example: 215.4 })
  calories: number;

  @ApiProperty({ example: 12.3 })
  protein: number;

  @ApiProperty({ example: 8.1 })
  fat: number;

  @ApiProperty({ example: 25.6 })
  carbs: number;

  @ApiPropertyOptional({ example: 1.2, nullable: true })
  fiber: number | null;
}

export class AiScanResultDto {
  @ApiProperty({
    example: 'grilled salmon',
    description: 'Tên món AI nhận diện (Food-101 label)',
  })
  ai_food_name: string;

  @ApiProperty({
    example: 610.7,
    description: 'Thể tích vật lý ước lượng từ ảnh (cm³)',
  })
  estimated_volume_cm3: number;

  @ApiProperty({
    example: 641.2,
    description: 'Khối lượng ước lượng (g) = volume × density theo category',
  })
  estimated_weight_g: number;

  @ApiPropertyOptional({
    example: 0.95,
    description: 'Confidence score (không có trong contract volume hiện tại)',
  })
  confidence_score?: number;

  @ApiPropertyOptional({
    type: EstimatedNutritionDto,
    nullable: true,
    description:
      'Dinh dưỡng ước lượng cho estimated_weight_g, dựa trên món khớp nhất trong DB. null nếu không khớp.',
  })
  estimated_nutrition?: EstimatedNutritionDto | null;

  @ApiProperty({ description: 'Matching foods found in database' })
  matched_foods: Array<{
    id: string;
    name: string;
    name_en: string | null;
    category: string | null;
    calories_per_100g: number;
    protein_per_100g: number;
    fat_per_100g: number;
    carbs_per_100g: number;
    image_urls: string[] | null;
  }>;
}
