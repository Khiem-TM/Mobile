import { Injectable, Inject, NotFoundException, forwardRef } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, Repository } from 'typeorm';
import {
  TRAINING_EXERCISES_REPOSITORY,
  WORKOUT_SESSIONS_REPOSITORY,
} from '../train.constants';
import type {
  IExercisesRepository,
  IWorkoutSessionsRepository,
} from '../repositories/training.repository.interface';
import {
  ExerciseQueryDto,
  CreateWorkoutSessionDto,
  AddWorkoutDetailDto,
} from '../dto/training.dto';
import { UpdateWorkoutSessionDto } from '../dto/update-training.dto';
import { BodyMetricsService } from './body-metrics.service';
import { ActivityLogsService } from './activity-logs.service';
import { StreaksService } from '../../user/services/streaks.service';
import { UsersService } from '../../user/services/users.service';
import { StreakType } from '../../../common/enums/streak-type.enum';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { ExerciseUserFavorite } from '../entities/exercise-user-favorite.entity';
import { Exercise } from '../entities/exercise.entity';
import { WorkoutSession } from '../entities/workout-session.entity';
import { WorkoutSessionDetail } from '../entities/workout-session-detail.entity';

@Injectable()
export class TrainingService {
  constructor(
    @Inject(TRAINING_EXERCISES_REPOSITORY)
    private readonly exerciseRepo: IExercisesRepository,
    @Inject(WORKOUT_SESSIONS_REPOSITORY)
    private readonly sessionRepo: IWorkoutSessionsRepository,
    @InjectRepository(ExerciseUserFavorite)
    private readonly exerciseFavoriteRepo: Repository<ExerciseUserFavorite>,
    private readonly bodyMetricsService: BodyMetricsService,
    private readonly activityLogsService: ActivityLogsService,
    @Inject(forwardRef(() => StreaksService))
    private readonly streaksService: StreaksService,
    @Inject(forwardRef(() => UsersService))
    private readonly usersService: UsersService,
    private readonly cloudinaryService: CloudinaryService,
    private readonly dataSource: DataSource,
  ) {}

  // ─── Exercises ────────────────────────────────────────────────────────────

  async getExercises(query: ExerciseQueryDto) {
    return this.exerciseRepo.findAll(query);
  }

  async uploadExerciseAvtImage(exerciseId: string, file: Express.Multer.File) {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');
    if (exercise.imageAvtPublicId) {
      await this.cloudinaryService.deleteFile(exercise.imageAvtPublicId);
    }
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'exercises');
    return this.exerciseRepo.updateAvtImage(exerciseId, url, publicId);
  }

  async addExerciseGalleryImage(exerciseId: string, file: Express.Multer.File) {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');
    const { url, publicId } = await this.cloudinaryService.uploadFile(file, 'exercises');
    return this.exerciseRepo.addImageToGallery(exerciseId, url, publicId);
  }

  async removeExerciseGalleryImage(exerciseId: string, imagePublicId: string) {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');
    await this.cloudinaryService.deleteFile(imagePublicId);
    return this.exerciseRepo.removeImageFromGallery(exerciseId, imagePublicId);
  }

  // ─── Workout Sessions ─────────────────────────────────────────────────────

  private async _calcCalories(
    userId: string,
    exerciseId: string,
    durationMinutes: number,
  ): Promise<number> {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');

    let weight = 70;
    const latestMetric = await this.bodyMetricsService.getLatest(userId);
    if (latestMetric?.weightKg) {
      weight = Number(latestMetric.weightKg);
    } else {
      const profile = await this.usersService.getHealthProfile(userId);
      if (profile?.initialWeightKg) weight = Number(profile.initialWeightKg);
    }

    return Number((Number(exercise.metValue) * weight * (durationMinutes / 60)).toFixed(2));
  }

  private async _recalcSessionTotals(
    sessionId: string,
  ): Promise<{ totalDurationMinutes: number; totalCaloriesBurned: number }> {
    const details = await this.sessionRepo.findDetailsBySession(sessionId);
    const totalDurationMinutes = details.reduce((sum, d) => sum + d.durationMinutes, 0);
    const totalCaloriesBurned = Number(
      details.reduce((sum, d) => sum + Number(d.caloriesBurned), 0).toFixed(2),
    );
    return { totalDurationMinutes, totalCaloriesBurned };
  }

  private async _syncActivityLog(userId: string, date: string): Promise<void> {
    const total = await this.sessionRepo.sumCaloriesForDate(userId, date);
    await this.activityLogsService.setWorkoutCalories(userId, date, total);
  }

  async createWorkoutSession(userId: string, dto: CreateWorkoutSessionDto): Promise<WorkoutSession> {
    // Build details with calorie calculations
    const detailData: Array<Partial<WorkoutSessionDetail>> = [];
    let totalDurationMinutes = 0;
    let totalCaloriesBurned = 0;

    for (const d of dto.details) {
      const calories = await this._calcCalories(userId, d.exerciseId, d.durationMinutes);
      detailData.push({
        exerciseId: d.exerciseId,
        durationMinutes: d.durationMinutes,
        weightKg: d.weightKg ?? null,
        sets: d.sets ?? null,
        repsPerSet: d.repsPerSet ?? null,
        orderIndex: d.orderIndex ?? 0,
        notes: d.notes ?? null,
        caloriesBurned: calories,
      });
      totalDurationMinutes += d.durationMinutes;
      totalCaloriesBurned += calories;
    }

    const session = await this.sessionRepo.createSession({
      userId,
      sessionDate: dto.sessionDate,
      sessionName: dto.sessionName ?? null,
      notes: dto.notes ?? null,
      totalDurationMinutes,
      totalCaloriesBurned: Number(totalCaloriesBurned.toFixed(2)),
    });

    for (const detail of detailData) {
      await this.sessionRepo.addDetail({ ...detail, workoutSessionId: session.id });
    }

    await this._syncActivityLog(userId, dto.sessionDate);
    await this.streaksService.updateActivity(userId, StreakType.WORKOUT, dto.sessionDate);

    return this.sessionRepo.findById(session.id) as Promise<WorkoutSession>;
  }

  async addExerciseToSession(
    userId: string,
    sessionId: string,
    dto: AddWorkoutDetailDto,
  ): Promise<WorkoutSession> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) {
      throw new NotFoundException('Workout session not found');
    }

    const calories = await this._calcCalories(userId, dto.exerciseId, dto.durationMinutes);
    await this.sessionRepo.addDetail({
      workoutSessionId: sessionId,
      exerciseId: dto.exerciseId,
      durationMinutes: dto.durationMinutes,
      weightKg: dto.weightKg ?? null,
      sets: dto.sets ?? null,
      repsPerSet: dto.repsPerSet ?? null,
      orderIndex: dto.orderIndex ?? 0,
      notes: dto.notes ?? null,
      caloriesBurned: calories,
    });

    const totals = await this._recalcSessionTotals(sessionId);
    await this.sessionRepo.updateTotals(sessionId, totals.totalDurationMinutes, totals.totalCaloriesBurned);
    await this._syncActivityLog(userId, session.sessionDate);

    return this.sessionRepo.findById(sessionId) as Promise<WorkoutSession>;
  }

  async removeExerciseFromSession(
    userId: string,
    sessionId: string,
    detailId: string,
  ): Promise<void> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) {
      throw new NotFoundException('Workout session not found');
    }
    const detail = await this.sessionRepo.findDetailById(detailId);
    if (!detail || detail.workoutSessionId !== sessionId) {
      throw new NotFoundException('Exercise detail not found');
    }

    await this.sessionRepo.deleteDetail(detailId);

    const totals = await this._recalcSessionTotals(sessionId);
    await this.sessionRepo.updateTotals(sessionId, totals.totalDurationMinutes, totals.totalCaloriesBurned);
    await this._syncActivityLog(userId, session.sessionDate);
  }

  async updateWorkoutSession(
    userId: string,
    sessionId: string,
    dto: UpdateWorkoutSessionDto,
  ): Promise<WorkoutSession> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) {
      throw new NotFoundException('Workout session not found');
    }
    return this.sessionRepo.updateSession(sessionId, {
      sessionName: dto.sessionName,
      notes: dto.notes,
    });
  }

  async deleteWorkoutSession(userId: string, sessionId: string): Promise<void> {
    const session = await this.sessionRepo.findById(sessionId);
    if (!session || session.userId !== userId) {
      throw new NotFoundException('Workout session not found');
    }
    const date = session.sessionDate;
    await this.sessionRepo.deleteSession(sessionId);
    await this._syncActivityLog(userId, date);
  }

  async getWorkoutHistory(userId: string, limit = 20): Promise<WorkoutSession[]> {
    return this.sessionRepo.findByUser(userId, limit);
  }

  async getWorkoutHistoryRange(
    userId: string,
    fromDate?: string,
    toDate?: string,
  ): Promise<WorkoutSession[]> {
    const today = new Date().toISOString().split('T')[0];
    return this.sessionRepo.findByDateRange(userId, fromDate || today, toDate || today);
  }

  async getWorkoutSessionsByDate(userId: string, date: string): Promise<WorkoutSession[]> {
    return this.sessionRepo.findByDateRange(userId, date, date);
  }

  // ─── Exercise Favorites ───────────────────────────────────────────────────

  async getExerciseFavorites(userId: string): Promise<Exercise[]> {
    const favs = await this.exerciseFavoriteRepo.find({
      where: { user_id: userId },
      relations: ['exercise'],
    });
    return favs.map((f) => f.exercise);
  }

  async addExerciseFavorite(userId: string, exerciseId: string): Promise<void> {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');

    const existing = await this.exerciseFavoriteRepo.findOne({
      where: { user_id: userId, exercise_id: exerciseId },
    });
    if (!existing) {
      await this.dataSource.transaction(async (manager) => {
        await manager.save(ExerciseUserFavorite, { user_id: userId, exercise_id: exerciseId });
        await manager.increment(Exercise, { id: exerciseId }, 'favoritesCount', 1);
      });
    }
  }

  async removeExerciseFavorite(userId: string, exerciseId: string): Promise<void> {
    const existing = await this.exerciseFavoriteRepo.findOne({
      where: { user_id: userId, exercise_id: exerciseId },
    });
    if (existing) {
      await this.dataSource.transaction(async (manager) => {
        await manager.delete(ExerciseUserFavorite, { user_id: userId, exercise_id: exerciseId });
        await manager.decrement(Exercise, { id: exerciseId }, 'favoritesCount', 1);
      });
    }
  }
}
