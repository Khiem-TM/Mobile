import { BadRequestException } from '@nestjs/common';
import { AiScanService } from './ai-scan.service';
import {
  volumeToGrams,
  scaleNutrition,
  densityForCategory,
} from '../nutrition.util';
import { Food } from '../entities/food.entity';

describe('nutrition.util', () => {
  it('picks category density and falls back to default', () => {
    expect(densityForCategory('Cereal Grains and Pasta')).toBe(0.7);
    expect(densityForCategory('Soups, Sauces')).toBe(1.0);
    expect(densityForCategory('Totally Unknown')).toBe(1.0);
    expect(densityForCategory(null)).toBe(1.0);
  });

  it('converts volume to grams via category density', () => {
    // 100 cm³ of bread (0.3) -> 30 g; unknown -> 1.0 -> 100 g
    expect(volumeToGrams(100, 'Breads & Buns')).toBe(30);
    expect(volumeToGrams(100, undefined)).toBe(100);
  });

  it('scales per-100g macros and coerces decimal strings', () => {
    const food = {
      calories_per_100g: '200',
      protein_per_100g: '10',
      fat_per_100g: '5',
      carbs_per_100g: '25',
      fiber_per_100g: '2',
    } as unknown as Food;

    expect(scaleNutrition(food, 50)).toEqual({
      calories: 100,
      protein: 5,
      fat: 2.5,
      carbs: 12.5,
      fiber: 1,
    });
  });

  it('returns null fiber when the food has none', () => {
    const food = {
      calories_per_100g: '100',
      protein_per_100g: '0',
      fat_per_100g: '0',
      carbs_per_100g: '0',
      fiber_per_100g: null,
    } as unknown as Food;
    expect(scaleNutrition(food, 100).fiber).toBeNull();
  });
});

describe('AiScanService.analyzeImage', () => {
  const cloudinary = {
    uploadFile: jest
      .fn()
      .mockResolvedValue({ url: 'http://img', publicId: 'pid' }),
  };
  const scanLogRepo = {
    create: jest.fn((d) => d),
    save: jest.fn().mockResolvedValue(undefined),
  };

  const file = {
    buffer: Buffer.from([1, 2, 3]),
    mimetype: 'image/jpeg',
    originalname: 'food.jpg',
  } as Express.Multer.File;

  const makeService = (foodRepo: any) =>
    new AiScanService(
      foodRepo as any,
      scanLogRepo as any,
      cloudinary as any,
    );

  afterEach(() => jest.restoreAllMocks());

  const mockFetch = (status: number, body: unknown) => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: status >= 200 && status < 300,
      status,
      json: jest.fn().mockResolvedValue(body),
    }) as unknown as typeof fetch;
  };

  it('maps volume -> weight -> nutrition using the best DB match', async () => {
    mockFetch(200, {
      success: true,
      data: [{ food_title: 'grilled salmon', volume_cm3: 100 }],
    });
    const foodRepo = {
      find: jest.fn().mockResolvedValue([
        {
          id: 'f1',
          name: 'Salmon',
          name_en: 'Grilled Salmon',
          category: 'Finfish and Shellfish Products',
          calories_per_100g: '200',
          protein_per_100g: '20',
          fat_per_100g: '12',
          carbs_per_100g: '0',
          fiber_per_100g: null,
          image_urls: null,
        },
      ]),
    };

    const [result] = await makeService(foodRepo).analyzeImage(file, 'u1');

    expect(result.ai_food_name).toBe('grilled salmon');
    expect(result.estimated_volume_cm3).toBe(100);
    // fish density 1.05 -> 105 g
    expect(result.estimated_weight_g).toBe(105);
    expect(result.estimated_nutrition).toEqual({
      calories: 210,
      protein: 21,
      fat: 12.6,
      carbs: 0,
      fiber: null,
    });
    expect(result.matched_foods[0].calories_per_100g).toBe(200);
  });

  it('returns null nutrition when there is no DB match', async () => {
    mockFetch(200, {
      success: true,
      data: [{ food_title: 'mystery dish', volume_cm3: 50 }],
    });
    const foodRepo = { find: jest.fn().mockResolvedValue([]) };

    const [result] = await makeService(foodRepo).analyzeImage(file, 'u1');

    expect(result.estimated_nutrition).toBeNull();
    // no category -> default density 1.0 -> 50 g
    expect(result.estimated_weight_g).toBe(50);
    expect(result.matched_foods).toEqual([]);
  });

  it('surfaces a 400 "no plate" from the CV service as BadRequest', async () => {
    mockFetch(400, { success: false, detail: 'No plate or bowl detected' });
    const foodRepo = { find: jest.fn() };

    await expect(
      makeService(foodRepo).analyzeImage(file, 'u1'),
    ).rejects.toBeInstanceOf(BadRequestException);
  });
});
