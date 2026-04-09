import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException } from '@nestjs/common';
import { ILike } from 'typeorm';
import { FoodsService } from './foods.service';
import { Food } from './entities/food.entity';
import { FoodBarcode } from './entities/food-barcode.entity';
import { FoodUserFavorite } from './entities/food-user-favorite.entity';

const mockFood: Partial<Food> = {
  id: 'food-uuid',
  name: 'Apple',
  calories_per_100g: 52,
  protein_per_100g: 0.3,
  fat_per_100g: 0.2,
  carbs_per_100g: 14,
  serving_size_g: 150,
  serving_unit: 'g',
};

const makeRepo = (overrides = {}) => ({
  findOne: jest.fn(),
  find: jest.fn(),
  findAndCount: jest.fn(),
  save: jest.fn(),
  create: jest.fn(),
  delete: jest.fn(),
  ...overrides,
});

describe('FoodsService', () => {
  let service: FoodsService;
  let foodRepo: ReturnType<typeof makeRepo>;
  let barcodeRepo: ReturnType<typeof makeRepo>;
  let favoriteRepo: ReturnType<typeof makeRepo>;

  beforeEach(async () => {
    foodRepo = makeRepo();
    barcodeRepo = makeRepo();
    favoriteRepo = makeRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        FoodsService,
        { provide: getRepositoryToken(Food), useValue: foodRepo },
        { provide: getRepositoryToken(FoodBarcode), useValue: barcodeRepo },
        { provide: getRepositoryToken(FoodUserFavorite), useValue: favoriteRepo },
      ],
    }).compile();

    service = module.get(FoodsService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── findAll ─────────────────────────────────────────────────────────────

  describe('findAll', () => {
    it('should return paginated foods without query', async () => {
      foodRepo.findAndCount.mockResolvedValue([[mockFood], 1]);
      const result = await service.findAll('', 1, 20);
      expect(result.foods).toHaveLength(1);
      expect(result.total).toBe(1);
      expect(result.page).toBe(1);
    });

    it('should search by name with ILike', async () => {
      foodRepo.findAndCount.mockResolvedValue([[mockFood], 1]);
      await service.findAll('apple', 1, 10);
      expect(foodRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({ where: [{ name: ILike('%apple%') }] }),
      );
    });

    it('should apply pagination correctly', async () => {
      foodRepo.findAndCount.mockResolvedValue([[], 100]);
      await service.findAll('', 3, 10);
      expect(foodRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({ skip: 20, take: 10 }),
      );
    });
  });

  // ─── findOne ─────────────────────────────────────────────────────────────

  describe('findOne', () => {
    it('should return food by id', async () => {
      foodRepo.findOne.mockResolvedValue(mockFood);
      const result = await service.findOne('food-uuid');
      expect(result).toEqual(mockFood);
    });

    it('should throw NotFoundException if not found', async () => {
      foodRepo.findOne.mockResolvedValue(null);
      await expect(service.findOne('bad-id')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── findByBarcode ───────────────────────────────────────────────────────

  describe('findByBarcode', () => {
    it('should return food by barcode', async () => {
      barcodeRepo.findOne.mockResolvedValue({ barcode: '1234567890', food: mockFood });
      const result = await service.findByBarcode('1234567890');
      expect(result).toEqual(mockFood);
    });

    it('should throw NotFoundException if barcode not found', async () => {
      barcodeRepo.findOne.mockResolvedValue(null);
      await expect(service.findByBarcode('0000000000')).rejects.toThrow(NotFoundException);
    });

    it('should throw NotFoundException if barcode record has no food', async () => {
      barcodeRepo.findOne.mockResolvedValue({ barcode: '1234', food: null });
      await expect(service.findByBarcode('1234')).rejects.toThrow(NotFoundException);
    });
  });

  // ─── Favorites ───────────────────────────────────────────────────────────

  describe('getFavorites', () => {
    it('should return array of foods from favorites', async () => {
      favoriteRepo.find.mockResolvedValue([{ food: mockFood }]);
      const result = await service.getFavorites('user-uuid');
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual(mockFood);
    });
  });

  describe('addFavorite', () => {
    it('should save favorite if not already exists', async () => {
      favoriteRepo.findOne.mockResolvedValue(null);
      favoriteRepo.save.mockResolvedValue({});
      await service.addFavorite('user-uuid', 'food-uuid');
      expect(favoriteRepo.save).toHaveBeenCalledWith({
        user_id: 'user-uuid',
        food_id: 'food-uuid',
      });
    });

    it('should not duplicate if already in favorites', async () => {
      favoriteRepo.findOne.mockResolvedValue({ user_id: 'user-uuid', food_id: 'food-uuid' });
      await service.addFavorite('user-uuid', 'food-uuid');
      expect(favoriteRepo.save).not.toHaveBeenCalled();
    });
  });

  describe('removeFavorite', () => {
    it('should delete favorite', async () => {
      favoriteRepo.delete.mockResolvedValue({ affected: 1 });
      await service.removeFavorite('user-uuid', 'food-uuid');
      expect(favoriteRepo.delete).toHaveBeenCalledWith({
        user_id: 'user-uuid',
        food_id: 'food-uuid',
      });
    });
  });

  // ─── createCustom ────────────────────────────────────────────────────────

  describe('createCustom', () => {
    it('should create a custom food with is_custom=true', async () => {
      const dto = { name: 'My Food', calories_per_100g: 200 } as any;
      const created = { ...dto, is_custom: true, created_by_user_id: 'user-uuid', is_verified: false };
      foodRepo.create.mockReturnValue(created);
      foodRepo.save.mockResolvedValue(created);

      const result = await service.createCustom('user-uuid', dto);
      expect(result.is_custom).toBe(true);
      expect(result.created_by_user_id).toBe('user-uuid');
      expect(result.is_verified).toBe(false);
    });
  });
});
