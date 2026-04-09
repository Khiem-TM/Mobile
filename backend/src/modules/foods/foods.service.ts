import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, ILike } from 'typeorm';
import { Food } from './entities/food.entity';
import { FoodBarcode } from './entities/food-barcode.entity';
import { FoodUserFavorite } from './entities/food-user-favorite.entity';
import { CreateFoodDto } from './dto/create-food.dto';
import { CloudinaryService } from '../cloudinary/cloudinary.service';

@Injectable()
export class FoodsService {
  constructor(
    @InjectRepository(Food)
    private readonly foodRepository: Repository<Food>,
    @InjectRepository(FoodBarcode)
    private readonly barcodeRepository: Repository<FoodBarcode>,
    @InjectRepository(FoodUserFavorite)
    private readonly favoriteRepository: Repository<FoodUserFavorite>,
    private readonly cloudinaryService: CloudinaryService,
  ) {}

  async findAll(query: string = '', page: number = 1, limit: number = 20) {
    const [foods, total] = await this.foodRepository.findAndCount({
      where: query ? [{ name: ILike(`%${query}%`) }] : {},
      take: limit,
      skip: (page - 1) * limit,
    });
    return { items: foods, total, page, limit };
  }

  async findOne(id: string): Promise<Food> {
    const food = await this.foodRepository.findOne({ where: { id } });
    if (!food) throw new NotFoundException('Food not found');
    return food;
  }

  async createCustom(userId: string, data: CreateFoodDto): Promise<Food> {
    const food = this.foodRepository.create({
      name: data.name,
      name_en: data.name_en,
      brand: data.brand,
      category: data.category,
      food_type: data.food_type,
      serving_size_g: data.serving_size_g,
      serving_unit: data.serving_unit,
      calories_per_100g: data.calories_per_100g,
      protein_per_100g: data.protein_per_100g,
      fat_per_100g: data.fat_per_100g,
      carbs_per_100g: data.carbs_per_100g,
      fiber_per_100g: data.fiber_per_100g,
      sugar_per_100g: data.sugar_per_100g,
      sodium_per_100g: data.sodium_per_100g,
      cholesterol_per_100g: data.cholesterol_per_100g,
      image_urls: data.image_urls,
      is_custom: true,
      created_by_user_id: userId,
      is_verified: false,
    });
    return this.foodRepository.save(food);
  }

  async getFavorites(userId: string): Promise<Food[]> {
    const favs = await this.favoriteRepository.find({
      where: { user_id: userId },
      relations: ['food'],
    });
    return favs.map((f) => f.food);
  }

  async addFavorite(userId: string, foodId: string): Promise<void> {
    const existing = await this.favoriteRepository.findOne({
      where: { user_id: userId, food_id: foodId },
    });
    if (!existing) {
      await this.favoriteRepository.save({ user_id: userId, food_id: foodId });
    }
  }

  async removeFavorite(userId: string, foodId: string): Promise<void> {
    await this.favoriteRepository.delete({ user_id: userId, food_id: foodId });
  }

  async uploadImage(foodId: string, file: Express.Multer.File): Promise<Food> {
    const food = await this.findOne(foodId);
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'foods');
    food.image_urls = [...(food.image_urls ?? []), url];
    food.image_public_ids = [...(food.image_public_ids ?? []), publicId];
    return this.foodRepository.save(food);
  }

  async removeImage(foodId: string, publicId: string): Promise<Food> {
    const food = await this.findOne(foodId);
    const idx = (food.image_public_ids ?? []).indexOf(publicId);
    if (idx === -1) throw new NotFoundException('Image not found on this food');
    await this.cloudinaryService.deleteFile(publicId);
    food.image_public_ids = (food.image_public_ids ?? []).filter((_, i) => i !== idx);
    food.image_urls = (food.image_urls ?? []).filter((_, i) => i !== idx);
    return this.foodRepository.save(food);
  }

  async findByBarcode(barcode: string): Promise<Food> {
    const record = await this.barcodeRepository.findOne({
      where: { barcode },
      relations: ['food'],
    });
    if (record?.food) return record.food;

    // Fallback: Open Food Facts API
    try {
      const res = await fetch(
        `https://world.openfoodfacts.org/api/v3/product/${barcode}.json`,
      );
      const data = await res.json() as any;
      if (data.status === 'success' && data.product) {
        const p = data.product;
        const n = p.nutriments ?? {};
        const food = this.foodRepository.create({
          name: p.product_name || p.abbreviated_product_name || barcode,
          name_en: p.product_name_en || null,
          brand: p.brands || null,
          category: p.categories_tags?.[0]?.replace('en:', '') || null,
          food_type: 'product',
          serving_size_g: parseFloat(p.serving_quantity) || 100,
          calories_per_100g: n['energy-kcal_100g'] ?? 0,
          protein_per_100g: n['proteins_100g'] ?? 0,
          fat_per_100g: n['fat_100g'] ?? 0,
          carbs_per_100g: n['carbohydrates_100g'] ?? 0,
          fiber_per_100g: n['fiber_100g'] ?? 0,
          sugar_per_100g: n['sugars_100g'] ?? 0,
          sodium_per_100g: n['sodium_100g'] ? n['sodium_100g'] * 1000 : 0,
          is_verified: false,
          is_active: true,
          is_custom: false,
        });
        const saved = await this.foodRepository.save(food);
        await this.barcodeRepository.save({ food_id: saved.id, barcode });
        return saved;
      }
    } catch {
      // ignore external API errors
    }

    throw new NotFoundException(`Food with barcode ${barcode} not found`);
  }
}
