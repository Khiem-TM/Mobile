import { Injectable, Inject, NotFoundException, ConflictException, BadRequestException, forwardRef } from '@nestjs/common';
import { DataSource } from 'typeorm';
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
import { Exercise } from '../entities/exercise.entity';
import { ExerciseType } from '../enums/exercise-type.enum';
import { toExerciseMobileDto } from '../mappers/exercise-mobile.mapper';

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
    private readonly dataSource: DataSource,
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
        avgSpeedKmh: item.avgSpeedKmh === null ? null : Number(item.avgSpeedKmh),
        exercise: item.exercise ? toExerciseMobileDto(item.exercise) : null,
      })),
    };
  }

  private resolveItemCaloriesAndDuration(
    exercise: Exercise,
    item: CreateSessionItemDto | UpdateTrainingSessionItemDto,
    currentItem: TrainingSessionItem | null,
    userWeightKg: number,
  ): { calories: number; durationMinutes: number } {
    const exerciseType =
      (item as CreateSessionItemDto).exerciseType ?? currentItem?.exerciseType ?? exercise.exerciseType;

    if (exerciseType === ExerciseType.CARDIO) {
      const distanceKm = (item.distanceKm ?? currentItem?.distanceKm) as number | undefined;
      const avgSpeedKmh = (item.avgSpeedKmh ?? currentItem?.avgSpeedKmh) as number | undefined;
      if (!distanceKm || !avgSpeedKmh) {
        throw new BadRequestException('CARDIO items require distanceKm and avgSpeedKmh');
      }
      return this.caloriesCalcService.calculateCardio(exercise, distanceKm, avgSpeedKmh, userWeightKg);
    }

    const durationMinutes =
      item.durationMinutes ??
      (exerciseType === ExerciseType.GYM
        ? this.estimateGymDurationMinutes(exercise, item, currentItem)
        : exercise.defaultDurationMinutes);

    if (!durationMinutes || durationMinutes <= 0) {
      throw new BadRequestException(`${exerciseType} items require durationMinutes`);
    }

    const calories = this.caloriesCalcService.calculateForExercise(exercise, durationMinutes, {
      intensityLevel: item.intensityLevel ?? currentItem?.intensityLevel ?? undefined,
      weightKg: item.weightKg ?? currentItem?.weightKg ?? undefined,
      userWeightKg,
    });
    return { calories, durationMinutes };
  }

  private estimateGymDurationMinutes(
    exercise: Exercise,
    item: CreateSessionItemDto | UpdateTrainingSessionItemDto,
    currentItem: TrainingSessionItem | null,
  ): number {
    const sets = item.sets ?? currentItem?.sets ?? exercise.defaultSets;
    const reps = item.reps ?? currentItem?.reps ?? exercise.defaultReps;
    const restSeconds = item.restTimeSeconds ?? currentItem?.restTimeSeconds ?? exercise.restTimeSeconds ?? 0;

    if (!sets || !reps) {
      throw new BadRequestException('GYM items require sets and reps when durationMinutes is not provided');
    }

    return Math.max(1, Math.ceil((sets * reps * 3 + Math.max(sets - 1, 0) * restSeconds) / 60));
  }

  private async recalcSessionTotals(sessionId: string): Promise<{ totalDurationMinutes: number; totalCaloriesBurned: number }> {
    const items = await this.sessionRepo.findItemsBySession(sessionId);
    const totalDurationMinutes = items.reduce((sum, i) => sum + (i.durationMinutes ?? 0), 0);
    const totalCaloriesBurned = Number(
      items.reduce((sum, i) => sum + Number(i.caloriesBurned ?? 0), 0).toFixed(2),
    );
    return { totalDurationMinutes, totalCaloriesBurned };
  }

  private async refreshWorkoutStreak(userId: string) {
    const dates = await this.sessionRepo.findDistinctSessionDates(userId);
    return this.streaksService.recomputeFromActivityDates(userId, StreakType.WORKOUT, dates);
  }

  async createSession(userId: string, dto: CreateTrainingSessionDto): Promise<any> {
    const existing = await this.sessionRepo.findByUserAndDate(userId, dto.sessionDate);
    if (existing) {
      throw new ConflictException({
        message: `A training session already exists for ${dto.sessionDate}`,
        existingSessionId: existing.id,
      });
    }

    const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
    const exercises = await this.exerciseRepo.findByIds(dto.items.map((i) => i.exerciseId));
    const exerciseMap = new Map(exercises.map((e) => [e.id, e]));

    let totalDurationMinutes = 0;
    let totalCaloriesBurned = 0;
    const itemDataList: Array<Partial<TrainingSessionItem>> = [];

    for (const item of dto.items) {
      const exercise = exerciseMap.get(item.exerciseId);
      if (!exercise) throw new NotFoundException(`Exercise ${item.exerciseId} not found`);

      const { calories, durationMinutes } = this.resolveItemCaloriesAndDuration(
        exercise,
        item,
        null,
        userWeightKg,
      );

      itemDataList.push({
        exerciseId: item.exerciseId,
        exerciseType: item.exerciseType,
        orderIndex: item.orderIndex ?? 0,
        durationMinutes,
        caloriesBurned: calories,
        note: item.note ?? null,
        sets: item.sets ?? (item.exerciseType === ExerciseType.GYM ? exercise.defaultSets : null),
        reps: item.reps ?? (item.exerciseType === ExerciseType.GYM ? exercise.defaultReps : null),
        weightKg: item.weightKg ?? null,
        restTimeSeconds:
          item.restTimeSeconds ?? (item.exerciseType === ExerciseType.GYM ? exercise.restTimeSeconds : null),
        intensityLevel: item.intensityLevel ?? null,
        distanceKm: item.distanceKm ?? null,
        avgSpeedKmh: item.avgSpeedKmh ?? null,
        pace: item.pace ?? null,
      });

      totalDurationMinutes += durationMinutes;
      totalCaloriesBurned += calories;
    }

    const sessionId = await this.dataSource.transaction(async (manager) => {
      const session = manager.create(TrainingSession, {
        userId,
        sessionDate: dto.sessionDate,
        title: dto.title ?? null,
        note: dto.note ?? null,
        totalDurationMinutes,
        totalCaloriesBurned: Number(totalCaloriesBurned.toFixed(2)),
      });
      const saved = await manager.save(session);

      for (const itemData of itemDataList) {
        await manager.save(TrainingSessionItem, { ...itemData, sessionId: saved.id });
      }

      return saved.id;
    });

    await this.refreshWorkoutStreak(userId);

    const result = await this.sessionRepo.findById(sessionId);
    return this.formatSession(result as TrainingSession);
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
    if (dto.sessionDate !== undefined) {
      const existing = await this.sessionRepo.findByUserAndDate(userId, dto.sessionDate);
      if (existing && existing.id !== sessionId) {
        throw new ConflictException({
          message: `A training session already exists for ${dto.sessionDate}`,
          existingSessionId: existing.id,
        });
      }
      updateData.sessionDate = dto.sessionDate;
    }
    if (dto.title !== undefined) updateData.title = dto.title;
    if (dto.note !== undefined) updateData.note = dto.note;

    const updated = await this.sessionRepo.updateSession(sessionId, updateData);
    if (dto.sessionDate !== undefined && dto.sessionDate !== session.sessionDate) {
      await this.refreshWorkoutStreak(userId);
    }
    return this.formatSession(updated);
  }

  async deleteSession(userId: string, sessionId: string): Promise<void> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');
    await this.sessionRepo.deleteSession(sessionId);
    await this.refreshWorkoutStreak(userId);
  }

  async addItem(userId: string, sessionId: string, dto: CreateSessionItemDto): Promise<any> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) throw new NotFoundException('Training session not found');

    const exercise = await this.exerciseRepo.findById(dto.exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');

    const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
    const { calories, durationMinutes } = this.resolveItemCaloriesAndDuration(
      exercise,
      dto,
      null,
      userWeightKg,
    );

    await this.sessionRepo.addItem({
      sessionId,
      exerciseId: dto.exerciseId,
      exerciseType: dto.exerciseType,
      orderIndex: dto.orderIndex ?? 0,
      durationMinutes,
      caloriesBurned: calories,
      note: dto.note ?? null,
      sets: dto.sets ?? (dto.exerciseType === ExerciseType.GYM ? exercise.defaultSets : null),
      reps: dto.reps ?? (dto.exerciseType === ExerciseType.GYM ? exercise.defaultReps : null),
      weightKg: dto.weightKg ?? null,
      restTimeSeconds:
        dto.restTimeSeconds ?? (dto.exerciseType === ExerciseType.GYM ? exercise.restTimeSeconds : null),
      intensityLevel: dto.intensityLevel ?? null,
      distanceKm: dto.distanceKm ?? null,
      avgSpeedKmh: dto.avgSpeedKmh ?? null,
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
    if (dto.note !== undefined) updateData.note = dto.note;
    if (dto.sets !== undefined) updateData.sets = dto.sets;
    if (dto.reps !== undefined) updateData.reps = dto.reps;
    if (dto.weightKg !== undefined) updateData.weightKg = dto.weightKg;
    if (dto.restTimeSeconds !== undefined) updateData.restTimeSeconds = dto.restTimeSeconds;
    if (dto.intensityLevel !== undefined) updateData.intensityLevel = dto.intensityLevel;
    if (dto.distanceKm !== undefined) updateData.distanceKm = dto.distanceKm;
    if (dto.avgSpeedKmh !== undefined) updateData.avgSpeedKmh = dto.avgSpeedKmh;
    if (dto.pace !== undefined) updateData.pace = dto.pace;

    // Recalculate calories + duration whenever any computation-relevant field changes
    const needsRecalc =
      dto.durationMinutes !== undefined ||
      dto.intensityLevel !== undefined ||
      dto.distanceKm !== undefined ||
      dto.avgSpeedKmh !== undefined ||
      dto.weightKg !== undefined ||
      dto.sets !== undefined ||
      dto.reps !== undefined ||
      dto.restTimeSeconds !== undefined;

    if (needsRecalc) {
      const exercise = await this.exerciseRepo.findById(item.exerciseId);
      if (exercise) {
        const userWeightKg = await this.caloriesCalcService.getUserWeight(userId);
        const { calories, durationMinutes } = this.resolveItemCaloriesAndDuration(
          exercise,
          dto,
          item,
          userWeightKg,
        );
        updateData.caloriesBurned = calories;
        updateData.durationMinutes = durationMinutes;
      }
    } else if (dto.durationMinutes !== undefined) {
      updateData.durationMinutes = dto.durationMinutes;
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
