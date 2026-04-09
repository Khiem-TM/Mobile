import { Test, TestingModule } from '@nestjs/testing';
import { ForbiddenException, NotFoundException } from '@nestjs/common';
import { BodyMetricsService } from './body-metrics.service';
import { BODY_METRICS_REPOSITORY } from '../body-metrics.constants';
import { UsersService } from '../../users/users.service';

const makeRepo = () => ({
  findHistory: jest.fn(),
  findLatest: jest.fn(),
  upsert: jest.fn(),
  findPhotosByUser: jest.fn(),
  savePhoto: jest.fn(),
  findPhotoById: jest.fn(),
  deletePhoto: jest.fn(),
  findRange: jest.fn(),
  getProgressSummary: jest.fn(),
});

describe('BodyMetricsService', () => {
  let service: BodyMetricsService;
  let repo: ReturnType<typeof makeRepo>;
  let usersService: jest.Mocked<Partial<UsersService>>;

  beforeEach(async () => {
    repo = makeRepo();
    usersService = { getHealthProfile: jest.fn().mockResolvedValue(null) };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        BodyMetricsService,
        { provide: BODY_METRICS_REPOSITORY, useValue: repo },
        { provide: UsersService, useValue: usersService },
      ],
    }).compile();

    service = module.get(BodyMetricsService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getHistory ───────────────────────────────────────────────────────────

  describe('getHistory', () => {
    it('should return body metrics history', async () => {
      const metrics = [
        { id: '1', weightKg: 70, recordedAt: '2024-01-10' },
        { id: '2', weightKg: 69.5, recordedAt: '2024-01-15' },
      ];
      repo.findHistory.mockResolvedValue(metrics);
      const result = await service.getHistory('user-uuid', {});
      expect(result).toHaveLength(2);
    });
  });

  // ─── getLatest ────────────────────────────────────────────────────────────

  describe('getLatest', () => {
    it('should return latest body metric', async () => {
      const metric = { id: '2', weightKg: 69.5, bmi: 22.5 };
      repo.findLatest.mockResolvedValue(metric);
      const result = await service.getLatest('user-uuid');
      expect(result?.weightKg).toBe(69.5);
    });

    it('should return null if no metrics', async () => {
      repo.findLatest.mockResolvedValue(null);
      const result = await service.getLatest('user-uuid');
      expect(result).toBeNull();
    });
  });

  // ─── upsertMetric ─────────────────────────────────────────────────────────

  describe('upsert', () => {
    it('should calculate BMI when health profile has height', async () => {
      (usersService.getHealthProfile as jest.Mock).mockResolvedValue({
        heightCm: 175,
      });
      repo.upsert.mockImplementation((userId, data) =>
        Promise.resolve({ userId, ...data }),
      );

      const result = await service.upsert('user-uuid', {
        weightKg: 70,
        recordedAt: '2024-01-15',
      });

      // BMI should be calculated via BMIUtil
      expect(repo.upsert).toHaveBeenCalledWith(
        'user-uuid',
        expect.objectContaining({ bmi: expect.any(Number) }),
      );
      expect(result).toBeDefined();
    });

    it('should not calculate BMI when no height in profile', async () => {
      (usersService.getHealthProfile as jest.Mock).mockResolvedValue(null);
      repo.upsert.mockImplementation((userId, data) =>
        Promise.resolve({ userId, ...data }),
      );

      await service.upsert('user-uuid', {
        weightKg: 70,
        recordedAt: '2024-01-15',
      });

      const call = repo.upsert.mock.calls[0][1];
      expect(call.bmi).toBeUndefined();
    });
  });

  // ─── getPhotos ────────────────────────────────────────────────────────────

  describe('getPhotosByUser', () => {
    it('should return progress photos', async () => {
      const photos = [{ id: 'p1', photoUrl: '/uploads/front.jpg' }];
      repo.findPhotosByUser.mockResolvedValue(photos);
      const result = await service.getPhotosByUser('user-uuid');
      expect(result).toHaveLength(1);
    });
  });

  // ─── deletePhoto ─────────────────────────────────────────────────────────

  describe('deletePhoto', () => {
    it('should delete photo if user owns it', async () => {
      repo.findPhotoById.mockResolvedValue({ id: 'p1', userId: 'user-uuid' });
      repo.deletePhoto.mockResolvedValue(undefined);

      await service.deletePhoto('user-uuid', 'p1');
      expect(repo.deletePhoto).toHaveBeenCalledWith('p1');
    });

    it('should throw NotFoundException if photo not found', async () => {
      repo.findPhotoById.mockResolvedValue(null);
      await expect(service.deletePhoto('user-uuid', 'bad-id')).rejects.toThrow(
        NotFoundException,
      );
    });

    it('should throw ForbiddenException if photo belongs to another user', async () => {
      repo.findPhotoById.mockResolvedValue({ id: 'p1', userId: 'other-uuid' });
      await expect(service.deletePhoto('user-uuid', 'p1')).rejects.toThrow(
        ForbiddenException,
      );
    });
  });
});
