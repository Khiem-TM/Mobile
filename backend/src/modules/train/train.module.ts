import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Exercise } from './entities/exercise.entity';
import { TrainingSession } from './entities/training-session.entity';
import { TrainingSessionItem } from './entities/training-session-item.entity';
import { FavoriteExercise } from './entities/favorite-exercise.entity';
import { BodyMetric } from './entities/body-metric.entity';
import { BodyProgressPhoto } from './entities/body-progress-photo.entity';
import { ActivityLog } from './entities/activity-log.entity';
import { SupportModule } from '../support/support.module';
import { UserModule } from '../user/user.module';

import { ExercisesController } from './controllers/exercises.controller';
import { TrainingSessionsController } from './controllers/training-sessions.controller';
import { FavoriteExercisesController } from './controllers/favorite-exercises.controller';
import { BodyMetricsController } from './controllers/body-metrics.controller';
import { ActivityLogsController } from './controllers/activity-logs.controller';

import { ExercisesService } from './services/exercises.service';
import { TrainingSessionsService } from './services/training-sessions.service';
import { FavoriteExercisesService } from './services/favorite-exercises.service';
import { CaloriesCalculationService } from './services/calories-calculation.service';
import { BodyMetricsService } from './services/body-metrics.service';
import { ActivityLogsService } from './services/activity-logs.service';

import { ExercisesRepository } from './repositories/exercises.repository';
import { TrainingSessionsRepository } from './repositories/training-sessions.repository';
import { BodyMetricsRepository } from './repositories/body-metrics.repository';
import { BodyProgressPhotosRepository } from './repositories/body-progress-photos.repository';
import { ActivityLogsRepository } from './repositories/activity-logs.repository';

import {
  EXERCISES_REPOSITORY,
  TRAINING_SESSIONS_REPOSITORY,
  BODY_METRICS_REPOSITORY,
  BODY_PHOTOS_REPOSITORY,
  ACTIVITY_LOGS_REPOSITORY,
} from './train.constants';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Exercise,
      TrainingSession,
      TrainingSessionItem,
      FavoriteExercise,
      BodyMetric,
      BodyProgressPhoto,
      ActivityLog,
    ]),
    SupportModule,
    forwardRef(() => UserModule),
  ],
  controllers: [
    ExercisesController,
    TrainingSessionsController,
    FavoriteExercisesController,
    BodyMetricsController,
    ActivityLogsController,
  ],
  providers: [
    ExercisesService,
    TrainingSessionsService,
    FavoriteExercisesService,
    CaloriesCalculationService,
    BodyMetricsService,
    ActivityLogsService,
    { provide: EXERCISES_REPOSITORY, useClass: ExercisesRepository },
    { provide: TRAINING_SESSIONS_REPOSITORY, useClass: TrainingSessionsRepository },
    { provide: BODY_METRICS_REPOSITORY, useClass: BodyMetricsRepository },
    { provide: BODY_PHOTOS_REPOSITORY, useClass: BodyProgressPhotosRepository },
    { provide: ACTIVITY_LOGS_REPOSITORY, useClass: ActivityLogsRepository },
  ],
  exports: [
    ExercisesService,
    TrainingSessionsService,
    FavoriteExercisesService,
    BodyMetricsService,
    ActivityLogsService,
  ],
})
export class TrainModule {}
