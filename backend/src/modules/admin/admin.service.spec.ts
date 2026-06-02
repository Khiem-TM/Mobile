import { ForbiddenException, UnauthorizedException } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { AdminService } from './admin.service';
import { UserRole } from '../../common/enums/user-role.enum';

describe('AdminService', () => {
  function createService(overrides: Record<string, any> = {}) {
    const userRepo = {
      findOne: jest.fn(),
      count: jest.fn().mockResolvedValue(0),
      find: jest.fn().mockResolvedValue([]),
    };
    const foodRepo = {
      findOne: jest.fn(),
      findAndCount: jest.fn().mockResolvedValue([[], 0]),
      save: jest.fn((entity) => Promise.resolve(entity)),
      count: jest.fn().mockResolvedValue(0),
    };
    const exerciseRepo = { count: jest.fn().mockResolvedValue(0) };
    const trainingSessionRepo = { count: jest.fn().mockResolvedValue(0) };
    const blogRepo = { count: jest.fn().mockResolvedValue(0) };
    const dataSource = { query: jest.fn().mockResolvedValue([{ count: 0 }]) };
    const authService = {
      generateAuthResponse: jest.fn().mockResolvedValue({ access_token: 'token' }),
      logout: jest.fn(),
    };
    const notificationsService = { create: jest.fn(), createMany: jest.fn() };
    const redisService = {
      del: jest.fn(),
      delByPattern: jest.fn(),
      getJson: jest.fn(),
      setJson: jest.fn(),
      getClient: jest.fn().mockReturnValue({ ping: jest.fn().mockResolvedValue('PONG') }),
    };
    const auditLogService = { recordFromContext: jest.fn() };

    const service = new AdminService(
      overrides.userRepo ?? userRepo,
      overrides.foodRepo ?? foodRepo,
      overrides.exerciseRepo ?? exerciseRepo,
      overrides.trainingSessionRepo ?? trainingSessionRepo,
      overrides.blogRepo ?? blogRepo,
      dataSource as any,
      authService as any,
      { get: jest.fn() } as any,
      notificationsService as any,
      redisService as any,
      auditLogService as any,
    );
    return {
      service,
      userRepo,
      foodRepo,
      authService,
      redisService,
      auditLogService,
    };
  }

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('logs in active DB-backed admins through the shared auth response flow', async () => {
    const { service, userRepo, authService, auditLogService } = createService();
    userRepo.findOne.mockResolvedValue({
      id: 'admin-id',
      email: 'admin@test.com',
      role: UserRole.ADMIN,
      password_hash: await bcrypt.hash('Password123', 4),
      is_active: true,
    });

    const result = await service.adminLogin('ADMIN@test.com', 'Password123');

    expect(result).toEqual({ access_token: 'token' });
    expect(userRepo.findOne).toHaveBeenCalledWith({
      where: { email: 'admin@test.com', role: UserRole.ADMIN },
    });
    expect(authService.generateAuthResponse).toHaveBeenCalled();
    expect(auditLogService.recordFromContext).toHaveBeenCalledWith(
      undefined,
      expect.objectContaining({ action: 'admin.login' }),
    );
  });

  it('rejects inactive admin accounts', async () => {
    const { service, userRepo } = createService();
    userRepo.findOne.mockResolvedValue({
      id: 'admin-id',
      email: 'admin@test.com',
      role: UserRole.ADMIN,
      password_hash: 'hash',
      is_active: false,
    });

    await expect(service.adminLogin('admin@test.com', 'Password123')).rejects.toBeInstanceOf(
      ForbiddenException,
    );
  });

  it('rejects non-admin or missing accounts', async () => {
    const { service, userRepo } = createService();
    userRepo.findOne.mockResolvedValue(null);

    await expect(service.adminLogin('user@test.com', 'Password123')).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('soft-deletes foods and invalidates public food caches', async () => {
    const { service, foodRepo, redisService } = createService();
    const food = { id: 'food-id', is_active: true, is_custom: false };
    foodRepo.findOne.mockResolvedValue(food);

    await service.deleteFood('food-id');

    expect(food.is_active).toBe(false);
    expect(foodRepo.save).toHaveBeenCalledWith(food);
    expect(redisService.del).toHaveBeenCalledWith('cache:foods:one:food-id');
    expect(redisService.delByPattern).toHaveBeenCalledWith('cache:foods:list:*');
    expect(redisService.delByPattern).toHaveBeenCalledWith('cache:foods:explore:*');
  });

  it('does not include custom foods in the admin pending queue', async () => {
    const { service, foodRepo } = createService();

    await service.getPendingFoods(1, 20);

    expect(foodRepo.findAndCount).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { is_verified: false, is_active: true, is_custom: false },
      }),
    );
  });

  it('blocks admin verification for custom foods', async () => {
    const { service, foodRepo } = createService();
    const food = { id: 'custom-food', is_verified: false, is_active: true, is_custom: true };
    foodRepo.findOne.mockResolvedValue(food);

    await expect(service.verifyFood('custom-food')).rejects.toBeInstanceOf(
      ForbiddenException,
    );
    expect(foodRepo.save).not.toHaveBeenCalled();
    expect(food.is_verified).toBe(false);
  });

  it('counts only global foods in admin stats', async () => {
    const { service, foodRepo } = createService();

    await service.getStats();

    expect(foodRepo.count).toHaveBeenCalledWith({
      where: { is_active: true, is_custom: false },
    });
    expect(foodRepo.count).toHaveBeenCalledWith({
      where: { is_verified: false, is_active: true, is_custom: false },
    });
  });
});
