import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Exercise } from './entities/exercise.entity';
import { WorkoutSession } from './entities/workout-session.entity';
import { WorkoutSessionDetail } from './entities/workout-session-detail.entity';
import { ExerciseUserFavorite } from './entities/exercise-user-favorite.entity';
import { BodyMetric } from './entities/body-metric.entity';
import { BodyProgressPhoto } from './entities/body-progress-photo.entity';
import { ActivityLog } from './entities/activity-log.entity';
import { SupportModule } from '../support/support.module';
import { UserModule } from '../user/user.module';
import { TrainingController } from './controllers/training.controller';
import { BodyMetricsController } from './controllers/body-metrics.controller';
import { ActivityLogsController } from './controllers/activity-logs.controller';
import { TrainingService } from './services/training.service';
import { BodyMetricsService } from './services/body-metrics.service';
import { ActivityLogsService } from './services/activity-logs.service';
import {
  ExercisesRepository,
  WorkoutSessionsRepository,
} from './repositories/training.repository';
import { BodyMetricsRepository } from './repositories/body-metrics.repository';
import { ActivityLogsRepository } from './repositories/activity-logs.repository';
import {
  TRAINING_EXERCISES_REPOSITORY,
  WORKOUT_SESSIONS_REPOSITORY,
  BODY_METRICS_REPOSITORY,
  BODY_PHOTOS_REPOSITORY,
  ACTIVITY_LOGS_REPOSITORY,
} from './train.constants';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Exercise,
      WorkoutSession,
      WorkoutSessionDetail,
      ExerciseUserFavorite,
      BodyMetric,
      BodyProgressPhoto,
      ActivityLog,
    ]),
    SupportModule,
    forwardRef(() => UserModule),
  ],
  controllers: [TrainingController, BodyMetricsController, ActivityLogsController],
  providers: [
    TrainingService,
    BodyMetricsService,
    ActivityLogsService,
    { provide: TRAINING_EXERCISES_REPOSITORY, useClass: ExercisesRepository },
    { provide: WORKOUT_SESSIONS_REPOSITORY, useClass: WorkoutSessionsRepository },
    { provide: BODY_METRICS_REPOSITORY, useClass: BodyMetricsRepository },
    { provide: BODY_PHOTOS_REPOSITORY, useClass: BodyMetricsRepository },
    { provide: ACTIVITY_LOGS_REPOSITORY, useClass: ActivityLogsRepository },
  ],
  exports: [TrainingService, BodyMetricsService, ActivityLogsService],
})
export class TrainModule {}
