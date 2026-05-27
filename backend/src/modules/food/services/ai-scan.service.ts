import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, ILike } from 'typeorm';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { Food } from '../entities/food.entity';
import { AiScanLog } from '../entities/ai-scan-log.entity';
import { AiScanResultDto } from '../dto/ai-scan-result.dto';

interface PredictResult {
  label: string;
  score: number;
}

const FOOD_AI_URL = process.env.FOOD_AI_SERVICE_URL ?? 'http://localhost:8000';
const HF_CONFIDENCE_THRESHOLD = 0.05;
const HF_TOP_K = 5;
const DEFAULT_WEIGHT_G = 100;

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

    // 2. Call Python food-ai-service
    let predictions: PredictResult[] = [];
    let rawResponse = '';

    try {
      predictions = await this.callFoodAiService(file);
      rawResponse = JSON.stringify(predictions);
    } catch (err) {
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

    // 4. Filter by confidence, take top K, fuzzy-match in DB
    const filtered = predictions
      .filter((r) => r.score >= HF_CONFIDENCE_THRESHOLD)
      .slice(0, HF_TOP_K);

    const results: AiScanResultDto[] = await Promise.all(
      filtered.map((item) => this.buildResult(item)),
    );

    return results;
  }

  private async callFoodAiService(
    file: Express.Multer.File,
  ): Promise<PredictResult[]> {
    const form = new FormData();
    const blob = new Blob([new Uint8Array(file.buffer)], { type: file.mimetype });
    form.append('image', blob, file.originalname || 'image.jpg');

    const response = await fetch(`${FOOD_AI_URL}/predict`, {
      method: 'POST',
      body: form,
    });

    if (!response.ok) {
      const errText = await response.text().catch(() => '');
      throw new Error(`food-ai-service returned ${response.status}: ${errText}`);
    }

    const data = (await response.json()) as PredictResult[];
    if (!Array.isArray(data)) {
      throw new Error(`Unexpected response shape: ${JSON.stringify(data)}`);
    }
    return data;
  }

  private async buildResult(item: PredictResult): Promise<AiScanResultDto> {
    const matched = await this.foodRepo.find({
      where: [
        { name: ILike(`%${item.label}%`), is_active: true, is_custom: false },
        { name_en: ILike(`%${item.label}%`), is_active: true, is_custom: false },
      ],
      take: 5,
      select: [
        'id',
        'name',
        'name_en',
        'calories_per_100g',
        'protein_per_100g',
        'fat_per_100g',
        'carbs_per_100g',
        'image_urls',
      ],
    });

    return {
      ai_food_name: item.label,
      estimated_weight_g: DEFAULT_WEIGHT_G,
      confidence_score: Math.round(item.score * 10_000) / 10_000,
      matched_foods: matched,
    };
  }
}
