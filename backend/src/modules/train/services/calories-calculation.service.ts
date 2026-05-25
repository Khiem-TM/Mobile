import { Injectable, Inject, forwardRef } from '@nestjs/common';
import { Exercise } from '../entities/exercise.entity';
import { ExerciseType } from '../enums/exercise-type.enum';
import { IntensityLevel } from '../enums/intensity-level.enum';
import { BodyMetricsService } from './body-metrics.service';
import { UsersService } from '../../user/services/users.service';

const INTENSITY_CALORIES_PER_MINUTE: Record<IntensityLevel, number> = {
  [IntensityLevel.LOW]: 4,
  [IntensityLevel.MEDIUM]: 7,
  [IntensityLevel.HIGH]: 10,
};

const GYM_INTENSITY_FACTOR: Record<IntensityLevel, number> = {
  [IntensityLevel.LOW]: 3.5,
  [IntensityLevel.MEDIUM]: 5.0,
  [IntensityLevel.HIGH]: 6.5,
};

@Injectable()
export class CaloriesCalculationService {
  constructor(
    @Inject(forwardRef(() => BodyMetricsService))
    private readonly bodyMetricsService: BodyMetricsService,
    @Inject(forwardRef(() => UsersService))
    private readonly usersService: UsersService,
  ) {}

  async getUserWeight(userId: string): Promise<number> {
    const latestMetric = await this.bodyMetricsService.getLatest(userId);
    if (latestMetric?.weightKg) return Number(latestMetric.weightKg);

    const profile = await this.usersService.getHealthProfile(userId);
    if (profile?.initialWeightKg) return Number(profile.initialWeightKg);

    return 70;
  }

  calculateForExercise(
    exercise: Exercise,
    durationMinutes: number,
    options: {
      intensityLevel?: IntensityLevel;
      weightKg?: number;
      userWeightKg?: number;
    } = {},
  ): number {
    const userWeight = options.userWeightKg ?? 70;
    const metValue = Number(exercise.metValue ?? 0);

    // Primary: MET-based formula if metValue is set
    if (metValue > 0) {
      return Number((metValue * userWeight * (durationMinutes / 60)).toFixed(2));
    }

    if (exercise.exerciseType === ExerciseType.SPORT) {
      // Use estimatedCaloriesPerMinute if available
      if (exercise.estimatedCaloriesPerMinute) {
        return Number((Number(exercise.estimatedCaloriesPerMinute) * durationMinutes).toFixed(2));
      }
      const intensity = options.intensityLevel ?? IntensityLevel.MEDIUM;
      return Number((INTENSITY_CALORIES_PER_MINUTE[intensity] * durationMinutes).toFixed(2));
    }

    // GYM: use intensity factor * weight * duration
    const intensity = options.intensityLevel ?? IntensityLevel.MEDIUM;
    const factor = GYM_INTENSITY_FACTOR[intensity];
    return Number(((factor * userWeight * durationMinutes) / 60).toFixed(2));
  }
}
