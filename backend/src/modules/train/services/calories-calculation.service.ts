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

const CYCLING_PATTERN = /cycl|bike|bicycle|đạp xe/i;

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

  // MET lookup by speed — cycling vs running
  private getCardioMet(avgSpeedKmh: number, isCycling: boolean): number {
    if (isCycling) {
      if (avgSpeedKmh < 16) return 4.0;
      if (avgSpeedKmh < 22) return 8.0;
      if (avgSpeedKmh < 26) return 10.0;
      return 12.0;
    }
    // Running / walking
    if (avgSpeedKmh < 6) return 3.5;
    if (avgSpeedKmh < 8) return 6.0;
    if (avgSpeedKmh < 10) return 8.3;
    if (avgSpeedKmh < 12) return 9.8;
    if (avgSpeedKmh < 14) return 11.0;
    return 12.8;
  }

  // CARDIO: derive duration from distance + speed, then compute calories
  calculateCardio(
    exercise: Exercise,
    distanceKm: number,
    avgSpeedKmh: number,
    userWeightKg: number,
  ): { calories: number; durationMinutes: number } {
    const durationHours = distanceKm / avgSpeedKmh;
    const durationMinutes = Number((durationHours * 60).toFixed(1));

    const metValue = Number(exercise.metValue ?? 0);
    let calories: number;

    if (metValue > 0) {
      calories = Number((metValue * userWeightKg * durationHours).toFixed(2));
    } else {
      const isCycling = CYCLING_PATTERN.test(
        `${exercise.movementType ?? ''} ${exercise.category ?? ''} ${exercise.name}`,
      );
      const met = this.getCardioMet(avgSpeedKmh, isCycling);
      calories = Number((met * userWeightKg * durationHours).toFixed(2));
    }

    return { calories, durationMinutes };
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

    if (metValue > 0) {
      return Number((metValue * userWeight * (durationMinutes / 60)).toFixed(2));
    }

    if (exercise.exerciseType === ExerciseType.SPORT) {
      if (exercise.estimatedCaloriesPerMinute) {
        return Number((Number(exercise.estimatedCaloriesPerMinute) * durationMinutes).toFixed(2));
      }
      const intensity = options.intensityLevel ?? IntensityLevel.MEDIUM;
      return Number((INTENSITY_CALORIES_PER_MINUTE[intensity] * durationMinutes).toFixed(2));
    }

    // GYM
    const intensity = options.intensityLevel ?? IntensityLevel.MEDIUM;
    const factor = GYM_INTENSITY_FACTOR[intensity];
    return Number(((factor * userWeight * durationMinutes) / 60).toFixed(2));
  }
}
