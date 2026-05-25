import { Injectable, Inject, NotFoundException, ForbiddenException, forwardRef } from '@nestjs/common';
import { TRAINING_SESSIONS_REPOSITORY, EXERCISES_REPOSITORY } from '../train.constants';
import type { ITrainingSessionsRepository } from '../repositories/training-sessions.repository.interface';
import type { IExercisesRepository } from '../repositories/exercises.repository.interface';
import { CreateTrainingSessionDto, CreateSessionItemDto } from '../dto/create-training-session.dto';
import { UpdateTrainingSessionDto } from '../dto/update-training-session.dto';
import { UpdateTrainingSessionItemDto } from '../dto/update-training-session-item.dto';
import { CaloriesCalculationService } from './calories-calculation.service';
import { StreaksService } from '../../user/services/streaks.service';
import { StreakType } from '../../../common/enums/streak-type.enum';
import { TrainingSession } from '../entities/training-session.entity';
import { TrainingSessionItem } from '../entities/training-session-item.entity';
import { ExerciseType } from '../enums/exercise-type.enum';

@Injectable()
export class TrainingSessionsService {
  constructor(
    @Inject(TRAINING_SESSIONS_REPOSITORY)
    private readonly sessionRepo: ITrainingSessionsRepository,
    @Inject(EXERCISES_REPOSITORY)
    private readonly exerciseRepo: IExercisesRepository,
    private readonly caloriesCalcService: CaloriesCalculationService,
    @Inject(forwardRef(() => StreaksService))
    private readonly streaksService: StreaksService,
  ) {}

  private formatSession(session: TrainingSession) {
    return {
      ...session,
      totalCaloriesBurned: Number(session.totalCaloriesBurned ?? 0),
      items: (session.items ?? []).map((item) => ({
        ...item,
        weightKg: item.weightKg === null ? null : Number(item.weightKg),
        caloriesBurned: Number(item.caloriesBurned ?? 0),
        distanceKm: item.distanceKm === null ? null : Number(item.distanceKm),
        exercise: item.exercise ?? null,
      })),
    };
  }

  private async calcItemCalories(
    userId: string,
    item: CreateSessionItemDto,
    userWeightKg: number,
  ): Promise<number> {
    const exercise = await this.exerciseRepo.findById(item.exerciseId);
    if (!exercise) throw new NotFoundException(`Exercise ${item.exerciseId} not found`);

    const durationMinutes =
      item.durationMinutes ??
      (item.exerciseType === ExerciseType.GYM
        ? (item.sets ?? 3) * (item.reps ?? 10) * 3 / 60 // rough estimate: 3s per rep
        : 20);

    return this.caloriesCalcService.calculateForExercise(exercise, durationMinutes, {
      intensityLevel: item.intensityLevel,
      weightKg: item.weightKg,
      userWeightKg,
    });
  }

  private async recalcSessionTotals(sessionId: string): Promise<{ totalDurationMinutes: number; totalCaloriesBurned: number }> {
    const items = await this.sessionRepo.findItemsBySession(sessionId);
    const totalDurationMinutes = items.reduce((sum, i) => sum + (i.durationMinutes ?? 0), 0);
    const totalCaloriesBurned = Number(
      items.reduce((sum, i) => sum + Number(i.caloriesBurned ?? 0), 0).toFixed(2),
    );
    return { totalDurationMinutes, totalCaloriesBurned };
  }

  async createSession(userId: string, dto: CreateTrainingSessionDto): Promise<any> {
    const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
    const exercises = await this.exerciseRepo.findByIds(dto.items.map((i) => i.exerciseId));
    const exerciseMap = new Map(exercises.map((e) => [e.id, e]));

    let totalDurationMinutes = 0;
    let totalCaloriesBurned = 0;
    const itemDataList: Array<Partial<TrainingSessionItem>> = [];

    for (const item of dto.items) {
      const exercise = exerciseMap.get(item.exerciseId);
      if (!exercise) throw new NotFoundException(`Exercise ${item.exerciseId} not found`);

      const duration = item.durationMinutes ?? 0;
      const calories = this.caloriesCalcService.calculateForExercise(exercise, duration, {
        intensityLevel: item.intensityLevel,
        weightKg: item.weightKg,
        userWeightKg,
      });

      itemDataList.push({
        exerciseId: item.exerciseId,
        exerciseType: item.exerciseType,
        orderIndex: item.orderIndex ?? 0,
        durationMinutes: duration,
        caloriesBurned: calories,
        note: item.note ?? null,
        sets: item.sets ?? null,
        reps: item.reps ?? null,
        weightKg: item.weightKg ?? null,
        restTimeSeconds: item.restTimeSeconds ?? null,
        intensityLevel: item.intensityLevel ?? null,
        distanceKm: item.distanceKm ?? null,
        pace: item.pace ?? null,
      });

      totalDurationMinutes += duration;
      totalCaloriesBurned += calories;
    }

    const session = await this.sessionRepo.createSession({
      userId,
      sessionDate: dto.sessionDate,
      title: dto.title ?? null,
      note: dto.note ?? null,
      totalDurationMinutes,
      totalCaloriesBurned: Number(totalCaloriesBurned.toFixed(2)),
    });

    for (const itemData of itemDataList) {
      await this.sessionRepo.addItem({ ...itemData, sessionId: session.id });
    }

    await this.streaksService.updateActivity(userId, StreakType.WORKOUT, dto.sessionDate);

    const saved = await this.sessionRepo.findById(session.id);
    return this.formatSession(saved as TrainingSession);
  }

  async getSessions(userId: string, limit = 20): Promise<any[]> {
    const sessions = await this.sessionRepo.findByUser(userId, limit);
    return sessions.map((s) => this.formatSession(s));
  }

  async getSessionsByDateRange(userId: string, fromDate: string, toDate: string): Promise<any[]> {
    const sessions = await this.sessionRepo.findByDateRange(userId, fromDate, toDate);
    return sessions.map((s) => this.formatSession(s));
  }

  async getSessionsByDate(userId: string, date: string): Promise<any[]> {
    return this.getSessionsByDateRange(userId, date, date);
  }

  async getSessionById(userId: string, sessionId: string): Promise<any> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');
    return this.formatSession(session);
  }

  async updateSession(userId: string, sessionId: string, dto: UpdateTrainingSessionDto): Promise<any> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');

    const updateData: Partial<TrainingSession> = {};
    if (dto.sessionDate !== undefined) updateData.sessionDate = dto.sessionDate;
    if (dto.title !== undefined) updateData.title = dto.title;
    if (dto.note !== undefined) updateData.note = dto.note;

    const updated = await this.sessionRepo.updateSession(sessionId, updateData);
    return this.formatSession(updated);
  }

  async deleteSession(userId: string, sessionId: string): Promise<void> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');
    await this.sessionRepo.deleteSession(sessionId);
  }

  async addItem(userId: string, sessionId: string, dto: CreateSessionItemDto): Promise<any> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');

    const exercise = await this.exerciseRepo.findById(dto.exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');

    const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
    const duration = dto.durationMinutes ?? 0;
    const calories = this.caloriesCalcService.calculateForExercise(exercise, duration, {
      intensityLevel: dto.intensityLevel,
      weightKg: dto.weightKg,
      userWeightKg,
    });

    await this.sessionRepo.addItem({
      sessionId,
      exerciseId: dto.exerciseId,
      exerciseType: dto.exerciseType,
      orderIndex: dto.orderIndex ?? 0,
      durationMinutes: duration,
      caloriesBurned: calories,
      note: dto.note ?? null,
      sets: dto.sets ?? null,
      reps: dto.reps ?? null,
      weightKg: dto.weightKg ?? null,
      restTimeSeconds: dto.restTimeSeconds ?? null,
      intensityLevel: dto.intensityLevel ?? null,
      distanceKm: dto.distanceKm ?? null,
      pace: dto.pace ?? null,
    });

    const totals = await this.recalcSessionTotals(sessionId);
    await this.sessionRepo.updateTotals(sessionId, totals.totalDurationMinutes, totals.totalCaloriesBurned);

    const updated = await this.sessionRepo.findById(sessionId);
    return this.formatSession(updated as TrainingSession);
  }

  async updateItem(
    userId: string,
    sessionId: string,
    itemId: string,
    dto: UpdateTrainingSessionItemDto,
  ): Promise<any> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');

    const item = await this.sessionRepo.findItemById(itemId);
    if (!item || item.sessionId !== sessionId) throw new NotFoundException('Item not found in this session');

    const updateData: Partial<TrainingSessionItem> = {};
    if (dto.orderIndex !== undefined) updateData.orderIndex = dto.orderIndex;
    if (dto.durationMinutes !== undefined) updateData.durationMinutes = dto.durationMinutes;
    if (dto.note !== undefined) updateData.note = dto.note;
    if (dto.sets !== undefined) updateData.sets = dto.sets;
    if (dto.reps !== undefined) updateData.reps = dto.reps;
    if (dto.weightKg !== undefined) updateData.weightKg = dto.weightKg;
    if (dto.restTimeSeconds !== undefined) updateData.restTimeSeconds = dto.restTimeSeconds;
    if (dto.intensityLevel !== undefined) updateData.intensityLevel = dto.intensityLevel;
    if (dto.distanceKm !== undefined) updateData.distanceKm = dto.distanceKm;
    if (dto.pace !== undefined) updateData.pace = dto.pace;

    // Recalculate calories if duration or intensity changed
    if (dto.durationMinutes !== undefined || dto.intensityLevel !== undefined) {
      const exercise = await this.exerciseRepo.findById(item.exerciseId);
      if (exercise) {
        const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
        const duration = dto.durationMinutes ?? item.durationMinutes;
        updateData.caloriesBurned = this.caloriesCalcService.calculateForExercise(exercise, duration, {
          intensityLevel: dto.intensityLevel ?? item.intensityLevel ?? undefined,
          weightKg: dto.weightKg ?? item.weightKg ?? undefined,
          userWeightKg,
        });
      }
    }

    await this.sessionRepo.updateItem(itemId, updateData);

    const totals = await this.recalcSessionTotals(sessionId);
    await this.sessionRepo.updateTotals(sessionId, totals.totalDurationMinutes, totals.totalCaloriesBurned);

    const updated = await this.sessionRepo.findById(sessionId);
    return this.formatSession(updated as TrainingSession);
  }

  async removeItem(userId: string, sessionId: string, itemId: string): Promise<void> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');

    const item = await this.sessionRepo.findItemById(itemId);
    if (!item || item.sessionId !== sessionId) throw new NotFoundException('Item not found in this session');

    await this.sessionRepo.deleteItem(itemId);

    const totals = await this.recalcSessionTotals(sessionId);
    await this.sessionRepo.updateTotals(sessionId, totals.totalDurationMinutes, totals.totalCaloriesBurned);
  }

  // Used by dashboard service
  async getSessionHistory(userId: string, limit = 20): Promise<any[]> {
    return this.getSessions(userId, limit);
  }

  async getSessionHistoryRange(userId: string, fromDate: string, toDate: string): Promise<any[]> {
    return this.getSessionsByDateRange(userId, fromDate, toDate);
  }
}
