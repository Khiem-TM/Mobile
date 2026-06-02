import { ForbiddenException } from '@nestjs/common';
import { FoodsService } from './foods.service';

const redisMock = () => ({
  getJson: jest.fn().mockResolvedValue(null),
  setJson: jest.fn().mockResolvedValue(undefined),
  del: jest.fn().mockResolvedValue(undefined),
  delByPattern: jest.fn().mockResolvedValue(undefined),
});

const serviceWith = (overrides: Partial<Record<string, any>> = {}) => {
  const foodRepository = {
    findAndCount: jest.fn(),
    findOne: jest.fn(),
    create: jest.fn((data) => data),
    save: jest.fn((data) => Promise.resolve({ id: 'food-1', ...data })),
    delete: jest.fn(),
    ...overrides.foodRepository,
  };
  const favoriteRepository = {
    find: jest.fn(),
    findOne: jest.fn(),
    ...overrides.favoriteRepository,
  };
  const redisService = {
    ...redisMock(),
    ...overrides.redisService,
  };
  const dataSource = {
    transaction: jest.fn((cb) => cb(overrides.manager)),
    ...overrides.dataSource,
  };

  const service = new FoodsService(
    foodRepository as any,
    {} as any,
    favoriteRepository as any,
    {} as any,
    dataSource as any,
    redisService as any,
  );

  return { service, foodRepository, favoriteRepository, redisService, dataSource };
};

describe('FoodsService', () => {
  it('lists only active system foods through public search', async () => {
    const { service, foodRepository } = serviceWith({
      foodRepository: { findAndCount: jest.fn().mockResolvedValue([[], 0]) },
    });

    await service.findAll('', 1, 20);

    expect(foodRepository.findAndCount).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { is_custom: false, is_active: true, is_verified: true },
      }),
    );
  });

  it('does not return cached custom food from public get-by-id', async () => {
    const cachedCustomFood = {
      id: 'custom-food',
      is_custom: true,
      is_active: true,
      created_by_user_id: 'owner-1',
    };
    const { service, foodRepository } = serviceWith({
      redisService: { getJson: jest.fn().mockResolvedValue(cachedCustomFood) },
      foodRepository: {
        findOne: jest.fn().mockResolvedValue({
          id: 'system-food',
          is_custom: false,
          is_active: true,
        }),
      },
    });

    const result = await service.findOne('custom-food');

    expect(foodRepository.findOne).toHaveBeenCalledWith({
      where: { id: 'custom-food', is_custom: false, is_active: true, is_verified: true },
    });
    expect(result.id).toBe('system-food');
  });

  it('forbids viewing another user custom food', async () => {
    const { service } = serviceWith({
      foodRepository: {
        findOne: jest.fn().mockResolvedValue({
          id: 'custom-food',
          is_custom: true,
          is_active: true,
          created_by_user_id: 'owner-1',
        }),
      },
    });

    await expect(service.findOneForUser('custom-food', 'user-2')).rejects.toBeInstanceOf(
      ForbiddenException,
    );
  });

  it('does not increment favorite count when insert is ignored', async () => {
    const insertBuilder = {
      insert: jest.fn().mockReturnThis(),
      into: jest.fn().mockReturnThis(),
      values: jest.fn().mockReturnThis(),
      orIgnore: jest.fn().mockReturnThis(),
      returning: jest.fn().mockReturnThis(),
      execute: jest.fn().mockResolvedValue({ raw: [] }),
    };
    const manager = {
      createQueryBuilder: jest.fn().mockReturnValue(insertBuilder),
      increment: jest.fn(),
    };
    const { service } = serviceWith({
      manager,
      foodRepository: {
        findOne: jest.fn().mockResolvedValue({
          id: 'food-1',
          is_custom: false,
          is_active: true,
        }),
      },
    });

    await service.addFavorite('user-1', 'food-1');

    expect(manager.increment).not.toHaveBeenCalled();
  });
});
