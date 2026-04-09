import { Injectable } from '@nestjs/common';
import { MealLogsService } from '../meal-logs/meal-logs.service';
import { ActivityLogsService } from '../activity-logs/activity-logs.service';
import { BodyMetricsService } from '../body-metrics/services/body-metrics.service';
import { StreaksService } from '../streaks/streaks.service';
import { TrainingService } from '../training/training.service';

@Injectable()
export class DashboardService {
  constructor(
    private readonly mealLogsService: MealLogsService,
    private readonly activityLogsService: ActivityLogsService,
    private readonly bodyMetricsService: BodyMetricsService,
    private readonly streaksService: StreaksService,
    private readonly trainingService: TrainingService,
  ) {}

  async getUserDailyDashboard(userId: string, date: string) {
    const [meals, activity, latestMetric, streaks, recentWorkouts] =
      await Promise.all([
        this.mealLogsService.getDailySummary(userId, date),
        this.activityLogsService.getByDate(userId, date),
        this.bodyMetricsService.getLatest(userId),
        this.streaksService.getStreaks(userId),
        this.trainingService.getWorkoutHistory(userId, 5),
      ]);

    return {
      date,
      nutrition: {
        total_calories: meals.total_calories,
        total_protein: meals.total_protein,
        total_fat: meals.total_fat,
        total_carbs: meals.total_carbs,
        total_fiber: meals.total_fiber ?? 0,
        meal_logs: meals.logs,
      },
      activity: {
        steps: activity?.steps || 0,
        calories_burned: activity?.caloriesBurned || 0,
        active_minutes: activity?.activeMinutes || 0,
        water_ml: activity?.waterMl || 0,
      },
      body: {
        current_weight: latestMetric?.weightKg || null,
        bmi: latestMetric?.bmi || null,
      },
      streaks,
      recent_workouts: recentWorkouts,
    };
  }

  async getWeeklyReport(userId: string, weekStart: string) {
    const endDate = new Date(weekStart);
    endDate.setDate(endDate.getDate() + 6);

    const fromDate = weekStart;
    const toDate = endDate.toISOString().split('T')[0];

    const [activityRange, bodyRange, workouts] = await Promise.all([
      this.activityLogsService.getRange(userId, fromDate, toDate),
      this.bodyMetricsService.getRange(userId, fromDate, toDate),
      this.trainingService.getWorkoutHistory(userId, 50),
    ]);

    const dailyNutrition: Record<string, any> = {};
    for (let i = 0; i < 7; i++) {
      const d = new Date(weekStart);
      d.setDate(d.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];
      dailyNutrition[dateStr] = await this.mealLogsService.getDailySummary(
        userId,
        dateStr,
      );
    }

    const totalCalories = Object.values(dailyNutrition).reduce(
      (sum: number, day: any) => sum + (day.total_calories || 0),
      0,
    );
    const daysLogged = Object.values(dailyNutrition).filter(
      (d: any) => d.logs?.length > 0,
    ).length;

    const totalSteps = activityRange.reduce(
      (sum, a) => sum + (Number(a.steps) || 0),
      0,
    );
    const totalWater = activityRange.reduce(
      (sum, a) => sum + (Number(a.waterMl) || 0),
      0,
    );

    const weekWorkouts = workouts.filter(
      (w) => w.sessionDate >= fromDate && w.sessionDate <= toDate,
    );

    return {
      period: { from: fromDate, to: toDate },
      nutrition: {
        avg_daily_calories:
          daysLogged > 0 ? Math.round(totalCalories / daysLogged) : 0,
        total_calories: Math.round(totalCalories),
        days_logged: daysLogged,
        daily_breakdown: dailyNutrition,
      },
      activity: {
        total_steps: totalSteps,
        avg_daily_steps: activityRange.length
          ? Math.round(totalSteps / activityRange.length)
          : 0,
        total_water_ml: totalWater,
      },
      training: {
        workout_count: weekWorkouts.length,
        workouts: weekWorkouts,
      },
      body_metrics: bodyRange,
    };
  }

  async getMonthlyReport(userId: string, year: number, month: number) {
    const fromDate = `${year}-${String(month).padStart(2, '0')}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const toDate = `${year}-${String(month).padStart(2, '0')}-${lastDay}`;

    const [activityRange, bodyRange, workouts] = await Promise.all([
      this.activityLogsService.getRange(userId, fromDate, toDate),
      this.bodyMetricsService.getRange(userId, fromDate, toDate),
      this.trainingService.getWorkoutHistory(userId, 200),
    ]);

    const monthWorkouts = workouts.filter(
      (w) => w.sessionDate >= fromDate && w.sessionDate <= toDate,
    );

    const totalSteps = activityRange.reduce(
      (sum, a) => sum + (Number(a.steps) || 0),
      0,
    );
    const totalWater = activityRange.reduce(
      (sum, a) => sum + (Number(a.waterMl) || 0),
      0,
    );
    const totalActiveMinutes = activityRange.reduce(
      (sum, a) => sum + (Number(a.activeMinutes) || 0),
      0,
    );

    const weights = bodyRange
      .filter((b) => b.weightKg)
      .map((b) => Number(b.weightKg));
    const weightChange =
      weights.length >= 2 ? weights[weights.length - 1] - weights[0] : null;

    return {
      period: { from: fromDate, to: toDate, year, month },
      activity: {
        total_steps: totalSteps,
        avg_daily_steps: activityRange.length
          ? Math.round(totalSteps / activityRange.length)
          : 0,
        total_water_ml: totalWater,
        total_active_minutes: totalActiveMinutes,
        active_days: activityRange.length,
      },
      training: {
        workout_count: monthWorkouts.length,
        avg_workouts_per_week:
          Math.round((monthWorkouts.length / lastDay) * 7 * 10) / 10,
      },
      body: {
        weight_change_kg:
          weightChange !== null ? Math.round(weightChange * 10) / 10 : null,
        measurements: bodyRange,
      },
    };
  }
}
