import { BodyMetric } from '../entities/body-metric.entity';
import { BodyProgressPhoto } from '../entities/body-progress-photo.entity';
import { UpsertBodyMetricDto } from '../dto/upsert-body-metric.dto';
import { BodyMetricQueryDto } from '../dto/body-metric-query.dto';

export interface IBodyMetricsRepository {
  upsert(
    userId: string,
    dto: UpsertBodyMetricDto & { bmi?: number; bmr?: number; tdee?: number },
  ): Promise<BodyMetric>;
  findByUserAndDate(userId: string, date: string): Promise<BodyMetric | null>;
  findLatest(userId: string): Promise<BodyMetric | null>;
  findHistory(userId: string, query: BodyMetricQueryDto): Promise<BodyMetric[]>;
  findRange(
    userId: string,
    fromDate: string,
    toDate: string,
  ): Promise<BodyMetric[]>;
  savePhoto(data: Partial<BodyProgressPhoto>): Promise<BodyProgressPhoto>;
  findPhotosByUser(userId: string, limit: number): Promise<BodyProgressPhoto[]>;
  findPhotoById(id: string): Promise<BodyProgressPhoto | null>;
  deletePhoto(id: string): Promise<void>;
}
