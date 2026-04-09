import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Exercise } from './entities/exercise.entity';
import { WorkoutSession } from './entities/workout-session.entity';
import { TrainingGoal } from './entities/training-goal.entity';
import { TrainingService } from './training.service';
import { TrainingController } from './training.controller';
import {
  ExercisesRepository,
  WorkoutSessionsRepository,
  TrainingGoalsRepository,
} from './repositories/training.repository';
import {
  EXERCISES_REPOSITORY,
  WORKOUT_SESSIONS_REPOSITORY,
  TRAINING_GOALS_REPOSITORY,
} from './training.constants';
import { BodyMetricsModule } from '../body-metrics/body-metrics.module';
import { StreaksModule } from '../streaks/streaks.module';
import { UsersModule } from '../users/users.module';
import { LocalUploadModule } from '../local-upload/local-upload.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Exercise, WorkoutSession, TrainingGoal]),
    BodyMetricsModule,
    StreaksModule,
    UsersModule,
    LocalUploadModule,
  ],
  controllers: [TrainingController],
  providers: [
    TrainingService,
    {
      provide: EXERCISES_REPOSITORY,
      useClass: ExercisesRepository,
    },
    {
      provide: WORKOUT_SESSIONS_REPOSITORY,
      useClass: WorkoutSessionsRepository,
    },
    {
      provide: TRAINING_GOALS_REPOSITORY,
      useClass: TrainingGoalsRepository,
    },
  ],
  exports: [TrainingService],
})
export class TrainingModule {}
