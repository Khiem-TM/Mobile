import { Test, TestingModule } from '@nestjs/testing';
import { DashboardService } from './dashboard.service';
import { MealLogsService } from '../meal-logs/meal-logs.service';
import { ActivityLogsService } from '../activity-logs/activity-logs.service';
import { BodyMetricsService } from '../body-metrics/services/body-metrics.service';
import { StreaksService } from '../streaks/streaks.service';
import { TrainingService } from '../training/training.service';

const USER_ID = 'user-uuid';
const DATE = '2024-01-15';
const WEEK_START = '2024-01-15';

describe('DashboardService', () => {
  let service: DashboardService;
  let mealLogsService: jest.Mocked<Partial<MealLogsService>>;
  let activityLogsService: jest.Mocked<Partial<ActivityLogsService>>;
  let bodyMetricsService: jest.Mocked<Partial<BodyMetricsService>>;
  let streaksService: jest.Mocked<Partial<StreaksService>>;
  let trainingService: jest.Mocked<Partial<TrainingService>>;

  beforeEach(async () => {
    mealLogsService = { getDailySummary: jest.fn() };
    activityLogsService = { getByDate: jest.fn(), getRange: jest.fn() };
    bodyMetricsService = { getLatest: jest.fn(), getRange: jest.fn() };
    streaksService = { getStreaks: jest.fn() };
    trainingService = { getWorkoutHistory: jest.fn() };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DashboardService,
        { provide: MealLogsService, useValue: mealLogsService },
        { provide: ActivityLogsService, useValue: activityLogsService },
        { provide: BodyMetricsService, useValue: bodyMetricsService },
        { provide: StreaksService, useValue: streaksService },
        { provide: TrainingService, useValue: trainingService },
      ],
    }).compile();

    service = module.get(DashboardService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getUserDailyDashboard ────────────────────────────────────────────────

  describe('getUserDailyDashboard', () => {
    it('should aggregate all daily data', async () => {
      (mealLogsService.getDailySummary as jest.Mock).mockResolvedValue({
        total_calories: 1800,
        total_protein: 120,
        total_fat: 60,
        total_carbs: 200,
        logs: [],
      });
      (activityLogsService.getByDate as jest.Mock).mockResolvedValue({
        steps: 8000,
        caloriesBurned: 400,
        activeMinutes: 45,
        waterMl: 2500,
      });
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue({
        weightKg: 70,
        bmi: 22.5,
      });
      (streaksService.getStreaks as jest.Mock).mockResolvedValue([
        { streak_type: 'workout', current_streak: 5 },
      ]);
      (trainingService.getWorkoutHistory as jest.Mock).mockResolvedValue([
        { id: 's1', sessionDate: DATE },
      ]);

      const result = await service.getUserDailyDashboard(USER_ID, DATE);

      expect(result.date).toBe(DATE);
      expect(result.nutrition.total_calories).toBe(1800);
      expect(result.activity.steps).toBe(8000);
      expect(result.activity.water_ml).toBe(2500);
      expect(result.body.current_weight).toBe(70);
      expect(result.streaks).toHaveLength(1);
      expect(result.recent_workouts).toHaveLength(1);
    });

    it('should return zeros when no activity log found', async () => {
      (mealLogsService.getDailySummary as jest.Mock).mockResolvedValue({
        total_calories: 0,
        total_protein: 0,
        total_fat: 0,
        total_carbs: 0,
        logs: [],
      });
      (activityLogsService.getByDate as jest.Mock).mockResolvedValue(null);
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue(null);
      (streaksService.getStreaks as jest.Mock).mockResolvedValue([]);
      (trainingService.getWorkoutHistory as jest.Mock).mockResolvedValue([]);

      const result = await service.getUserDailyDashboard(USER_ID, DATE);
      expect(result.activity.steps).toBe(0);
      expect(result.body.current_weight).toBeNull();
    });
  });

  // ─── getWeeklyReport ──────────────────────────────────────────────────────

  describe('getWeeklyReport', () => {
    it('should aggregate 7-day data', async () => {
      (mealLogsService.getDailySummary as jest.Mock).mockResolvedValue({
        total_calories: 1800,
        total_protein: 100,
        total_fat: 60,
        total_carbs: 200,
        logs: [{}],
      });
      (activityLogsService.getRange as jest.Mock).mockResolvedValue([
        { steps: 8000, waterMl: 2000 },
        { steps: 10000, waterMl: 2500 },
      ]);
      (bodyMetricsService.getRange as jest.Mock).mockResolvedValue([]);
      (trainingService.getWorkoutHistory as jest.Mock).mockResolvedValue([
        { sessionDate: '2024-01-16' },
        { sessionDate: '2024-01-18' },
      ]);

      const result = await service.getWeeklyReport(USER_ID, WEEK_START);

      expect(result.period.from).toBe('2024-01-15');
      expect(result.period.to).toBe('2024-01-21');
      expect(result.nutrition.days_logged).toBe(7);
      expect(result.activity.total_steps).toBe(18000);
      expect(result.training.workout_count).toBe(2);
    });
  });

  // ─── getMonthlyReport ─────────────────────────────────────────────────────

  describe('getMonthlyReport', () => {
    it('should aggregate monthly data and calculate weight change', async () => {
      (activityLogsService.getRange as jest.Mock).mockResolvedValue([
        { steps: 8000, waterMl: 2000, activeMinutes: 30 },
        { steps: 9000, waterMl: 2500, activeMinutes: 45 },
      ]);
      (bodyMetricsService.getRange as jest.Mock).mockResolvedValue([
        { weightKg: 72 },
        { weightKg: 70.5 },
      ]);
      (trainingService.getWorkoutHistory as jest.Mock).mockResolvedValue([
        { sessionDate: '2024-01-10' },
        { sessionDate: '2024-01-15' },
        { sessionDate: '2024-01-20' },
        { sessionDate: '2024-01-25' },
      ]);

      const result = await service.getMonthlyReport(USER_ID, 2024, 1);

      expect(result.period.year).toBe(2024);
      expect(result.period.month).toBe(1);
      expect(result.activity.total_steps).toBe(17000);
      expect(result.body.weight_change_kg).toBeCloseTo(-1.5, 1);
      expect(result.training.workout_count).toBe(4);
    });

    it('should handle no weight measurements', async () => {
      (activityLogsService.getRange as jest.Mock).mockResolvedValue([]);
      (bodyMetricsService.getRange as jest.Mock).mockResolvedValue([]);
      (trainingService.getWorkoutHistory as jest.Mock).mockResolvedValue([]);

      const result = await service.getMonthlyReport(USER_ID, 2024, 1);
      expect(result.body.weight_change_kg).toBeNull();
    });
  });
});
