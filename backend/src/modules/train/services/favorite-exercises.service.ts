import { Injectable, Inject, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';
import { EXERCISES_REPOSITORY } from '../train.constants';
import type { IExercisesRepository } from '../repositories/exercises.repository.interface';
import { FavoriteExercise } from '../entities/favorite-exercise.entity';
import { Exercise } from '../entities/exercise.entity';
import { toExerciseMobileDto } from '../mappers/exercise-mobile.mapper';

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
    return favs.map((f) => toExerciseMobileDto(f.exercise, true));
  }

  async checkIsFavorite(userId: string, exerciseId: string): Promise<{ isFavorite: boolean }> {
    const fav = await this.favoriteRepo.findOne({ where: { userId, exerciseId } });
    return { isFavorite: !!fav };
  }

  async addFavorite(userId: string, exerciseId: string): Promise<void> {
    await this.dataSource.transaction(async (manager) => {
      const exercise = await manager.findOne(Exercise, { where: { id: exerciseId } });
      if (!exercise) throw new NotFoundException('Exercise not found');

      const result = await manager
        .createQueryBuilder()
        .insert()
        .into(FavoriteExercise)
        .values({ userId, exerciseId })
        .orIgnore()
        .returning(['user_id', 'exercise_id'])
        .execute();

      if (!result.raw?.length) {
        throw new ConflictException('Exercise is already in favorites');
      }

      await manager.increment(Exercise, { id: exerciseId }, 'favoritesCount', 1);
    });
  }

  async removeFavorite(userId: string, exerciseId: string): Promise<void> {
    await this.dataSource.transaction(async (manager) => {
      const result = await manager.delete(FavoriteExercise, { userId, exerciseId });
      if (!result.affected) throw new NotFoundException('Exercise is not in favorites');

      await manager
        .createQueryBuilder()
        .update(Exercise)
        .set({ favoritesCount: () => 'GREATEST(favorites_count - 1, 0)' })
        .where('id = :exerciseId', { exerciseId })
        .execute();
    });
  }
}
