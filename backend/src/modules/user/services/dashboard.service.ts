import { Injectable, forwardRef, Inject } from '@nestjs/common';
import { MealLogsService } from '../../food/services/meal-logs.service';
import { ActivityLogsService } from '../../train/services/activity-logs.service';
import { BodyMetricsService } from '../../train/services/body-metrics.service';
import { StreaksService } from './streaks.service';
import { TrainingSessionsService } from '../../train/services/training-sessions.service';
import { RedisService } from '../../support/redis/redis.service';

const TTL = {
  DAILY: 120,
  WEEKLY: 600,
  MONTHLY: 1800,
};

@Injectable()
export class DashboardService {
  constructor(
    @Inject(forwardRef(() => MealLogsService))
    private readonly mealLogsService: MealLogsService,
    @Inject(forwardRef(() => ActivityLogsService))
    private readonly activityLogsService: ActivityLogsService,
    @Inject(forwardRef(() => BodyMetricsService))
    private readonly bodyMetricsService: BodyMetricsService,
    private readonly streaksService: StreaksService,
    @Inject(forwardRef(() => TrainingSessionsService))
    private readonly trainingSessionsService: TrainingSessionsService,
    private readonly redisService: RedisService,
  ) { }

  async getUserDailyDashboard(userId: string, date: string) {
    const key = `cache:dashboard:daily:${userId}:${date}`;
    const cached = await this.redisService.getJson<object>(key);
    if (cached) return cached;

    const [meals, activity, latestMetric, streaks, recentSessions] =
      await Promise.all([
        this.mealLogsService.getDailySummary(userId, date),
        this.activityLogsService.getByDate(userId, date),
        this.bodyMetricsService.getLatest(userId),
        this.streaksService.getStreaks(userId),
        this.trainingSessionsService.getSessionHistory(userId, 5),
      ]);

    const result = {
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
        water_ml: activity?.waterMl || 0,
        sleep_hours: activity?.sleepHours || null,
        mood: activity?.mood || null,
      },
      body: {
        current_weight: latestMetric?.weightKg || null,
        bmi: latestMetric?.bmi || null,
      },
      streaks,
      recent_sessions: recentSessions,
    };
    await this.redisService.setJson(key, result, TTL.DAILY);
    return result;
  }

  async invalidateDailyCache(userId: string, date: string): Promise<void> {
    const d = new Date(date);
    const day = d.getDay() || 7;
    const weekStart = new Date(d);
    weekStart.setDate(weekStart.getDate() - (day - 1));
    const weekStartStr = weekStart.toISOString().split('T')[0];
    const year = d.getFullYear();
    const month = d.getMonth() + 1;

    await Promise.all([
      this.redisService.del(`cache:dashboard:daily:${userId}:${date}`),
      this.redisService.del(`cache:dashboard:weekly:${userId}:${weekStartStr}`),
      this.redisService.del(`cache:dashboard:monthly:${userId}:${year}:${month}`),
    ]);
  }

  async getWeeklyReport(userId: string, weekStart: string) {
    const key = `cache:dashboard:weekly:${userId}:${weekStart}`;
    const cached = await this.redisService.getJson<object>(key);
    if (cached) return cached;

    const endDate = new Date(weekStart);
    endDate.setDate(endDate.getDate() + 6);
    const fromDate = weekStart;
    const toDate = endDate.toISOString().split('T')[0];

    const [activityRange, bodyRange, rawSessions] = await Promise.all([
      this.activityLogsService.getRange(userId, fromDate, toDate),
      this.bodyMetricsService.getRange(userId, fromDate, toDate),
      this.trainingSessionsService.getSessionHistoryRange(userId, fromDate, toDate),
    ]);

    const sessions = rawSessions.filter((s: any) => Number(s.totalDurationMinutes) > 0 || Number(s.totalCaloriesBurned) > 0);

    const dailyNutrition = await this.mealLogsService.getDailySummaryRange(userId, fromDate, toDate);

    const totalCalories = Object.values(dailyNutrition).reduce(
      (sum: number, day: any) => sum + (day.total_calories || 0),
      0,
    );
    const daysLogged = Object.values(dailyNutrition).filter(
      (d: any) => d.logs?.length > 0,
    ).length;

    const totalSteps = activityRange.reduce((sum, a) => sum + (Number(a.steps) || 0), 0);
    const totalWater = activityRange.reduce((sum, a) => sum + (Number(a.waterMl) || 0), 0);
    const totalTrainingMinutes = sessions.reduce(
      (sum, s: any) => sum + (Number(s.totalDurationMinutes) || 0),
      0,
    );

    // Calculate days passed in the week for accurate averaging
    const todayStr = new Date().toISOString().split('T')[0];
    const todayDate = new Date(todayStr);
    const fromDateObj = new Date(fromDate);
    let daysDivider = 7;
    if (todayStr >= fromDate && todayStr <= toDate) {
      const diffTime = todayDate.getTime() - fromDateObj.getTime();
      daysDivider = Math.floor(diffTime / (1000 * 60 * 60 * 24)) + 1;
    } else if (todayStr < fromDate) {
      daysDivider = 1;
    }

    const weekResult = {
      period: { from: fromDate, to: toDate },
      nutrition: {
        avg_daily_calories: Math.round(totalCalories / daysDivider),
        total_calories: Math.round(totalCalories),
        days_logged: daysLogged,
        daily_breakdown: dailyNutrition,
      },
      activity: {
        total_steps: totalSteps,
        avg_daily_steps: Math.round(totalSteps / daysDivider),
        total_water_ml: totalWater,
      },
      training: {
        session_count: sessions.length,
        total_duration_minutes: totalTrainingMinutes,
        sessions,
      },
      body_metrics: bodyRange,
    };
    await this.redisService.setJson(key, weekResult, TTL.WEEKLY);
    return weekResult;
  }

  async getMonthlyReport(userId: string, year: number, month: number) {
    const key = `cache:dashboard:monthly:${userId}:${year}:${month}`;
    const cached = await this.redisService.getJson<object>(key);
    if (cached) return cached;

    const fromDate = `${year}-${String(month).padStart(2, '0')}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const toDate = `${year}-${String(month).padStart(2, '0')}-${lastDay}`;

    const [activityRange, bodyRange, rawSessions] = await Promise.all([
      this.activityLogsService.getRange(userId, fromDate, toDate),
      this.bodyMetricsService.getRange(userId, fromDate, toDate),
      this.trainingSessionsService.getSessionHistoryRange(userId, fromDate, toDate),
    ]);

    const sessions = rawSessions.filter((s: any) => Number(s.totalDurationMinutes) > 0 || Number(s.totalCaloriesBurned) > 0);

    const dailyNutrition = await this.mealLogsService.getDailySummaryRange(userId, fromDate, toDate);
    const monthTotalCalories = Object.values(dailyNutrition).reduce(
      (sum: number, d: any) => sum + (d.total_calories || 0),
      0,
    );
    const daysLogged = Object.values(dailyNutrition).filter(
      (d: any) => d.logs?.length > 0,
    ).length;

    const totalSteps = activityRange.reduce((sum, a) => sum + (Number(a.steps) || 0), 0);
    const totalWater = activityRange.reduce((sum, a) => sum + (Number(a.waterMl) || 0), 0);

    const weights = bodyRange.filter((b) => b.weightKg).map((b) => Number(b.weightKg));
    const weightChange = weights.length >= 2 ? weights[weights.length - 1] - weights[0] : null;

    const totalCaloriesBurned = sessions.reduce(
      (sum, s: any) => sum + (Number(s.totalCaloriesBurned) || 0),
      0,
    );

    const monthResult = {
      period: { from: fromDate, to: toDate, year, month },
      nutrition: {
        total_calories: Math.round(monthTotalCalories),
        avg_daily_calories: Math.round(monthTotalCalories / lastDay),
        days_logged: daysLogged,
        daily_breakdown: dailyNutrition,
      },
      activity: {
        total_steps: totalSteps,
        avg_daily_steps: Math.round(totalSteps / lastDay),
        total_water_ml: totalWater,
        active_days: activityRange.length,
      },
      training: {
        session_count: sessions.length,
        total_calories_burned: Math.round(totalCaloriesBurned),
        avg_sessions_per_week: Math.round((sessions.length / lastDay) * 7 * 10) / 10,
      },
      body: {
        weight_change_kg: weightChange !== null ? Math.round(weightChange * 10) / 10 : null,
        measurements: bodyRange,
      },
    };
    await this.redisService.setJson(key, monthResult, TTL.MONTHLY);
    return monthResult;
  }
}
