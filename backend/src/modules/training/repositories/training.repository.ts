import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { Exercise } from '../entities/exercise.entity';
import { WorkoutSession } from '../entities/workout-session.entity';
import { TrainingGoal } from '../entities/training-goal.entity';
import {
  IExercisesRepository,
  IWorkoutSessionsRepository,
  ITrainingGoalsRepository,
} from './training.repository.interface';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';

@Injectable()
export class ExercisesRepository implements IExercisesRepository {
  constructor(
    @InjectRepository(Exercise)
    private readonly repo: Repository<Exercise>,
  ) {}

  async findAll(query: { name?: string; muscleGroup?: MuscleGroup }): Promise<Exercise[]> {
    const where: any = {};
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
  ) {}

  async save(session: Partial<WorkoutSession>): Promise<WorkoutSession> {
    const newSession = this.repo.create(session);
    return this.repo.save(newSession);
  }

  async findByUser(userId: string, limit: number): Promise<WorkoutSession[]> {
    return this.repo.find({
      where: { userId },
      relations: ['exercise'],
      order: { sessionDate: 'DESC' },
      take: limit,
    });
  }

  async findByDateRange(userId: string, from: string, to: string): Promise<WorkoutSession[]> {
    return this.repo
      .createQueryBuilder('ws')
      .leftJoinAndSelect('ws.exercise', 'exercise')
      .where('ws.userId = :userId', { userId })
      .andWhere('ws.sessionDate BETWEEN :from AND :to', { from, to })
      .orderBy('ws.sessionDate', 'ASC')
      .getMany();
  }

  async findById(id: string): Promise<WorkoutSession | null> {
    return this.repo.findOne({ where: { id }, relations: ['exercise'] });
  }

  async update(id: string, data: Partial<WorkoutSession>): Promise<WorkoutSession> {
    await this.repo.update(id, data);
    return this.repo.findOne({ where: { id }, relations: ['exercise'] }) as Promise<WorkoutSession>;
  }

  async delete(id: string): Promise<void> {
    await this.repo.delete(id);
  }
}

@Injectable()
export class TrainingGoalsRepository implements ITrainingGoalsRepository {
  constructor(
    @InjectRepository(TrainingGoal)
    private readonly repo: Repository<TrainingGoal>,
  ) {}

  async save(goal: Partial<TrainingGoal>): Promise<TrainingGoal> {
    const newGoal = this.repo.create(goal);
    return this.repo.save(newGoal);
  }

  async findByUser(userId: string): Promise<TrainingGoal[]> {
    return this.repo.find({
      where: { userId },
      order: { deadline: 'ASC' },
    });
  }

  async findById(id: string): Promise<TrainingGoal | null> {
    return this.repo.findOne({ where: { id } });
  }

  async updateProgress(goalId: string, progress: number): Promise<void> {
    await this.repo.update(goalId, { currentValue: progress });
  }

  async update(id: string, data: Partial<TrainingGoal>): Promise<TrainingGoal> {
    await this.repo.update(id, data);
    return this.repo.findOne({ where: { id } }) as Promise<TrainingGoal>;
  }

  async delete(id: string): Promise<void> {
    await this.repo.delete(id);
  }
}
