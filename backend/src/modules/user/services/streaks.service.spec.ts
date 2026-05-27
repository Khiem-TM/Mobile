import { StreaksService } from './streaks.service';
import { StreakType } from '../../../common/enums/streak-type.enum';

describe('StreaksService', () => {
  it('recomputes current streak and maxStreak from distinct workout dates', async () => {
    const baseStreak = {
      id: 'streak-1',
      user_id: 'user-1',
      streak_type: StreakType.WORKOUT,
      current_streak: 0,
      longest_streak: 0,
      last_activity_date: null,
    };
    const repository = {
      findOrCreate: jest.fn().mockResolvedValue(baseStreak),
      updateStreak: jest.fn().mockImplementation((id, current, longest, lastDate) => ({
        ...baseStreak,
        id,
        current_streak: current,
        longest_streak: longest,
        last_activity_date: lastDate,
      })),
    };

    const service = new StreaksService(
      repository as any,
      {} as any,
      {} as any,
    );

    const result = await service.recomputeFromActivityDates('user-1', StreakType.WORKOUT, [
      '2026-05-20',
      '2026-05-21',
      '2026-05-23',
      '2026-05-24',
      '2026-05-25',
      '2026-05-25',
    ]);

    expect(repository.updateStreak).toHaveBeenCalledWith('streak-1', 3, 3, '2026-05-25');
    expect(result).toEqual(
      expect.objectContaining({
        currentStreak: 3,
        maxStreak: 3,
        lastActivityDate: '2026-05-25',
      }),
    );
  });

  it('resets streak values when a user has no workout dates', async () => {
    const baseStreak = {
      id: 'streak-1',
      user_id: 'user-1',
      streak_type: StreakType.WORKOUT,
      current_streak: 5,
      longest_streak: 7,
      last_activity_date: '2026-05-25',
    };
    const repository = {
      findOrCreate: jest.fn().mockResolvedValue(baseStreak),
      updateStreak: jest.fn().mockImplementation((id, current, longest, lastDate) => ({
        ...baseStreak,
        id,
        current_streak: current,
        longest_streak: longest,
        last_activity_date: lastDate,
      })),
    };

    const service = new StreaksService(repository as any, {} as any, {} as any);

    const result = await service.recomputeFromActivityDates('user-1', StreakType.WORKOUT, []);

    expect(repository.updateStreak).toHaveBeenCalledWith('streak-1', 0, 0, null);
    expect(result).toEqual(
      expect.objectContaining({
        currentStreak: 0,
        maxStreak: 0,
        lastActivityDate: null,
      }),
    );
  });
});
