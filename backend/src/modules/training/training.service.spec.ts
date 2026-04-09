import { Test, TestingModule } from '@nestjs/testing';
import { NotFoundException } from '@nestjs/common';
import { TrainingService } from './training.service';
import {
  EXERCISES_REPOSITORY,
  WORKOUT_SESSIONS_REPOSITORY,
  TRAINING_GOALS_REPOSITORY,
} from './training.constants';
import { BodyMetricsService } from '../body-metrics/services/body-metrics.service';
import { StreaksService } from '../streaks/streaks.service';
import { UsersService } from '../users/users.service';
import { TrainingGoalType } from '../../common/enums/training-goal-type.enum';
import { StreakType } from '../../common/enums/streak-type.enum';

const makeExerciseRepo = () => ({
  findAll: jest.fn(),
  findById: jest.fn(),
});
const makeSessionRepo = () => ({
  save: jest.fn(),
  findByUser: jest.fn(),
  findById: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
});
const makeGoalRepo = () => ({
  save: jest.fn(),
  findByUser: jest.fn(),
  findById: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
});

describe('TrainingService', () => {
  let service: TrainingService;
  let exerciseRepo: ReturnType<typeof makeExerciseRepo>;
  let sessionRepo: ReturnType<typeof makeSessionRepo>;
  let goalRepo: ReturnType<typeof makeGoalRepo>;
  let bodyMetricsService: jest.Mocked<Partial<BodyMetricsService>>;
  let streaksService: jest.Mocked<Partial<StreaksService>>;
  let usersService: jest.Mocked<Partial<UsersService>>;

  beforeEach(async () => {
    exerciseRepo = makeExerciseRepo();
    sessionRepo = makeSessionRepo();
    goalRepo = makeGoalRepo();
    bodyMetricsService = { getLatest: jest.fn() };
    streaksService = { updateActivity: jest.fn() };
    usersService = { getHealthProfile: jest.fn() };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TrainingService,
        { provide: EXERCISES_REPOSITORY, useValue: exerciseRepo },
        { provide: WORKOUT_SESSIONS_REPOSITORY, useValue: sessionRepo },
        { provide: TRAINING_GOALS_REPOSITORY, useValue: goalRepo },
        { provide: BodyMetricsService, useValue: bodyMetricsService },
        { provide: StreaksService, useValue: streaksService },
        { provide: UsersService, useValue: usersService },
      ],
    }).compile();

    service = module.get(TrainingService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getExercises ─────────────────────────────────────────────────────────

  describe('getExercises', () => {
    it('should return exercises from repo', async () => {
      const exercises = [{ id: 'e1', name: 'Push Up', metValue: 3.8 }];
      exerciseRepo.findAll.mockResolvedValue(exercises);
      const result = await service.getExercises({} as any);
      expect(result).toEqual(exercises);
    });
  });

  // ─── logWorkout ───────────────────────────────────────────────────────────

  describe('logWorkout', () => {
    const dto = {
      exerciseId: 'e1',
      sessionDate: '2024-01-15',
      durationMinutes: 60,
      weightKg: 0,
      sets: 3,
      repsPerSet: 10,
      notes: 'felt good',
    };

    it('should log workout and update streak', async () => {
      exerciseRepo.findById.mockResolvedValue({ id: 'e1', metValue: 3.8 });
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue({ weightKg: 70, tdee: 2000 });
      sessionRepo.save.mockResolvedValue({ id: 's1', ...dto, caloriesBurnedSnapshot: 266 });
      (streaksService.updateActivity as jest.Mock).mockResolvedValue({});

      const result = await service.logWorkout('user-uuid', dto);
      expect(result.caloriesBurnedSnapshot).toBeCloseTo(3.8 * 70 * 1, 0);
      expect(streaksService.updateActivity).toHaveBeenCalledWith(
        'user-uuid', StreakType.WORKOUT, dto.sessionDate,
      );
    });

    it('should use fallback weight 70kg when no body metrics', async () => {
      exerciseRepo.findById.mockResolvedValue({ id: 'e1', metValue: 5 });
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue(null);
      (usersService.getHealthProfile as jest.Mock).mockResolvedValue(null);
      sessionRepo.save.mockImplementation((data) => Promise.resolve(data));
      (streaksService.updateActivity as jest.Mock).mockResolvedValue({});

      const result = await service.logWorkout('user-uuid', dto);
      // calories = 5 * 70 * (60/60) = 350
      expect(result.caloriesBurnedSnapshot).toBeCloseTo(350, 0);
    });

    it('should throw if exercise not found', async () => {
      exerciseRepo.findById.mockResolvedValue(null);
      await expect(service.logWorkout('user-uuid', dto)).rejects.toThrow(NotFoundException);
    });
  });

  // ─── getWorkoutHistory ────────────────────────────────────────────────────

  describe('getWorkoutHistory', () => {
    it('should return session history', async () => {
      sessionRepo.findByUser.mockResolvedValue([{ id: 's1' }, { id: 's2' }]);
      const result = await service.getWorkoutHistory('user-uuid', 10);
      expect(result).toHaveLength(2);
    });
  });

  // ─── createGoal ───────────────────────────────────────────────────────────

  describe('createGoal', () => {
    it('should create goal with macro calculation when TDEE available', async () => {
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue({ tdee: 2200 });
      goalRepo.save.mockImplementation((data) => Promise.resolve({ id: 'g1', ...data }));

      const result = await service.createGoal('user-uuid', {
        goalType: TrainingGoalType.LOSE_WEIGHT,
        targetValue: 65,
        deadline: '2024-06-01',
        workoutsPerWeek: 3,
      } as any);

      // calories = 2200 - 500 = 1700
      expect(result.dailyCaloriesGoal).toBeCloseTo(1700, 0);
    });

    it('should create goal with zero macros when no TDEE', async () => {
      (bodyMetricsService.getLatest as jest.Mock).mockResolvedValue(null);
      goalRepo.save.mockImplementation((data) => Promise.resolve({ id: 'g1', ...data }));

      const result = await service.createGoal('user-uuid', {
        goalType: TrainingGoalType.MAINTAIN,
        targetValue: 70,
      } as any);

      expect(result.dailyCaloriesGoal).toBeUndefined();
    });
  });

  // ─── updateGoal ───────────────────────────────────────────────────────────

  describe('updateGoal', () => {
    it('should update goal', async () => {
      goalRepo.findById.mockResolvedValue({ id: 'g1', userId: 'user-uuid' });
      goalRepo.update.mockResolvedValue({ id: 'g1', targetValue: 68 });

      const result = await service.updateGoal('user-uuid', 'g1', { targetValue: 68 } as any);
      expect(result.targetValue).toBe(68);
    });

    it('should throw if goal not found or not owned', async () => {
      goalRepo.findById.mockResolvedValue(null);
      await expect(
        service.updateGoal('user-uuid', 'bad-id', {} as any),
      ).rejects.toThrow(NotFoundException);
    });
  });

  // ─── deleteGoal ───────────────────────────────────────────────────────────

  describe('deleteGoal', () => {
    it('should delete goal if owned by user', async () => {
      goalRepo.findById.mockResolvedValue({ id: 'g1', userId: 'user-uuid' });
      goalRepo.delete.mockResolvedValue(undefined);

      await service.deleteGoal('user-uuid', 'g1');
      expect(goalRepo.delete).toHaveBeenCalledWith('g1');
    });

    it('should throw if goal belongs to another user', async () => {
      goalRepo.findById.mockResolvedValue({ id: 'g1', userId: 'other-uuid' });
      await expect(service.deleteGoal('user-uuid', 'g1')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── deleteWorkout ────────────────────────────────────────────────────────

  describe('deleteWorkout', () => {
    it('should delete workout session if owned', async () => {
      sessionRepo.findById.mockResolvedValue({ id: 's1', userId: 'user-uuid' });
      sessionRepo.delete.mockResolvedValue(undefined);
      await service.deleteWorkout('user-uuid', 's1');
      expect(sessionRepo.delete).toHaveBeenCalledWith('s1');
    });

    it('should throw if session not owned', async () => {
      sessionRepo.findById.mockResolvedValue({ id: 's1', userId: 'other-uuid' });
      await expect(service.deleteWorkout('user-uuid', 's1')).rejects.toThrow(NotFoundException);
    });
  });
});
