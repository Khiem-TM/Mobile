import { Test, TestingModule } from '@nestjs/testing';
import { StreaksService } from './streaks.service';
import { STREAKS_REPOSITORY } from './streaks.constants';
import { StreakType } from '../../common/enums/streak-type.enum';

const makeRepo = () => ({
  findByUser: jest.fn(),
  findOrCreate: jest.fn(),
  updateStreak: jest.fn(),
  findAllByType: jest.fn(),
});

describe('StreaksService', () => {
  let service: StreaksService;
  let repo: ReturnType<typeof makeRepo>;

  beforeEach(async () => {
    repo = makeRepo();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        StreaksService,
        { provide: STREAKS_REPOSITORY, useValue: repo },
      ],
    }).compile();

    service = module.get(StreaksService);
  });

  afterEach(() => jest.clearAllMocks());

  // ─── getStreaks ───────────────────────────────────────────────────────────

  describe('getStreaks', () => {
    it('should return all streaks for a user', async () => {
      const streaks = [
        { id: 's1', streak_type: StreakType.LOGIN, current_streak: 5, longest_streak: 10 },
        { id: 's2', streak_type: StreakType.WORKOUT, current_streak: 3, longest_streak: 7 },
      ];
      repo.findByUser.mockResolvedValue(streaks);
      const result = await service.getStreaks('user-uuid');
      expect(result).toHaveLength(2);
      expect(repo.findByUser).toHaveBeenCalledWith('user-uuid');
    });
  });

  // ─── updateActivity ───────────────────────────────────────────────────────

  describe('updateActivity', () => {
    const baseStreak = {
      id: 'streak-uuid',
      user_id: 'user-uuid',
      streak_type: StreakType.WORKOUT,
      current_streak: 3,
      longest_streak: 5,
      last_activity_date: '2024-01-14',
    };

    it('should increment streak if activity is next consecutive day', async () => {
      repo.findOrCreate.mockResolvedValue(baseStreak);
      repo.updateStreak.mockResolvedValue({ ...baseStreak, current_streak: 4 });

      const result = await service.updateActivity('user-uuid', StreakType.WORKOUT, '2024-01-15');
      // yesterday = '2024-01-14' which matches lastDate → increment
      expect(repo.updateStreak).toHaveBeenCalledWith(
        'streak-uuid', 4, 5, '2024-01-15',
      );
      expect(result.current_streak).toBe(4);
    });

    it('should reset streak to 1 if not consecutive', async () => {
      repo.findOrCreate.mockResolvedValue({ ...baseStreak, last_activity_date: '2024-01-10' });
      repo.updateStreak.mockResolvedValue({ ...baseStreak, current_streak: 1 });

      await service.updateActivity('user-uuid', StreakType.WORKOUT, '2024-01-15');
      expect(repo.updateStreak).toHaveBeenCalledWith(
        'streak-uuid', 1, 5, '2024-01-15',
      );
    });

    it('should update longest_streak if current exceeds it', async () => {
      repo.findOrCreate.mockResolvedValue({
        ...baseStreak,
        current_streak: 5,
        longest_streak: 5,
        last_activity_date: '2024-01-14',
      });
      repo.updateStreak.mockResolvedValue({ current_streak: 6, longest_streak: 6 });

      await service.updateActivity('user-uuid', StreakType.WORKOUT, '2024-01-15');
      expect(repo.updateStreak).toHaveBeenCalledWith(
        'streak-uuid', 6, 6, '2024-01-15',
      );
    });

    it('should not update if same day as last activity', async () => {
      repo.findOrCreate.mockResolvedValue({ ...baseStreak, last_activity_date: '2024-01-15' });

      const result = await service.updateActivity('user-uuid', StreakType.WORKOUT, '2024-01-15');
      expect(repo.updateStreak).not.toHaveBeenCalled();
      expect(result).toEqual(expect.objectContaining({ last_activity_date: '2024-01-15' }));
    });
  });

  // ─── resetExpiredStreaks ──────────────────────────────────────────────────

  describe('resetExpiredStreaks', () => {
    it('should reset streaks with last activity before yesterday', async () => {
      const oldStreak = {
        id: 's1',
        user_id: 'user-uuid',
        last_activity_date: '2024-01-01',
        current_streak: 10,
        longest_streak: 15,
      };
      repo.findAllByType.mockResolvedValue([oldStreak]);
      repo.updateStreak.mockResolvedValue({});

      await service.resetExpiredStreaks();

      expect(repo.updateStreak).toHaveBeenCalledWith('s1', 0, 15, '2024-01-01');
    });

    it('should not reset streaks that were active yesterday', async () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().split('T')[0];

      repo.findAllByType.mockResolvedValue([
        { id: 's1', last_activity_date: yesterdayStr, current_streak: 5, longest_streak: 10 },
      ]);

      await service.resetExpiredStreaks();
      // last_activity_date === yesterday → should not reset (< yesterday is the condition)
      expect(repo.updateStreak).not.toHaveBeenCalled();
    });
  });
});
