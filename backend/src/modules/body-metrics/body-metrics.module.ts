import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { BodyMetric } from './entities/body-metric.entity';
import { BodyProgressPhoto } from './entities/body-progress-photo.entity';
import { BodyMetricsService } from './services/body-metrics.service';
import { BodyMetricsController } from './controllers/body-metrics.controller';
import { BodyMetricsRepository } from './repositories/body-metrics.repository';
import {
  BODY_METRICS_REPOSITORY,
  BODY_PHOTOS_REPOSITORY,
} from './body-metrics.constants';
import { UsersModule } from '../users/users.module';
import { LocalUploadModule } from '../local-upload/local-upload.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([BodyMetric, BodyProgressPhoto]),
    UsersModule,
    LocalUploadModule,
  ],
  controllers: [BodyMetricsController],
  providers: [
    BodyMetricsService,
    {
      provide: BODY_METRICS_REPOSITORY,
      useClass: BodyMetricsRepository,
    },
    // We can use the same class if it handles both, or keep it flexible if separated later.
    // Given the repository implementation handles both repos, we'll use it for both for now if needed,
    // though usually one provides the specific repo.
    {
      provide: BODY_PHOTOS_REPOSITORY,
      useClass: BodyMetricsRepository,
    },
  ],
  exports: [BodyMetricsService],
})
export class BodyMetricsModule {}
