import {
  Injectable,
  InternalServerErrorException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, ILike } from 'typeorm';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { Food } from '../entities/food.entity';
import { AiScanLog } from '../entities/ai-scan-log.entity';
import { AiScanResultDto } from '../dto/ai-scan-result.dto';
import { volumeToGrams, scaleNutrition } from '../nutrition.util';

/** One detected dish from the Python CV service (volume only, no nutrition). */
interface FoodVolume {
  food_title: string;
  volume_cm3: number;
}

interface EstimateVolumeResponse {
  success: boolean;
  data: FoodVolume[];
  detail?: string;
}

const FOOD_AI_URL = process.env.FOOD_AI_SERVICE_URL ?? 'http://localhost:8000';

@Injectable()
export class AiScanService {
  constructor(
    @InjectRepository(Food)
    private readonly foodRepo: Repository<Food>,
    @InjectRepository(AiScanLog)
    private readonly scanLogRepo: Repository<AiScanLog>,
    private readonly cloudinaryService: CloudinaryService,
  ) {}

  async analyzeImage(
    file: Express.Multer.File,
    userId: string,
  ): Promise<AiScanResultDto[]> {
    // 1. Upload image to Cloudinary for storage
    const { url: image_url, publicId: image_public_id } =
      await this.cloudinaryService.uploadFile(file, 'ai-scans');

    // 2. Call Python food-ai-service -> [{ food_title, volume_cm3 }]
    let detections: FoodVolume[] = [];
    let rawResponse = '';

    try {
      detections = await this.callFoodAiService(file);
      rawResponse = JSON.stringify(detections);
    } catch (err) {
      // Preserve a 400 (e.g. "no plate detected") as a client error with hint.
      if (err instanceof BadRequestException) throw err;
      console.error('[AiScan] food-ai-service error:', err);
      throw new InternalServerErrorException(
        'AI analysis failed. Please ensure the food-ai-service is running.',
      );
    }

    // 3. Save scan log
    await this.scanLogRepo.save(
      this.scanLogRepo.create({
        user_id: userId,
        image_url,
        image_public_id,
        raw_response: rawResponse,
      }),
    );

    // 4. Map each detection -> DB match + estimated weight + nutrition
    const results: AiScanResultDto[] = await Promise.all(
      detections.map((item) => this.buildResult(item)),
    );

    return results;
  }

  private async callFoodAiService(
    file: Express.Multer.File,
  ): Promise<FoodVolume[]> {
    const form = new FormData();
    const blob = new Blob([new Uint8Array(file.buffer)], {
      type: file.mimetype,
    });
    form.append('image', blob, file.originalname || 'image.jpg');

    const response = await fetch(`${FOOD_AI_URL}/api/v1/estimate-volume`, {
      method: 'POST',
      body: form,
    });

    const data = (await response
      .json()
      .catch(() => null)) as EstimateVolumeResponse | null;

    if (!response.ok || !data?.success) {
      const detail = data?.detail ?? `food-ai-service returned ${response.status}`;
      // 400 from the CV service = bad input image (e.g. no plate/bowl found).
      if (response.status === 400) {
        throw new BadRequestException(detail);
      }
      throw new Error(detail);
    }

    if (!Array.isArray(data.data)) {
      throw new Error(`Unexpected response shape: ${JSON.stringify(data)}`);
    }
    return data.data;
  }

  private async buildResult(item: FoodVolume): Promise<AiScanResultDto> {
    const matched = await this.foodRepo.find({
      where: [
        { name: ILike(`%${item.food_title}%`), is_active: true, is_custom: false },
        {
          name_en: ILike(`%${item.food_title}%`),
          is_active: true,
          is_custom: false,
        },
      ],
      take: 5,
      select: [
        'id',
        'name',
        'name_en',
        'category',
        'calories_per_100g',
        'protein_per_100g',
        'fat_per_100g',
        'carbs_per_100g',
        'fiber_per_100g',
        'image_urls',
      ],
    });

    const bestMatch = matched[0];
    const estimated_weight_g = volumeToGrams(
      item.volume_cm3,
      bestMatch?.category,
    );
    const estimated_nutrition = bestMatch
      ? scaleNutrition(bestMatch, estimated_weight_g)
      : null;

    return {
      ai_food_name: item.food_title,
      estimated_volume_cm3: item.volume_cm3,
      estimated_weight_g,
      estimated_nutrition,
      matched_foods: matched.map((f) => ({
        id: f.id,
        name: f.name,
        name_en: f.name_en ?? null,
        category: f.category ?? null,
        calories_per_100g: Number(f.calories_per_100g),
        protein_per_100g: Number(f.protein_per_100g),
        fat_per_100g: Number(f.fat_per_100g),
        carbs_per_100g: Number(f.carbs_per_100g),
        image_urls: f.image_urls ?? null,
      })),
    };
  }
}
