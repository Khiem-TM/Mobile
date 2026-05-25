import { Injectable, Inject, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { EXERCISES_REPOSITORY } from '../train.constants';
import type { IExercisesRepository } from '../repositories/exercises.repository.interface';
import { FavoriteExercise } from '../entities/favorite-exercise.entity';
import { Exercise } from '../entities/exercise.entity';

@Injectable()
export class FavoriteExercisesService {
  constructor(
    @Inject(EXERCISES_REPOSITORY)
    private readonly exerciseRepo: IExercisesRepository,
    @InjectRepository(FavoriteExercise)
    private readonly favoriteRepo: Repository<FavoriteExercise>,
    private readonly dataSource: DataSource,
  ) {}

  async getFavorites(userId: string) {
    const favs = await this.favoriteRepo.find({
      where: { userId },
      relations: ['exercise'],
      order: { createdAt: 'DESC' },
    });
    return favs.map((f) => ({ ...f.exercise, isFavorite: true }));
  }

  async checkIsFavorite(userId: string, exerciseId: string): Promise<{ isFavorite: boolean }> {
    const fav = await this.favoriteRepo.findOne({ where: { userId, exerciseId } });
    return { isFavorite: !!fav };
  }

  async addFavorite(userId: string, exerciseId: string): Promise<void> {
    const exercise = await this.exerciseRepo.findById(exerciseId);
    if (!exercise) throw new NotFoundException('Exercise not found');

    const existing = await this.favoriteRepo.findOne({ where: { userId, exerciseId } });
    if (existing) throw new ConflictException('Exercise is already in favorites');

    await this.dataSource.transaction(async (manager) => {
      await manager.save(FavoriteExercise, { userId, exerciseId });
      await manager.increment(Exercise, { id: exerciseId }, 'favoritesCount', 1);
    });
  }

  async removeFavorite(userId: string, exerciseId: string): Promise<void> {
    const existing = await this.favoriteRepo.findOne({ where: { userId, exerciseId } });
    if (!existing) throw new NotFoundException('Exercise is not in favorites');

    await this.dataSource.transaction(async (manager) => {
      await manager.delete(FavoriteExercise, { userId, exerciseId });
      await manager.decrement(Exercise, { id: exerciseId }, 'favoritesCount', 1);
    });
  }
}
