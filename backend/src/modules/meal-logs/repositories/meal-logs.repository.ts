import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MealLog } from '../entities/meal-log.entity';
import { MealLogItem } from '../entities/meal-log-item.entity';
import { IMealLogsRepository } from './meal-logs.repository.interface';
import { MealType } from '../../../common/enums/meal-type.enum';

@Injectable()
export class MealLogsRepository implements IMealLogsRepository {
  constructor(
    @InjectRepository(MealLog)
    private readonly logRepo: Repository<MealLog>,
    @InjectRepository(MealLogItem)
    private readonly itemRepo: Repository<MealLogItem>,
  ) {}

  async findOrCreate(userId: string, date: string, type: MealType): Promise<MealLog> {
    let log = await this.logRepo
      .createQueryBuilder('ml')
      .where('ml.user_id = :userId', { userId })
      .andWhere('ml.meal_type = :type', { type })
      .andWhere("DATE(ml.log_date AT TIME ZONE 'UTC') = :date", { date })
      .leftJoinAndSelect('ml.items', 'items')
      .leftJoinAndSelect('items.food', 'food')
      .getOne();

    if (!log) {
      log = this.logRepo.create({
        user_id: userId,
        log_date: new Date(date),
        meal_type: type,
      });
      log = await this.logRepo.save(log);
      log.items = [];
    }

    return log;
  }

  async findById(id: string): Promise<MealLog | null> {
    return this.logRepo.findOne({
      where: { id },
      relations: ['items', 'items.food'],
    });
  }

  async findByUser(userId: string, date?: string): Promise<MealLog[]> {
    const qb = this.logRepo
      .createQueryBuilder('ml')
      .where('ml.user_id = :userId', { userId })
      .leftJoinAndSelect('ml.items', 'items')
      .leftJoinAndSelect('items.food', 'food')
      .orderBy('ml.meal_type', 'ASC');

    if (date) {
      qb.andWhere("DATE(ml.log_date AT TIME ZONE 'UTC') = :date", { date });
    }

    return qb.getMany();
  }

  async updateLog(id: string, data: Partial<MealLog>): Promise<MealLog> {
    await this.logRepo.update(id, data);
    return this.logRepo.findOne({ where: { id }, relations: ['items', 'items.food'] }) as Promise<MealLog>;
  }

  async deleteLog(id: string): Promise<void> {
    await this.logRepo.delete(id);
  }

  async saveItem(item: Partial<MealLogItem>): Promise<MealLogItem> {
    const newItem = this.itemRepo.create(item);
    return this.itemRepo.save(newItem);
  }

  async updateItem(itemId: string, data: Partial<MealLogItem>): Promise<MealLogItem> {
    await this.itemRepo.update(itemId, data);
    return this.itemRepo.findOne({ where: { id: itemId } }) as Promise<MealLogItem>;
  }

  async removeItem(itemId: string): Promise<void> {
    await this.itemRepo.delete(itemId);
  }

  async findItemById(itemId: string): Promise<MealLogItem | null> {
    return this.itemRepo.findOne({
      where: { id: itemId },
      relations: ['meal_log'],
    });
  }

  async updateImage(
    id: string,
    imageUrl: string | null,
    imagePublicId: string | null,
  ): Promise<MealLog> {
    await this.logRepo.update(id, {
      image_url: imageUrl,
      image_public_id: imagePublicId,
    });
    return this.logRepo.findOne({
      where: { id },
      relations: ['items', 'items.food'],
    }) as Promise<MealLog>;
  }
}
