import { Injectable, NotFoundException, Inject } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MealLog } from './entities/meal-log.entity';
import { MealLogItem } from './entities/meal-log-item.entity';
import {
  CreateMealLogDto,
  CreateMealLogItemDto,
} from './dto/create-meal-log.dto';
import { UpdateMealLogDto, UpdateMealLogItemDto } from './dto/update-meal-log.dto';
import { UploadImageDto } from './dto/upload-image.dto';
import { Food } from '../foods/entities/food.entity';
import { MEAL_LOGS_REPOSITORY } from './meal-logs.constants';
import type { IMealLogsRepository } from './repositories/meal-logs.repository.interface';
import { CloudinaryService } from '../cloudinary/cloudinary.service';
import { StreaksService } from '../streaks/streaks.service';
import { StreakType } from '../../common/enums/streak-type.enum';

@Injectable()
export class MealLogsService {
  constructor(
    @Inject(MEAL_LOGS_REPOSITORY)
    private readonly repository: IMealLogsRepository,
    @InjectRepository(Food)
    private readonly foodRepository: Repository<Food>,
    private readonly cloudinaryService: CloudinaryService,
    private readonly streaksService: StreaksService,
  ) {}

  async create(userId: string, dto: CreateMealLogDto): Promise<MealLog> {
    const logDate = dto.log_date || new Date().toISOString().split('T')[0];
    const log = await this.repository.findOrCreate(
      userId,
      logDate,
      dto.meal_type,
    );

    if (dto.items && dto.items.length > 0) {
      for (const itemDto of dto.items) {
        await this.addItemToLog(userId, log.id, itemDto);
      }
    }

    await this.streaksService.updateActivity(userId, StreakType.CALORIE_GOAL, logDate);

    return this.repository.findById(log.id) as Promise<MealLog>;
  }

  async findAllByUser(userId: string, date?: string): Promise<MealLog[]> {
    const effectiveDate = date || new Date().toISOString().split('T')[0];
    return this.repository.findByUser(userId, effectiveDate);
  }

  async findOne(userId: string, id: string): Promise<MealLog> {
    const log = await this.repository.findById(id);
    if (!log || log.user_id !== userId) {
      throw new NotFoundException('Meal log not found');
    }
    return log;
  }

  async updateLog(userId: string, id: string, dto: UpdateMealLogDto): Promise<MealLog> {
    await this.findOne(userId, id);
    return this.repository.updateLog(id, dto);
  }

  async deleteLog(userId: string, id: string): Promise<void> {
    const log = await this.findOne(userId, id);
    if (log.image_public_id) {
      await this.cloudinaryService.deleteFile(log.image_public_id);
    }
    return this.repository.deleteLog(id);
  }

  async uploadImage(
    userId: string,
    id: string,
    file: Express.Multer.File,
  ): Promise<MealLog> {
    const log = await this.findOne(userId, id);
    if (log.image_public_id) {
      await this.cloudinaryService.deleteFile(log.image_public_id);
    }
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'meal-logs');
    return this.repository.updateImage(id, url, publicId);
  }

  async uploadBase64Image(
    userId: string,
    id: string,
    dto: UploadImageDto,
  ): Promise<MealLog> {
    const log = await this.findOne(userId, id);
    if (log.image_public_id) {
      await this.cloudinaryService.deleteFile(log.image_public_id);
    }
    const { url, publicId } = await this.cloudinaryService.uploadBase64(dto.imageData, 'meal-logs');
    return this.repository.updateImage(id, url, publicId);
  }

  async addItemToLog(
    userId: string,
    logId: string,
    itemDto: CreateMealLogItemDto,
  ): Promise<MealLogItem> {
    const log = await this.findOne(userId, logId);

    const food = await this.foodRepository.findOne({ where: { id: itemDto.food_id } });
    if (!food) throw new NotFoundException('Food not found');

    const servingSizeG = Number(food.serving_size_g) || 100;
    let quantity_in_grams = Number(itemDto.quantity);
    if (itemDto.serving_unit.toLowerCase() !== 'g') {
      quantity_in_grams = Number(itemDto.quantity) * servingSizeG;
    }

    const ratio = quantity_in_grams / 100;
    const itemData: Partial<MealLogItem> = {
      meal_log_id: log.id,
      food_id: food.id,
      quantity: Number(itemDto.quantity),
      serving_unit: itemDto.serving_unit,
      quantity_in_grams,
      calories_snapshot: Number(food.calories_per_100g) * ratio,
      protein_snapshot: Number(food.protein_per_100g) * ratio,
      fat_snapshot: Number(food.fat_per_100g) * ratio,
      carbs_snapshot: Number(food.carbs_per_100g) * ratio,
      fiber_snapshot: Number(food.fiber_per_100g || 0) * ratio,
      sugar_snapshot: Number(food.sugar_per_100g || 0) * ratio,
      sodium_snapshot: Number(food.sodium_per_100g || 0) * ratio,
      source: itemDto.source || 'manual',
    };

    return this.repository.saveItem(itemData);
  }

  async updateItem(
    userId: string,
    logId: string,
    itemId: string,
    dto: UpdateMealLogItemDto,
  ): Promise<MealLogItem> {
    const item = await this.repository.findItemById(itemId);
    if (!item || item.meal_log_id !== logId) {
      throw new NotFoundException('Item not found');
    }
    const log = await this.repository.findById(logId);
    if (!log || log.user_id !== userId) {
      throw new NotFoundException('Meal log not found');
    }

    const food = await this.foodRepository.findOne({ where: { id: item.food_id } });
    if (!food) throw new NotFoundException('Food not found');

    const newQuantity = Number(dto.quantity ?? item.quantity);
    const newUnit = dto.serving_unit ?? item.serving_unit ?? 'g';
    const servingSizeG = Number(food.serving_size_g) || 100;

    let quantity_in_grams = newQuantity;
    if (newUnit.toLowerCase() !== 'g') {
      quantity_in_grams = newQuantity * servingSizeG;
    }

    const ratio = quantity_in_grams / 100;
    return this.repository.updateItem(itemId, {
      quantity: newQuantity,
      serving_unit: newUnit,
      quantity_in_grams,
      calories_snapshot: Number(food.calories_per_100g) * ratio,
      protein_snapshot: Number(food.protein_per_100g) * ratio,
      fat_snapshot: Number(food.fat_per_100g) * ratio,
      carbs_snapshot: Number(food.carbs_per_100g) * ratio,
      fiber_snapshot: Number(food.fiber_per_100g || 0) * ratio,
      sugar_snapshot: Number(food.sugar_per_100g || 0) * ratio,
      sodium_snapshot: Number(food.sodium_per_100g || 0) * ratio,
    });
  }

  async removeItemFromLog(
    userId: string,
    logId: string,
    itemId: string,
  ): Promise<void> {
    const item = await this.repository.findItemById(itemId);
    if (!item || item.meal_log_id !== logId) {
      throw new NotFoundException('Item not found');
    }
    const log = await this.repository.findById(logId);
    if (!log || log.user_id !== userId) {
      throw new NotFoundException('Meal log not found');
    }
    await this.repository.removeItem(itemId);
  }

  async getDailySummaryRange(
    userId: string,
    fromDate: string,
    toDate: string,
  ): Promise<Record<string, ReturnType<typeof this._summarizeLogs>>> {
    const allLogs = await this.repository.findByRange(userId, fromDate, toDate);

    const byDate: Record<string, typeof allLogs> = {};
    for (const log of allLogs) {
      const dateStr = new Date(log.log_date).toISOString().split('T')[0];
      if (!byDate[dateStr]) byDate[dateStr] = [];
      byDate[dateStr].push(log);
    }

    // Build a zero-entry for every date in range
    const result: Record<string, ReturnType<typeof this._summarizeLogs>> = {};
    const current = new Date(fromDate);
    const end = new Date(toDate);
    while (current <= end) {
      const dateStr = current.toISOString().split('T')[0];
      result[dateStr] = this._summarizeLogs(dateStr, byDate[dateStr] || []);
      current.setDate(current.getDate() + 1);
    }
    return result;
  }

  private _summarizeLogs(date: string, logs: import('./entities/meal-log.entity').MealLog[]) {
    let totalCalories = 0;
    let totalProtein = 0;
    let totalFat = 0;
    let totalCarbs = 0;
    let totalFiber = 0;
    for (const log of logs) {
      for (const item of log.items || []) {
        totalCalories += Number(item.calories_snapshot || 0);
        totalProtein += Number(item.protein_snapshot || 0);
        totalFat += Number(item.fat_snapshot || 0);
        totalCarbs += Number(item.carbs_snapshot || 0);
        totalFiber += Number(item.fiber_snapshot || 0);
      }
    }
    return {
      date,
      total_calories: Math.round(totalCalories * 100) / 100,
      total_protein: Math.round(totalProtein * 100) / 100,
      total_fat: Math.round(totalFat * 100) / 100,
      total_carbs: Math.round(totalCarbs * 100) / 100,
      total_fiber: Math.round(totalFiber * 100) / 100,
      logs,
    };
  }

  async getDailySummary(userId: string, date?: string) {
    const effectiveDate = date || new Date().toISOString().split('T')[0];
    const logs = await this.findAllByUser(userId, effectiveDate);
    return this._summarizeLogs(effectiveDate, logs);
  }
}
