import {
  Injectable,
  Inject,
  NotFoundException,
  ForbiddenException,
  forwardRef,
} from '@nestjs/common';
import { BODY_METRICS_REPOSITORY } from '../train.constants';
import type { IBodyMetricsRepository } from '../repositories/body-metrics.repository.interface';
import { UpsertBodyMetricDto } from '../dto/upsert-body-metric.dto';
import { BodyMetricQueryDto } from '../dto/body-metric-query.dto';
import { UsersService } from '../../user/services/users.service';
import { BMIUtil } from '../../../common/utils/bmi.util';
import { TDEEUtil } from '../../../common/utils/tdee.util';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';

interface MetricCalculated extends UpsertBodyMetricDto {
  bmi?: number;
  bmr?: number;
  tdee?: number;
}

@Injectable()
export class BodyMetricsService {
  constructor(
    @Inject(BODY_METRICS_REPOSITORY)
    private readonly repository: IBodyMetricsRepository,
    @Inject(forwardRef(() => UsersService))
    private readonly usersService: UsersService,
    private readonly cloudinaryService: CloudinaryService,
  ) {}

  async upsert(userId: string, dto: UpsertBodyMetricDto) {
    if (!dto.recordedAt) {
      dto.recordedAt = new Date().toISOString().split('T')[0];
    }
    const metric: MetricCalculated = { ...dto };

    if (dto.weightKg) {
      const profile = await this.usersService.getHealthProfile(userId);
      if (profile?.heightCm) {
        metric.bmi = BMIUtil.calculate(dto.weightKg, profile.heightCm);

        if (profile.birthDate && profile.gender && profile.activityLevel) {
          const birth = new Date(profile.birthDate);
          const today = new Date();
          let age = today.getFullYear() - birth.getFullYear();
          if (
            today.getMonth() < birth.getMonth() ||
            (today.getMonth() === birth.getMonth() &&
              today.getDate() < birth.getDate())
          ) {
            age--;
          }
          metric.bmr = TDEEUtil.calculateBMR(dto.weightKg, profile.heightCm, age, profile.gender);
          metric.tdee = TDEEUtil.calculateTDEE(metric.bmr, profile.activityLevel);
        }
      }
    }

    return this.repository.upsert(userId, metric);
  }

  async getHistory(userId: string, query: BodyMetricQueryDto) {
    if (!query.date && !query.fromDate && !query.toDate) {
      query.date = new Date().toISOString().split('T')[0];
    }
    return this.repository.findHistory(userId, query);
  }

  async getLatest(userId: string) {
    return this.repository.findLatest(userId);
  }

  async getRange(userId: string, fromDate: string, toDate: string) {
    return this.repository.findRange(userId, fromDate, toDate);
  }

  async getByPeriod(userId: string, period: 'week' | 'month' | '3months' | '6months' | 'year') {
    const to = new Date().toISOString().split('T')[0];
    const from = new Date();
    switch (period) {
      case 'week':    from.setDate(from.getDate() - 7); break;
      case 'month':   from.setMonth(from.getMonth() - 1); break;
      case '3months': from.setMonth(from.getMonth() - 3); break;
      case '6months': from.setMonth(from.getMonth() - 6); break;
      case 'year':    from.setFullYear(from.getFullYear() - 1); break;
    }
    return this.getRange(userId, from.toISOString().split('T')[0], to);
  }

  async getProgressSummary(userId: string) {
    const history = await this.repository.findHistory(userId, { limit: 1000 });
    if (history.length === 0) {
      return { startWeight: 0, currentWeight: 0, weightChange: 0, startDate: null, latestDate: null, totalRecords: 0 };
    }

    const sorted = [...history].sort(
      (a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime(),
    );

    const first = sorted[0];
    const latest = sorted[sorted.length - 1];
    const startWeight = Number(first.weightKg) || 0;
    const currentWeight = Number(latest.weightKg) || 0;

    return {
      startWeight,
      currentWeight,
      weightChange: Number((currentWeight - startWeight).toFixed(1)),
      startDate: first.recordedAt,
      latestDate: latest.recordedAt,
      totalRecords: history.length,
    };
  }

  async getPhotosByUser(userId: string, limit = 10) {
    return this.repository.findPhotosByUser(userId, limit);
  }

  async uploadPhoto(userId: string, file: Express.Multer.File, photoType: string, bodyMetricId?: string) {
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'progress-photos');
    return this.repository.savePhoto({ userId, photoUrl: url, photoPublicId: publicId, photoType, bodyMetricId });
  }

  async deletePhoto(userId: string, photoId: string): Promise<void> {
    const photo = await this.repository.findPhotoById(photoId);
    if (!photo) throw new NotFoundException('Photo not found');
    if (photo.userId !== userId) throw new ForbiddenException('You can only delete your own photos');
    if (photo.photoPublicId) await this.cloudinaryService.deleteFile(photo.photoPublicId);
    await this.repository.deletePhoto(photoId);
  }
}
