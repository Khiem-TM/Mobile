import { Test, TestingModule } from '@nestjs/testing';
import { ActivityLogsService } from './activity-logs.service';
import { ACTIVITY_LOGS_REPOSITORY } from './activity-logs.constants';

const makeRepo = () => ({
  upsertSteps: jest.fn(),
  upsertCaloriesBurned: jest.fn(),
  upsertWater: jest.fn(),
  findByDate: jest.fn(),
  findRange: jest.fn(),
});

describe('ActivityLogsService', () => {
  let service: ActivityLogsService;
  let repo: ReturnType<typeof makeRepo>;

  beforeEach(async () => {
    repo = makeRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ActivityLogsService,
        { provide: ACTIVITY_LOGS_REPOSITORY, useValue: repo },
      ],
    }).compile();

    service = module.get(ActivityLogsService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── logSteps ────────────────────────────────────────────────────────────

  describe('logSteps', () => {
    it('should call repository upsertSteps', async () => {
      const log = { steps: 5000 };
      repo.upsertSteps.mockResolvedValue(log);

      const result = await service.logSteps('user-uuid', {
        logDate: '2024-01-15',
        steps: 5000,
      });
      expect(repo.upsertSteps).toHaveBeenCalledWith(
        'user-uuid',
        '2024-01-15',
        5000,
      );
      expect(result).toEqual(log);
    });
  });

  // ─── logCaloriesBurned ────────────────────────────────────────────────────

  describe('logCaloriesBurned', () => {
    it('should call repository upsertCaloriesBurned with all fields', async () => {
      repo.upsertCaloriesBurned.mockResolvedValue({
        caloriesBurned: 300,
        activeMinutes: 45,
      });

      const result = await service.logCaloriesBurned('user-uuid', {
        logDate: '2024-01-15',
        caloriesBurned: 300,
        activeMinutes: 45,
        exerciseNotes: 'running',
      });
      expect(repo.upsertCaloriesBurned).toHaveBeenCalledWith(
        'user-uuid',
        '2024-01-15',
        300,
        45,
        'running',
      );
      expect(result.caloriesBurned).toBe(300);
    });
  });

  // ─── logWater ────────────────────────────────────────────────────────────

  describe('logWater', () => {
    it('should call repository upsertWater', async () => {
      repo.upsertWater.mockResolvedValue({ waterMl: 2000 });

      const result = await service.logWater('user-uuid', {
        logDate: '2024-01-15',
        waterMl: 2000,
      });
      expect(repo.upsertWater).toHaveBeenCalledWith(
        'user-uuid',
        '2024-01-15',
        2000,
      );
      expect(result.waterMl).toBe(2000);
    });
  });

  // ─── getByDate ────────────────────────────────────────────────────────────

  describe('getByDate', () => {
    it('should return activity log for a date', async () => {
      const log = { userId: 'user-uuid', logDate: '2024-01-15', steps: 3000 };
      repo.findByDate.mockResolvedValue(log);

      const result = await service.getByDate('user-uuid', '2024-01-15');
      expect(result).toEqual(log);
    });

    it('should return default zero values when no log found', async () => {
      repo.findByDate.mockResolvedValue(null);
      const result = await service.getByDate('user-uuid', '2024-01-15');

      expect(result.steps).toBe(0);
      expect(result.caloriesBurned).toBe(0);
      expect(result.waterMl).toBe(0);
      expect(result.activeMinutes).toBe(0);
    });
  });

  // ─── getRange ────────────────────────────────────────────────────────────

  describe('getRange', () => {
    it('should return logs for date range', async () => {
      const logs = [
        { logDate: '2024-01-13', steps: 8000 },
        { logDate: '2024-01-14', steps: 10000 },
        { logDate: '2024-01-15', steps: 6000 },
      ];
      repo.findRange.mockResolvedValue(logs);

      const result = await service.getRange(
        'user-uuid',
        '2024-01-13',
        '2024-01-15',
      );
      expect(result).toHaveLength(3);
      expect(result[1].steps).toBe(10000);
    });

    it('should return empty array if no logs in range', async () => {
      repo.findRange.mockResolvedValue([]);
      const result = await service.getRange(
        'user-uuid',
        '2024-01-01',
        '2024-01-07',
      );
      expect(result).toHaveLength(0);
    });
  });
});
