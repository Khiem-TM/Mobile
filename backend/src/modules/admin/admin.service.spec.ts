import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException } from '@nestjs/common';
import { AdminService } from './admin.service';
import { User } from '../users/entities/user.entity';
import { Food } from '../foods/entities/food.entity';

const mockUser = {
  id: 'user-uuid',
  email: 'test@example.com',
  display_name: 'Test User',
  is_active: true,
};

const mockFood: Partial<Food> = {
  id: 'food-uuid',
  name: 'Apple',
  is_verified: false,
  is_active: true,
};

const makeRepo = (overrides = {}) => ({
  findOne: jest.fn(),
  find: jest.fn(),
  findAndCount: jest.fn(),
  save: jest.fn(),
  delete: jest.fn(),
  count: jest.fn(),
  ...overrides,
});

describe('AdminService', () => {
  let service: AdminService;
  let userRepo: ReturnType<typeof makeRepo>;
  let foodRepo: ReturnType<typeof makeRepo>;

  beforeEach(async () => {
    userRepo = makeRepo();
    foodRepo = makeRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AdminService,
        { provide: getRepositoryToken(User), useValue: userRepo },
        { provide: getRepositoryToken(Food), useValue: foodRepo },
      ],
    }).compile();

    service = module.get(AdminService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getUsers ─────────────────────────────────────────────────────────────

  describe('getUsers', () => {
    it('should return paginated users', async () => {
      userRepo.findAndCount.mockResolvedValue([[mockUser], 1]);
      const result = await service.getUsers(1, 20);
      expect(result.users).toHaveLength(1);
      expect(result.total).toBe(1);
    });

    it('should apply search filter when provided', async () => {
      userRepo.findAndCount.mockResolvedValue([[mockUser], 1]);
      await service.getUsers(1, 20, 'test');
      expect(userRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({ where: expect.any(Array) }),
      );
    });
  });

  // ─── getUserById ──────────────────────────────────────────────────────────

  describe('getUserById', () => {
    it('should return user', async () => {
      userRepo.findOne.mockResolvedValue(mockUser);
      const result = await service.getUserById('user-uuid');
      expect(result).toEqual(mockUser);
    });

    it('should throw NotFoundException if not found', async () => {
      userRepo.findOne.mockResolvedValue(null);
      await expect(service.getUserById('bad-id')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── banUser / unbanUser ──────────────────────────────────────────────────

  describe('banUser', () => {
    it('should set is_active=false', async () => {
      const user = { ...mockUser };
      userRepo.findOne.mockResolvedValue(user);
      userRepo.save.mockResolvedValue({ ...user, is_active: false });

      const result = await service.banUser('user-uuid');
      expect(result.is_active).toBe(false);
    });
  });

  describe('unbanUser', () => {
    it('should set is_active=true', async () => {
      const user = { ...mockUser, is_active: false };
      userRepo.findOne.mockResolvedValue(user);
      userRepo.save.mockResolvedValue({ ...user, is_active: true });

      const result = await service.unbanUser('user-uuid');
      expect(result.is_active).toBe(true);
    });
  });

  // ─── getPendingFoods ──────────────────────────────────────────────────────

  describe('getPendingFoods', () => {
    it('should return pending (unverified) foods', async () => {
      foodRepo.findAndCount.mockResolvedValue([[mockFood], 1]);
      const result = await service.getPendingFoods(1, 20);
      expect(result.foods).toHaveLength(1);
      expect(result.foods[0].is_verified).toBe(false);
    });
  });

  // ─── verifyFood ───────────────────────────────────────────────────────────

  describe('verifyFood', () => {
    it('should set is_verified=true', async () => {
      const food = { ...mockFood };
      foodRepo.findOne.mockResolvedValue(food);
      foodRepo.save.mockResolvedValue({ ...food, is_verified: true });

      const result = await service.verifyFood('food-uuid');
      expect(result.is_verified).toBe(true);
    });

    it('should throw NotFoundException if food not found', async () => {
      foodRepo.findOne.mockResolvedValue(null);
      await expect(service.verifyFood('bad-id')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── rejectFood ───────────────────────────────────────────────────────────

  describe('rejectFood', () => {
    it('should set is_active=false', async () => {
      foodRepo.findOne.mockResolvedValue({ ...mockFood });
      foodRepo.save.mockResolvedValue({ ...mockFood, is_active: false });
      await service.rejectFood('food-uuid');
      expect(foodRepo.save).toHaveBeenCalledWith(expect.objectContaining({ is_active: false }));
    });
  });

  // ─── deleteFood ───────────────────────────────────────────────────────────

  describe('deleteFood', () => {
    it('should delete food', async () => {
      foodRepo.findOne.mockResolvedValue(mockFood);
      foodRepo.delete.mockResolvedValue({ affected: 1 });
      await service.deleteFood('food-uuid');
      expect(foodRepo.delete).toHaveBeenCalledWith('food-uuid');
    });
  });

  // ─── getStats ────────────────────────────────────────────────────────────

  describe('getStats', () => {
    it('should return aggregated stats', async () => {
      userRepo.count
        .mockResolvedValueOnce(100) // totalUsers
        .mockResolvedValueOnce(90); // activeUsers
      foodRepo.count
        .mockResolvedValueOnce(500) // totalFoods
        .mockResolvedValueOnce(10); // pendingFoods

      const result = await service.getStats();
      expect(result.totalUsers).toBe(100);
      expect(result.activeUsers).toBe(90);
      expect(result.totalFoods).toBe(500);
      expect(result.pendingFoods).toBe(10);
    });
  });
});
