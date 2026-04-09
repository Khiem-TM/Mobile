import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException } from '@nestjs/common';
import { MealLogsService } from './meal-logs.service';
import { Food } from '../foods/entities/food.entity';
import { MEAL_LOGS_REPOSITORY } from './meal-logs.constants';

const mockFood: Partial<Food> = {
  id: 'food-uuid',
  name: 'Apple',
  calories_per_100g: 52,
  protein_per_100g: 0.3,
  fat_per_100g: 0.2,
  carbs_per_100g: 14,
  fiber_per_100g: 2,
  sugar_per_100g: 10,
  sodium_per_100g: 0,
  serving_size_g: 150,
  serving_unit: 'serving',
};

const mockLog = {
  id: 'log-uuid',
  user_id: 'user-uuid',
  log_date: '2024-01-15',
  meal_type: 'lunch',
  items: [
    {
      id: 'item-uuid',
      meal_log_id: 'log-uuid',
      food_id: 'food-uuid',
      calories_snapshot: 78,
      protein_snapshot: 0.45,
      fat_snapshot: 0.3,
      carbs_snapshot: 21,
    },
  ],
};

const makeRepo = () => ({
  findOrCreate: jest.fn(),
  findById: jest.fn(),
  findByUser: jest.fn(),
  saveItem: jest.fn(),
  findItemById: jest.fn(),
  removeItem: jest.fn(),
  updateLog: jest.fn(),
  deleteLog: jest.fn(),
  updateItem: jest.fn(),
});

const makeFoodRepo = () => ({
  findOne: jest.fn(),
});

describe('MealLogsService', () => {
  let service: MealLogsService;
  let repo: ReturnType<typeof makeRepo>;
  let foodRepo: ReturnType<typeof makeFoodRepo>;

  beforeEach(async () => {
    repo = makeRepo();
    foodRepo = makeFoodRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        MealLogsService,
        { provide: MEAL_LOGS_REPOSITORY, useValue: repo },
        { provide: getRepositoryToken(Food), useValue: foodRepo },
      ],
    }).compile();

    service = module.get(MealLogsService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── create ──────────────────────────────────────────────────────────────

  describe('create', () => {
    it('should create a meal log', async () => {
      repo.findOrCreate.mockResolvedValue(mockLog);
      repo.findById.mockResolvedValue(mockLog);

      const result = await service.create('user-uuid', {
        log_date: '2024-01-15',
        meal_type: 'lunch',
        items: [],
      });
      expect(result.id).toBe('log-uuid');
    });

    it('should add items when provided', async () => {
      repo.findOrCreate.mockResolvedValue(mockLog);
      repo.findById.mockResolvedValue({ ...mockLog, user_id: 'user-uuid' });
      foodRepo.findOne.mockResolvedValue(mockFood);
      repo.saveItem.mockResolvedValue({});
      repo.findById.mockResolvedValue(mockLog);

      await service.create('user-uuid', {
        log_date: '2024-01-15',
        meal_type: 'lunch',
        items: [{ food_id: 'food-uuid', quantity: 1, serving_unit: 'serving' }],
      });
      expect(repo.saveItem).toHaveBeenCalled();
    });
  });

  // ─── findOne ─────────────────────────────────────────────────────────────

  describe('findOne', () => {
    it('should return log if user owns it', async () => {
      repo.findById.mockResolvedValue(mockLog);
      const result = await service.findOne('user-uuid', 'log-uuid');
      expect(result).toEqual(mockLog);
    });

    it('should throw if log not found', async () => {
      repo.findById.mockResolvedValue(null);
      await expect(service.findOne('user-uuid', 'bad-id')).rejects.toThrow(NotFoundException);
    });

    it('should throw if log belongs to another user', async () => {
      repo.findById.mockResolvedValue({ ...mockLog, user_id: 'other-uuid' });
      await expect(service.findOne('user-uuid', 'log-uuid')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── updateLog ───────────────────────────────────────────────────────────

  describe('updateLog', () => {
    it('should update log fields', async () => {
      repo.findById.mockResolvedValue(mockLog);
      const updated = { ...mockLog, notes: 'Updated note' };
      repo.updateLog.mockResolvedValue(updated);

      const result = await service.updateLog('user-uuid', 'log-uuid', { notes: 'Updated note' });
      expect(result.notes).toBe('Updated note');
    });

    it('should throw if log not found', async () => {
      repo.findById.mockResolvedValue(null);
      await expect(service.updateLog('user-uuid', 'bad-id', {})).rejects.toThrow(NotFoundException);
    });
  });

  // ─── deleteLog ───────────────────────────────────────────────────────────

  describe('deleteLog', () => {
    it('should delete log', async () => {
      repo.findById.mockResolvedValue(mockLog);
      repo.deleteLog.mockResolvedValue(undefined);
      await service.deleteLog('user-uuid', 'log-uuid');
      expect(repo.deleteLog).toHaveBeenCalledWith('log-uuid');
    });
  });

  // ─── addItemToLog ────────────────────────────────────────────────────────

  describe('addItemToLog', () => {
    it('should add item with correct calorie calculation (grams)', async () => {
      repo.findById.mockResolvedValue(mockLog);
      foodRepo.findOne.mockResolvedValue({ ...mockFood, serving_unit: 'g' });
      repo.saveItem.mockResolvedValue({
        calories_snapshot: 52 * (200 / 100),
      });

      const result = await service.addItemToLog('user-uuid', 'log-uuid', {
        food_id: 'food-uuid',
        quantity: 200,
        serving_unit: 'g',
      });
      expect(result.calories_snapshot).toBeCloseTo(104, 1);
    });

    it('should throw if food not found', async () => {
      repo.findById.mockResolvedValue(mockLog);
      foodRepo.findOne.mockResolvedValue(null);
      await expect(
        service.addItemToLog('user-uuid', 'log-uuid', {
          food_id: 'bad-id',
          quantity: 1,
          serving_unit: 'serving',
        }),
      ).rejects.toThrow(NotFoundException);
    });
  });

  // ─── removeItemFromLog ───────────────────────────────────────────────────

  describe('removeItemFromLog', () => {
    it('should remove item', async () => {
      repo.findItemById.mockResolvedValue({ id: 'item-uuid', meal_log_id: 'log-uuid' });
      repo.findById.mockResolvedValue(mockLog);
      repo.removeItem.mockResolvedValue(undefined);

      await service.removeItemFromLog('user-uuid', 'log-uuid', 'item-uuid');
      expect(repo.removeItem).toHaveBeenCalledWith('item-uuid');
    });

    it('should throw if item does not belong to this log', async () => {
      repo.findItemById.mockResolvedValue({ id: 'item-uuid', meal_log_id: 'other-log' });
      await expect(
        service.removeItemFromLog('user-uuid', 'log-uuid', 'item-uuid'),
      ).rejects.toThrow(NotFoundException);
    });
  });

  // ─── getDailySummary ─────────────────────────────────────────────────────

  describe('getDailySummary', () => {
    it('should sum all nutrients from logs', async () => {
      repo.findByUser.mockResolvedValue([mockLog]);
      const result = await service.getDailySummary('user-uuid', '2024-01-15');

      expect(result.date).toBe('2024-01-15');
      expect(result.total_calories).toBeCloseTo(78, 1);
      expect(result.total_protein).toBeCloseTo(0.45, 2);
      expect(result.logs).toHaveLength(1);
    });

    it('should return zero totals when no logs', async () => {
      repo.findByUser.mockResolvedValue([]);
      const result = await service.getDailySummary('user-uuid', '2024-01-15');
      expect(result.total_calories).toBe(0);
      expect(result.logs).toHaveLength(0);
    });
  });
});
