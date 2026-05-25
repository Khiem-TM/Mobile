import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { TrainingSession } from '../entities/training-session.entity';
import { TrainingSessionItem } from '../entities/training-session-item.entity';
import { ITrainingSessionsRepository } from './training-sessions.repository.interface';

@Injectable()
export class TrainingSessionsRepository implements ITrainingSessionsRepository {
  constructor(
    @InjectRepository(TrainingSession)
    private readonly repo: Repository<TrainingSession>,
    @InjectRepository(TrainingSessionItem)
    private readonly itemRepo: Repository<TrainingSessionItem>,
  ) {}

  async createSession(data: Partial<TrainingSession>): Promise<TrainingSession> {
    const session = this.repo.create(data);
    return this.repo.save(session);
  }

  async findByUser(userId: string, limit: number): Promise<TrainingSession[]> {
    return this.repo.find({
      where: { userId },
      relations: ['items', 'items.exercise'],
      order: { sessionDate: 'DESC' },
      take: limit,
    });
  }

  async findByDateRange(userId: string, from: string, to: string): Promise<TrainingSession[]> {
    return this.repo
      .createQueryBuilder('ts')
      .leftJoinAndSelect('ts.items', 'items')
      .leftJoinAndSelect('items.exercise', 'exercise')
      .where('ts.userId = :userId', { userId })
      .andWhere('ts.sessionDate BETWEEN :from AND :to', { from, to })
      .orderBy('ts.sessionDate', 'ASC')
      .addOrderBy('items.orderIndex', 'ASC')
      .getMany();
  }

  async findById(id: string): Promise<TrainingSession | null> {
    return this.repo.findOne({
      where: { id },
      relations: ['items', 'items.exercise'],
    });
  }

  async updateTotals(id: string, totalDurationMinutes: number, totalCaloriesBurned: number): Promise<void> {
    await this.repo.update(id, { totalDurationMinutes, totalCaloriesBurned });
  }

  async updateSession(id: string, data: Partial<TrainingSession>): Promise<TrainingSession> {
    await this.repo.update(id, data);
    return this.repo.findOne({
      where: { id },
      relations: ['items', 'items.exercise'],
    }) as Promise<TrainingSession>;
  }

  async deleteSession(id: string): Promise<void> {
    await this.repo.delete(id);
  }

  async sumCaloriesForDate(userId: string, date: string): Promise<number> {
    const result = await this.repo
      .createQueryBuilder('ts')
      .select('COALESCE(SUM(ts.totalCaloriesBurned), 0)', 'total')
      .where('ts.userId = :userId AND ts.sessionDate = :date', { userId, date })
      .getRawOne();
    return Number(result?.total ?? 0);
  }

  async addItem(data: Partial<TrainingSessionItem>): Promise<TrainingSessionItem> {
    const item = this.itemRepo.create(data);
    return this.itemRepo.save(item);
  }

  async findItemById(id: string): Promise<TrainingSessionItem | null> {
    return this.itemRepo.findOne({ where: { id }, relations: ['exercise'] });
  }

  async updateItem(id: string, data: Partial<TrainingSessionItem>): Promise<TrainingSessionItem> {
    await this.itemRepo.update(id, data);
    return this.itemRepo.findOne({ where: { id }, relations: ['exercise'] }) as Promise<TrainingSessionItem>;
  }

  async deleteItem(id: string): Promise<void> {
    await this.itemRepo.delete(id);
  }

  async findItemsBySession(sessionId: string): Promise<TrainingSessionItem[]> {
    return this.itemRepo.find({
      where: { sessionId },
      order: { orderIndex: 'ASC' },
    });
  }
}
