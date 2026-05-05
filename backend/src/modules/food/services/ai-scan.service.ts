import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, ILike } from 'typeorm';
import ModelClient, { isUnexpected } from '@azure-rest/ai-inference';
import { AzureKeyCredential } from '@azure/core-auth';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { Food } from '../entities/food.entity';
import { AiScanLog } from '../entities/ai-scan-log.entity';
import { AiScanResultDto } from '../dto/ai-scan-result.dto';

interface GeminiDetectedItem {
  food_name: string;
  weight_g: number;
}

@Injectable()
export class AiScanService {
  private readonly client: ReturnType<typeof ModelClient>;
  private readonly model = 'openai/gpt-5';
  private readonly endpoint = 'https://models.github.ai/inference';

  constructor(
    @InjectRepository(Food)
    private readonly foodRepo: Repository<Food>,
    @InjectRepository(AiScanLog)
    private readonly scanLogRepo: Repository<AiScanLog>,
    private readonly cloudinaryService: CloudinaryService,
  ) {
    this.client = ModelClient(
      this.endpoint,
      new AzureKeyCredential(process.env.GITHUB_TOKEN || ''),
    );
  }

  async analyzeImage(
    file: Express.Multer.File,
    userId: string,
  ): Promise<AiScanResultDto[]> {
    // 1. Upload image to Cloudinary for storage
    const { url: image_url, publicId: image_public_id } =
      await this.cloudinaryService.uploadFile(file, 'ai-scans');

    // 2. Build base64 data URI for GPT-5 vision
    const base64Data = file.buffer.toString('base64');
    const dataUri = `data:${file.mimetype};base64,${base64Data}`;

    const prompt =
      'Nhận diện tất cả các món ăn hoặc thực phẩm trong hình. ' +
      'Với mỗi món, ước lượng khối lượng (gram). ' +
      'Trả về JSON array duy nhất, không có markdown, không có giải thích. ' +
      'Format: [{"food_name":"tên tiếng Việt","weight_g":số}]';

    let rawText = '';
    let detectedItems: GeminiDetectedItem[] = [];

    try {
      const response = await this.client.path('/chat/completions').post({
        body: {
          model: this.model,
          messages: [
            {
              role: 'user',
              content: [
                {
                  type: 'image_url',
                  image_url: { url: dataUri },
                },
                {
                  type: 'text',
                  text: prompt,
                },
              ],
            },
          ],
        },
      });

      if (isUnexpected(response)) {
        console.error('[AiScan] API error:', response.body.error);
        throw new Error(response.body.error?.message ?? 'Unknown API error');
      }

      rawText = response.body.choices[0]?.message?.content ?? '';

      // Extract JSON (strip markdown code fences if present)
      const jsonMatch = rawText.match(/\[[\s\S]*\]/);
      if (jsonMatch) {
        detectedItems = JSON.parse(jsonMatch[0]) as GeminiDetectedItem[];
      }
    } catch (err) {
      console.error('[AiScan] Error:', err);
      throw new InternalServerErrorException('AI analysis failed. Please try again.');
    }

    // 3. Save scan log
    await this.scanLogRepo.save(
      this.scanLogRepo.create({
        user_id: userId,
        image_url,
        image_public_id,
        raw_response: rawText,
      }),
    );

    // 4. Fuzzy-match each detected item in DB
    const results: AiScanResultDto[] = await Promise.all(
      detectedItems.map(async (item: GeminiDetectedItem) => {
        const matched = await this.foodRepo.find({
          where: [
            { name: ILike(`%${item.food_name}%`), is_active: true },
            { name_en: ILike(`%${item.food_name}%`), is_active: true },
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
          ai_food_name: item.food_name,
          estimated_weight_g: item.weight_g,
          matched_foods: matched,
        };
      }),
    );

    return results;
  }
}
