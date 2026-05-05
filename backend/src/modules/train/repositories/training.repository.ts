import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { Exercise } from '../entities/exercise.entity';
import { WorkoutSession } from '../entities/workout-session.entity';
import { WorkoutSessionDetail } from '../entities/workout-session-detail.entity';
import {
  IExercisesRepository,
  IWorkoutSessionsRepository,
} from './training.repository.interface';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';

@Injectable()
export class ExercisesRepository implements IExercisesRepository {
  constructor(
    @InjectRepository(Exercise)
    private readonly repo: Repository<Exercise>,
  ) {}

  async findAll(query: { name?: string; muscleGroup?: MuscleGroup }): Promise<Exercise[]> {
    const where: any = { isActive: true };
    if (query.name) where.name = Like(`%${query.name}%`);
    if (query.muscleGroup) where.primaryMuscleGroup = query.muscleGroup;

    return this.repo.find({ where });
  }

  async findById(id: string): Promise<Exercise | null> {
    return this.repo.findOne({ where: { id } });
  }

  async updateAvtImage(id: string, imageAvtUrl: string | null, imageAvtPublicId: string | null): Promise<Exercise> {
    await this.repo.update(id, { imageAvtUrl, imageAvtPublicId });
    return this.repo.findOne({ where: { id } }) as Promise<Exercise>;
  }

  async addImageToGallery(id: string, imageUrl: string, imagePublicId: string): Promise<Exercise> {
    const exercise = await this.repo.findOne({ where: { id } });
    if (!exercise) throw new Error('Exercise not found');
    const urls = exercise.imageUrl ?? [];
    const publicIds = exercise.imagePublicIds ?? [];
    await this.repo.update(id, {
      imageUrl: [...urls, imageUrl],
      imagePublicIds: [...publicIds, imagePublicId],
    });
    return this.repo.findOne({ where: { id } }) as Promise<Exercise>;
  }

  async removeImageFromGallery(id: string, imagePublicId: string): Promise<Exercise> {
    const exercise = await this.repo.findOne({ where: { id } });
    if (!exercise) throw new Error('Exercise not found');
    const publicIds = exercise.imagePublicIds ?? [];
    const urls = exercise.imageUrl ?? [];
    const idx = publicIds.indexOf(imagePublicId);
    if (idx !== -1) {
      publicIds.splice(idx, 1);
      urls.splice(idx, 1);
    }
    await this.repo.update(id, { imageUrl: urls, imagePublicIds: publicIds });
    return this.repo.findOne({ where: { id } }) as Promise<Exercise>;
  }
}

@Injectable()
export class WorkoutSessionsRepository implements IWorkoutSessionsRepository {
  constructor(
    @InjectRepository(WorkoutSession)
    private readonly repo: Repository<WorkoutSession>,
    @InjectRepository(WorkoutSessionDetail)
    private readonly detailRepo: Repository<WorkoutSessionDetail>,
  ) {}

  async createSession(data: Partial<WorkoutSession>): Promise<WorkoutSession> {
    const session = this.repo.create(data);
    return this.repo.save(session);
  }

  async findByUser(userId: string, limit: number): Promise<WorkoutSession[]> {
    return this.repo.find({
      where: { userId },
      relations: ['details', 'details.exercise'],
      order: { sessionDate: 'DESC' },
      take: limit,
    });
  }

  async findByDateRange(userId: string, from: string, to: string): Promise<WorkoutSession[]> {
    return this.repo
      .createQueryBuilder('ws')
      .leftJoinAndSelect('ws.details', 'details')
      .leftJoinAndSelect('details.exercise', 'exercise')
      .where('ws.userId = :userId', { userId })
      .andWhere('ws.sessionDate BETWEEN :from AND :to', { from, to })
      .orderBy('ws.sessionDate', 'ASC')
      .addOrderBy('details.orderIndex', 'ASC')
      .getMany();
  }

  async findById(id: string): Promise<WorkoutSession | null> {
    return this.repo.findOne({
      where: { id },
      relations: ['details', 'details.exercise'],
    });
  }

  async updateTotals(id: string, totalDurationMinutes: number, totalCaloriesBurned: number): Promise<void> {
    await this.repo.update(id, { totalDurationMinutes, totalCaloriesBurned });
  }

  async updateSession(id: string, data: Partial<WorkoutSession>): Promise<WorkoutSession> {
    await this.repo.update(id, data);
    return this.repo.findOne({
      where: { id },
      relations: ['details', 'details.exercise'],
    }) as Promise<WorkoutSession>;
  }

  async deleteSession(id: string): Promise<void> {
    await this.repo.delete(id);
  }

  async sumCaloriesForDate(userId: string, date: string): Promise<number> {
    const result = await this.repo
      .createQueryBuilder('ws')
      .select('COALESCE(SUM(ws.totalCaloriesBurned), 0)', 'total')
      .where('ws.userId = :userId AND ws.sessionDate = :date', { userId, date })
      .getRawOne();
    return Number(result?.total ?? 0);
  }

  async addDetail(data: Partial<WorkoutSessionDetail>): Promise<WorkoutSessionDetail> {
    const detail = this.detailRepo.create(data);
    return this.detailRepo.save(detail);
  }

  async findDetailById(detailId: string): Promise<WorkoutSessionDetail | null> {
    return this.detailRepo.findOne({ where: { id: detailId }, relations: ['exercise'] });
  }

  async deleteDetail(detailId: string): Promise<void> {
    await this.detailRepo.delete(detailId);
  }

  async findDetailsBySession(sessionId: string): Promise<WorkoutSessionDetail[]> {
    return this.detailRepo.find({
      where: { workoutSessionId: sessionId },
      order: { orderIndex: 'ASC' },
    });
  }
}

// Export the concrete TrainingRepository class used in the module providers
export class TrainingRepository extends ExercisesRepository {}
