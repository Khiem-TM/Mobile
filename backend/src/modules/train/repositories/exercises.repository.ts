import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { Exercise } from '../entities/exercise.entity';
import { IExercisesRepository, ExerciseQuery } from './exercises.repository.interface';

const MUSCLE_GROUP_ALIASES: Record<string, string[]> = {
  CHEST: ['chest', 'pecs'],
  BACK: ['back', 'lats', 'traps', 'lower back'],
  LEGS: ['legs', 'quads', 'hamstrings', 'glutes', 'calves'],
  SHOULDERS: ['shoulders', 'deltoids'],
  ARMS: ['arms', 'biceps', 'triceps', 'forearms'],
  ABS: ['abs', 'core'],
  FULL_BODY: ['full body', 'full_body', 'total body', 'body'],
  CARDIO: ['cardio', 'cardiorespiratory', 'running', 'cycling'],
};

function normalizeFilterValue(value: string): string {
  return value.trim().replace(/_/g, ' ').toLowerCase();
}

function muscleAliases(value: string): string[] {
  const key = value.trim().toUpperCase().replace(/\s+/g, '_');
  return MUSCLE_GROUP_ALIASES[key] ?? [normalizeFilterValue(value)];
}

@Injectable()
export class ExercisesRepository implements IExercisesRepository {
  constructor(
    @InjectRepository(Exercise)
    private readonly repo: Repository<Exercise>,
  ) {}

  async findAll(query: ExerciseQuery): Promise<Exercise[]> {
    const limit = query.limit ?? 50;
    const skip = query.page ? (query.page - 1) * limit : 0;
    const exerciseType = query.exerciseType ?? query.type;

    const qb = this.repo
      .createQueryBuilder('exercise')
      .where('exercise.is_active = :isActive', { isActive: true });

    if (query.name) {
      qb.andWhere('LOWER(exercise.name) LIKE :name', {
        name: `%${query.name.trim().toLowerCase()}%`,
      });
    }
    if (exerciseType) {
      qb.andWhere('exercise.exercise_type = :exerciseType', { exerciseType });
    }
    if (query.category) {
      qb.andWhere('LOWER(exercise.category) = :category', {
        category: normalizeFilterValue(query.category),
      });
    }
    if (query.muscleGroup) {
      qb.andWhere(
        `(
          LOWER(exercise.muscle_group) IN (:...muscleGroups)
          OR LOWER(exercise.target_muscle_group) IN (:...muscleGroups)
          OR LOWER(exercise.category) IN (:...muscleGroups)
        )`,
        { muscleGroups: muscleAliases(query.muscleGroup) },
      );
    }
    if (query.difficultyLevel) {
      qb.andWhere('exercise.difficulty_level = :difficultyLevel', {
        difficultyLevel: query.difficultyLevel,
      });
    }

    return qb
      .orderBy('exercise.favorites_count', 'DESC')
      .addOrderBy('exercise.name', 'ASC')
      .take(limit)
      .skip(skip)
      .getMany();
  }

  async findById(id: string): Promise<Exercise | null> {
    return this.repo.findOne({ where: { id } });
  }

  async findByIds(ids: string[]): Promise<Exercise[]> {
    if (!ids.length) return [];
    return this.repo.find({ where: { id: In(ids) } });
  }

  async findPopular(limit: number): Promise<Exercise[]> {
    return this.repo.find({
      where: { isActive: true },
      order: { favoritesCount: 'DESC' },
      take: limit,
    });
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
