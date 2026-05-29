import { TrainingSessionsService } from './training-sessions.service';
import { ExerciseType } from '../enums/exercise-type.enum';
import { IntensityLevel } from '../enums/intensity-level.enum';

describe('TrainingSessionsService', () => {
  it('estimates GYM duration from sets, reps, and rest when durationMinutes is omitted', async () => {
    const sessionRepo = {
      findById: jest
        .fn()
        .mockResolvedValueOnce({ id: 'session-1', userId: 'user-1' })
        .mockResolvedValueOnce({ id: 'session-1', userId: 'user-1', items: [] }),
      addItem: jest.fn().mockResolvedValue({ id: 'item-1' }),
      findItemsBySession: jest.fn().mockResolvedValue([{ durationMinutes: 4, caloriesBurned: 42 }]),
      updateTotals: jest.fn().mockResolvedValue(undefined),
      findDistinctSessionDates: jest.fn().mockResolvedValue(['2026-05-25']),
    };
    const exerciseRepo = {
      findById: jest.fn().mockResolvedValue({
        id: 'exercise-1',
        exerciseType: ExerciseType.GYM,
        metValue: 0,
        defaultSets: null,
        defaultReps: null,
        restTimeSeconds: null,
      }),
    };
    const caloriesCalcService = {
      getUserWeight: jest.fn().mockResolvedValue(70),
      calculateForExercise: jest.fn().mockReturnValue(42),
    };
    const streaksService = {
      recomputeFromActivityDates: jest.fn().mockResolvedValue(undefined),
    };

    const dataSource = { transaction: jest.fn() };

    const service = new TrainingSessionsService(
      sessionRepo as any,
      exerciseRepo as any,
      caloriesCalcService as any,
      streaksService as any,
      dataSource as any,
    );

    await service.addItem('user-1', 'session-1', {
      exerciseId: 'exercise-1',
      exerciseType: ExerciseType.GYM,
      sets: 3,
      reps: 10,
      restTimeSeconds: 60,
      intensityLevel: IntensityLevel.MEDIUM,
    });

    expect(sessionRepo.addItem).toHaveBeenCalledWith(
      expect.objectContaining({
        durationMinutes: 4,
        caloriesBurned: 42,
        sets: 3,
        reps: 10,
        restTimeSeconds: 60,
      }),
    );
    expect(caloriesCalcService.calculateForExercise).toHaveBeenCalledWith(
      expect.any(Object),
      4,
      expect.objectContaining({ userWeightKg: 70 }),
    );
  });
});
