import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Brackets, Repository } from 'typeorm';
import { BodyMetric } from '../entities/body-metric.entity';
import { BodyProgressPhoto } from '../entities/body-progress-photo.entity';
import { IBodyMetricsRepository } from './body-metrics.repository.interface';
import { UpsertBodyMetricDto } from '../dto/upsert-body-metric.dto';
import { BodyMetricQueryDto } from '../dto/body-metric-query.dto';

@Injectable()
export class BodyMetricsRepository implements IBodyMetricsRepository {
  constructor(
    @InjectRepository(BodyMetric)
    private readonly repo: Repository<BodyMetric>,
    @InjectRepository(BodyProgressPhoto)
    private readonly photoRepo: Repository<BodyProgressPhoto>,
  ) {}

  async upsert(
    userId: string,
    dto: UpsertBodyMetricDto & { bmi?: number; bmr?: number; tdee?: number },
  ): Promise<BodyMetric> {
    const measuredDate = this.toDateOnly(dto.measuredAt);
    const measuredAt = dto.measuredAt ? new Date(dto.measuredAt) : new Date();

    const existing = await this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere(
        new Brackets((qb) => {
          qb.where('bm.measured_date = :date', { date: measuredDate }).orWhere(
            '(bm.measured_date IS NULL AND DATE(bm.measured_at AT TIME ZONE \'UTC\') = :date)',
            { date: measuredDate },
          );
        }),
      )
      .getOne();

    if (existing) {
      existing.measuredAt = measuredAt;
      existing.measuredDate = measuredDate;
      this.assignMetricFields(existing, dto);
      return this.repo.save(existing);
    }

    const metric = this.repo.create({
      userId,
      measuredAt,
      measuredDate,
      weightKg: dto.weightKg,
      bodyFatPercent: dto.bodyFatPercent,
      waistCm: dto.waistCm,
      hipCm: dto.hipCm,
      chestCm: dto.chestCm,
      neckCm: dto.neckCm,
      heightCm: dto.heightCm,
      armCm: dto.armCm,
      notes: dto.notes,
      bmi: dto.bmi,
      bmr: dto.bmr,
      tdee: dto.tdee,
    });
    try {
      return await this.repo.save(metric);
    } catch (error: any) {
      if (error?.code !== '23505') throw error;
      const conflicted = await this.findByUserAndDate(userId, measuredDate);
      if (!conflicted) throw error;
      conflicted.measuredAt = measuredAt;
      conflicted.measuredDate = measuredDate;
      this.assignMetricFields(conflicted, dto);
      return this.repo.save(conflicted);
    }
  }

  async findByUserAndDate(userId: string, date: string): Promise<BodyMetric | null> {
    return this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere(
        new Brackets((qb) => {
          qb.where('bm.measured_date = :date', { date }).orWhere(
            '(bm.measured_date IS NULL AND DATE(bm.measured_at AT TIME ZONE \'UTC\') = :date)',
            { date },
          );
        }),
      )
      .getOne();
  }

  async findLatest(userId: string): Promise<BodyMetric | null> {
    return this.repo.findOne({
      where: { userId },
      order: { measuredAt: 'DESC' },
    });
  }

  async findHistory(userId: string, query: BodyMetricQueryDto): Promise<BodyMetric[]> {
    const qb = this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .orderBy(this.metricDateExpression('bm'), 'DESC')
      .addOrderBy('bm.measured_at', 'DESC')
      .take(query.limit ?? 30);

    if (query.date) {
      qb.andWhere(`${this.metricDateExpression('bm')} = :date`, { date: query.date });
    } else {
      if (query.fromDate)
        qb.andWhere(`${this.metricDateExpression('bm')} >= :fromDate`, { fromDate: query.fromDate });
      if (query.toDate)
        qb.andWhere(`${this.metricDateExpression('bm')} <= :toDate`, { toDate: query.toDate });
    }

    return qb.getMany();
  }

  async findRange(userId: string, fromDate: string, toDate: string): Promise<BodyMetric[]> {
    return this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere(`${this.metricDateExpression('bm')} >= :fromDate`, { fromDate })
      .andWhere(`${this.metricDateExpression('bm')} <= :toDate`, { toDate })
      .orderBy(this.metricDateExpression('bm'), 'ASC')
      .addOrderBy('bm.measured_at', 'ASC')
      .getMany();
  }

  async savePhoto(data: Partial<BodyProgressPhoto>): Promise<BodyProgressPhoto> {
    const photo = this.photoRepo.create(data);
    return this.photoRepo.save(photo);
  }

  async findPhotosByUser(userId: string, limit: number): Promise<BodyProgressPhoto[]> {
    return this.photoRepo.find({
      where: { userId },
      order: { takenAt: 'DESC' },
      take: limit,
    });
  }

  async findPhotoById(id: string): Promise<BodyProgressPhoto | null> {
    return this.photoRepo.findOne({ where: { id } });
  }

  async deletePhoto(id: string): Promise<void> {
    await this.photoRepo.delete(id);
  }

  private toDateOnly(value?: string): string {
    if (!value) return new Date().toISOString().split('T')[0];
    return value.includes('T') ? new Date(value).toISOString().split('T')[0] : value.slice(0, 10);
  }

  private metricDateExpression(alias: string): string {
    return `COALESCE(${alias}.measured_date, DATE(${alias}.measured_at AT TIME ZONE 'UTC'))`;
  }

  private assignMetricFields(
    metric: BodyMetric,
    dto: UpsertBodyMetricDto & { bmi?: number; bmr?: number; tdee?: number },
  ) {
    if (dto.weightKg !== undefined) metric.weightKg = dto.weightKg;
    if (dto.bodyFatPercent !== undefined) metric.bodyFatPercent = dto.bodyFatPercent;
    if (dto.waistCm !== undefined) metric.waistCm = dto.waistCm;
    if (dto.hipCm !== undefined) metric.hipCm = dto.hipCm;
    if (dto.chestCm !== undefined) metric.chestCm = dto.chestCm;
    if (dto.neckCm !== undefined) metric.neckCm = dto.neckCm;
    if (dto.heightCm !== undefined) metric.heightCm = dto.heightCm;
    if (dto.armCm !== undefined) metric.armCm = dto.armCm;
    if (dto.notes !== undefined) metric.notes = dto.notes;
    if (dto.bmi !== undefined) metric.bmi = dto.bmi;
    if (dto.bmr !== undefined) metric.bmr = dto.bmr;
    if (dto.tdee !== undefined) metric.tdee = dto.tdee;
  }
}
