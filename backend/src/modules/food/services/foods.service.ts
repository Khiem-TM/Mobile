import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, Repository, ILike } from 'typeorm';
import { Food } from '../entities/food.entity';
import { FoodBarcode } from '../entities/food-barcode.entity';
import { FoodUserFavorite } from '../entities/food-user-favorite.entity';
import { CreateFoodDto } from '../dto/create-food.dto';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { RedisService } from '../../support/redis/redis.service';

const TTL = {
  FOOD_LIST: 300,    // 5 min
  FOOD_ONE: 3600,    // 1 hour
  FOOD_BARCODE: 3600,
  FOOD_EXPLORE: 600, // 10 min
};

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
    private readonly dataSource: DataSource,
    private readonly redisService: RedisService,
  ) {}

  private async invalidateFoodCache(foodId?: string): Promise<void> {
    await Promise.all([
      foodId ? this.redisService.del(`cache:foods:one:${foodId}`) : Promise.resolve(),
      this.redisService.delByPattern('cache:foods:list:*'),
      this.redisService.delByPattern('cache:foods:explore:*'),
    ]);
  }

  // System foods only — visible to all users
  async findAll(query: string = '', page: number = 1, limit: number = 20) {
    const key = `cache:foods:list:${query}:${page}:${limit}`;
    const cached = await this.redisService.getJson<{ items: Food[]; total: number; page: number; limit: number }>(key);
    if (cached) return cached;

    const where: any = { is_custom: false, is_active: true };
    if (query) where.name = ILike(`%${query}%`);

    const [foods, total] = await this.foodRepository.findAndCount({
      where,
      take: limit,
      skip: (page - 1) * limit,
      order: { favorites_count: 'DESC', created_at: 'DESC' },
    });
    const result = { items: foods, total, page, limit };
    await this.redisService.setJson(key, result, TTL.FOOD_LIST);
    return result;
  }

  // System dishes only — for explore/browse page
  async exploreDishe(page: number = 1, limit: number = 20, category?: string) {
    const key = `cache:foods:explore:${page}:${limit}:${category ?? ''}`;
    const cached = await this.redisService.getJson<{ items: Food[]; total: number; page: number; limit: number }>(key);
    if (cached) return cached;

    const where: any = { food_type: 'dish', is_active: true, is_custom: false };
    if (category) where.category = ILike(`%${category}%`);

    const [items, total] = await this.foodRepository.findAndCount({
      where,
      take: limit,
      skip: (page - 1) * limit,
      order: { favorites_count: 'DESC', created_at: 'DESC' },
    });
    const result = { items, total, page, limit };
    await this.redisService.setJson(key, result, TTL.FOOD_EXPLORE);
    return result;
  }

  async findOne(id: string): Promise<Food> {
    const key = `cache:foods:one:${id}`;
    const cached = await this.redisService.getJson<Food>(key);
    if (cached && !cached.is_custom && cached.is_active) return cached;

    const food = await this.foodRepository.findOne({
      where: { id, is_custom: false, is_active: true },
    });
    if (!food) throw new NotFoundException('Food not found');
    await this.redisService.setJson(key, food, TTL.FOOD_ONE);
    return food;
  }

  // Lookup by ID — allows access to custom food only if owned by the user
  async findOneForUser(id: string, userId: string): Promise<Food> {
    const food = await this.foodRepository.findOne({ where: { id } });
    if (!food || !food.is_active) throw new NotFoundException('Food not found');
    if (food.is_custom && food.created_by_user_id !== userId) {
      throw new ForbiddenException('This food is private');
    }
    return food;
  }

  async findCustomForUser(id: string, userId: string): Promise<Food> {
    const food = await this.foodRepository.findOne({
      where: {
        id,
        is_custom: true,
        created_by_user_id: userId,
        is_active: true,
      },
    });
    if (!food) throw new NotFoundException('Food not found');
    return food;
  }

  async createCustom(userId: string, data: CreateFoodDto): Promise<Food> {
    const food = this.foodRepository.create({
      name: data.name,
      name_en: data.name_en,
      brand: data.brand,
      category: data.category,
      description: data.description,
      food_type: data.food_type ?? 'ingredient',
      serving_size_g: data.serving_size_g ?? 100,
      serving_unit: data.serving_unit ?? 'g',
      calories_per_100g: data.calories_per_100g,
      protein_per_100g: data.protein_per_100g ?? 0,
      fat_per_100g: data.fat_per_100g ?? 0,
      carbs_per_100g: data.carbs_per_100g ?? 0,
      fiber_per_100g: data.fiber_per_100g,
      image_urls: data.image_urls,
      is_custom: true,
      created_by_user_id: userId,
      is_verified: false,
    });
    const saved = await this.foodRepository.save(food);
    await this.invalidateFoodCache();
    return saved;
  }

  async updateCustom(userId: string, id: string, data: Partial<CreateFoodDto>): Promise<Food> {
    const food = await this.foodRepository.findOne({ where: { id } });
    if (!food || !food.is_active) throw new NotFoundException('Food not found');
    if (!food.is_custom) throw new ForbiddenException('Cannot edit system foods');
    if (food.created_by_user_id !== userId) throw new ForbiddenException('Not your food');

    if (data.name !== undefined) food.name = data.name;
    if (data.name_en !== undefined) food.name_en = data.name_en;
    if (data.brand !== undefined) food.brand = data.brand;
    if (data.category !== undefined) food.category = data.category;
    if (data.description !== undefined) food.description = data.description;
    if (data.food_type !== undefined) food.food_type = data.food_type;
    if (data.serving_size_g !== undefined) food.serving_size_g = data.serving_size_g;
    if (data.serving_unit !== undefined) food.serving_unit = data.serving_unit;
    if (data.calories_per_100g !== undefined) food.calories_per_100g = data.calories_per_100g;
    if (data.protein_per_100g !== undefined) food.protein_per_100g = data.protein_per_100g;
    if (data.fat_per_100g !== undefined) food.fat_per_100g = data.fat_per_100g;
    if (data.carbs_per_100g !== undefined) food.carbs_per_100g = data.carbs_per_100g;
    if (data.fiber_per_100g !== undefined) food.fiber_per_100g = data.fiber_per_100g;
    if (data.image_urls !== undefined) food.image_urls = data.image_urls;

    const saved = await this.foodRepository.save(food);
    await this.invalidateFoodCache(id);
    return saved;
  }

  async deleteCustom(userId: string, id: string): Promise<void> {
    const food = await this.foodRepository.findOne({ where: { id } });
    if (!food || !food.is_active) throw new NotFoundException('Food not found');
    if (!food.is_custom) throw new ForbiddenException('Cannot delete system foods');
    if (food.created_by_user_id !== userId) throw new ForbiddenException('Not your food');
    food.is_active = false;
    await this.foodRepository.save(food);
    await this.invalidateFoodCache(id);
  }

  async getUserCustomFoods(userId: string, query = '', page = 1, limit = 20) {
    const where: any = { is_custom: true, created_by_user_id: userId, is_active: true };
    if (query) where.name = ILike(`%${query}%`);

    const [items, total] = await this.foodRepository.findAndCount({
      where,
      order: { created_at: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async getFavorites(userId: string): Promise<Food[]> {
    const favs = await this.favoriteRepository.find({
      where: { user_id: userId },
      relations: ['food'],
    });
    return favs
      .map((f) => f.food)
      .filter(
        (food) =>
          food?.is_active &&
          (!food.is_custom || food.created_by_user_id === userId),
      );
  }

  async addFavorite(userId: string, foodId: string): Promise<void> {
    await this.findOneForUser(foodId, userId);
    await this.dataSource.transaction(async (manager) => {
      const result = await manager
        .createQueryBuilder()
        .insert()
        .into(FoodUserFavorite)
        .values({ user_id: userId, food_id: foodId })
        .orIgnore()
        .returning(['user_id', 'food_id'])
        .execute();

      if (result.raw?.length) {
        await manager.increment(Food, { id: foodId }, 'favorites_count', 1);
      }
    });
    await this.invalidateFoodCache(foodId);
  }

  async removeFavorite(userId: string, foodId: string): Promise<void> {
    const existing = await this.favoriteRepository.findOne({
      where: { user_id: userId, food_id: foodId },
    });
    if (existing) {
      await this.dataSource.transaction(async (manager) => {
        await manager.delete(FoodUserFavorite, { user_id: userId, food_id: foodId });
        await manager
          .createQueryBuilder()
          .update(Food)
          .set({ favorites_count: () => 'GREATEST(favorites_count - 1, 0)' })
          .where('id = :foodId', { foodId })
          .execute();
      });
      await this.invalidateFoodCache(foodId);
    }
  }

  async uploadImage(foodId: string, userId: string, file: Express.Multer.File): Promise<Food> {
    const food = await this.findOneForUser(foodId, userId);
    if (!food.is_custom) throw new ForbiddenException('Users cannot edit system foods');
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'foods');
    food.image_urls = [...(food.image_urls ?? []), url];
    food.image_public_ids = [...(food.image_public_ids ?? []), publicId];
    const saved = await this.foodRepository.save(food);
    await this.invalidateFoodCache(foodId);
    return saved;
  }

  async removeImage(foodId: string, userId: string, publicId: string): Promise<Food> {
    const food = await this.findOneForUser(foodId, userId);
    if (!food.is_custom) throw new ForbiddenException('Users cannot edit system foods');
    const idx = (food.image_public_ids ?? []).indexOf(publicId);
    if (idx === -1) throw new NotFoundException('Image not found on this food');
    await this.cloudinaryService.deleteFile(publicId);
    food.image_public_ids = (food.image_public_ids ?? []).filter((_, i) => i !== idx);
    food.image_urls = (food.image_urls ?? []).filter((_, i) => i !== idx);
    const saved = await this.foodRepository.save(food);
    await this.invalidateFoodCache(foodId);
    return saved;
  }

  async findByBarcode(barcode: string): Promise<Food> {
    const key = `cache:foods:barcode:${barcode}`;
    const cached = await this.redisService.getJson<Food>(key);
    if (cached && !cached.is_custom && cached.is_active) return cached;

    const record = await this.barcodeRepository.findOne({
      where: { barcode },
      relations: ['food'],
    });
    if (record?.food) {
      if (record.food.is_custom || !record.food.is_active) {
        throw new NotFoundException(`Food with barcode ${barcode} not found`);
      }
      await this.redisService.setJson(key, record.food, TTL.FOOD_BARCODE);
      return record.food;
    }

    // Fallback: Open Food Facts API
    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v3/product/${barcode}.json`);
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
          fiber_per_100g: n['fiber_100g'] ?? null,
          is_verified: false,
          is_active: true,
          is_custom: false,
        });
        const saved = await this.foodRepository.save(food);
        await this.barcodeRepository.save({ food_id: saved.id, barcode });
        await this.redisService.setJson(key, saved, TTL.FOOD_BARCODE);
        return saved;
      }
    } catch {
      // ignore external API errors
    }

    throw new NotFoundException(`Food with barcode ${barcode} not found`);
  }
}
