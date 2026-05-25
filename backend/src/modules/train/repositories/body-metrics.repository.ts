import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
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
    const dateStr = dto.measuredAt
      ? new Date(dto.measuredAt).toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0];

    const existing = await this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere("DATE(bm.measured_at AT TIME ZONE 'UTC') = :date", { date: dateStr })
      .getOne();

    if (existing) {
      if (dto.weightKg !== undefined) existing.weightKg = dto.weightKg;
      if (dto.bodyFatPercent !== undefined) existing.bodyFatPercent = dto.bodyFatPercent;
      if (dto.waistCm !== undefined) existing.waistCm = dto.waistCm;
      if (dto.hipCm !== undefined) existing.hipCm = dto.hipCm;
      if (dto.chestCm !== undefined) existing.chestCm = dto.chestCm;
      if (dto.neckCm !== undefined) existing.neckCm = dto.neckCm;
      if (dto.heightCm !== undefined) existing.heightCm = dto.heightCm;
      if (dto.armCm !== undefined) existing.armCm = dto.armCm;
      if (dto.notes !== undefined) existing.notes = dto.notes;
      if (dto.bmi !== undefined) existing.bmi = dto.bmi;
      if (dto.bmr !== undefined) existing.bmr = dto.bmr;
      if (dto.tdee !== undefined) existing.tdee = dto.tdee;
      return this.repo.save(existing);
    }

    const metric = this.repo.create({
      userId,
      measuredAt: dto.measuredAt ? new Date(dto.measuredAt) : new Date(),
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
    return this.repo.save(metric);
  }

  async findByUserAndDate(userId: string, date: string): Promise<BodyMetric | null> {
    return this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere("DATE(bm.measured_at AT TIME ZONE 'UTC') = :date", { date })
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
      .orderBy('bm.measured_at', 'DESC')
      .take(query.limit ?? 30);

    if (query.date) {
      qb.andWhere("DATE(bm.measured_at AT TIME ZONE 'UTC') = :date", { date: query.date });
    } else {
      if (query.fromDate)
        qb.andWhere('bm.measured_at >= :fromDate', { fromDate: query.fromDate });
      if (query.toDate)
        qb.andWhere('bm.measured_at <= :toDate', { toDate: query.toDate });
    }

    return qb.getMany();
  }

  async findRange(userId: string, fromDate: string, toDate: string): Promise<BodyMetric[]> {
    return this.repo
      .createQueryBuilder('bm')
      .where('bm.user_id = :userId', { userId })
      .andWhere('bm.measured_at >= :fromDate', { fromDate })
      .andWhere('bm.measured_at <= :toDate', { toDate })
      .orderBy('bm.measured_at', 'ASC')
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
}
