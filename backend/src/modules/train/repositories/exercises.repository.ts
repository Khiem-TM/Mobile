import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like, In } from 'typeorm';
import { Exercise } from '../entities/exercise.entity';
import { IExercisesRepository, ExerciseQuery } from './exercises.repository.interface';

@Injectable()
export class ExercisesRepository implements IExercisesRepository {
  constructor(
    @InjectRepository(Exercise)
    private readonly repo: Repository<Exercise>,
  ) {}

  async findAll(query: ExerciseQuery): Promise<Exercise[]> {
    const where: any = { isActive: true };
    if (query.name) where.name = Like(`%${query.name}%`);
    if (query.exerciseType) where.exerciseType = query.exerciseType;
    if (query.category) where.category = query.category;
    if (query.muscleGroup) where.muscleGroup = query.muscleGroup;
    if (query.difficultyLevel) where.difficultyLevel = query.difficultyLevel;

    const limit = query.limit ?? 50;
    const skip = query.page ? (query.page - 1) * limit : 0;

    return this.repo.find({ where, take: limit, skip, order: { favoritesCount: 'DESC', name: 'ASC' } });
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
