import { Injectable, Inject, NotFoundException } from '@nestjs/common';
import { EXERCISES_REPOSITORY } from '../train.constants';
import type { IExercisesRepository } from '../repositories/exercises.repository.interface';
import { ExerciseQueryDto } from '../dto/exercise-query.dto';
import { CloudinaryService } from '../../support/cloudinary/cloudinary.service';
import { FavoriteExercise } from '../entities/favorite-exercise.entity';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { toExerciseMobileDto } from '../mappers/exercise-mobile.mapper';

@Injectable()
export class ExercisesService {
  constructor(
    @Inject(EXERCISES_REPOSITORY)
    private readonly exerciseRepo: IExercisesRepository,
    @InjectRepository(FavoriteExercise)
    private readonly favoriteRepo: Repository<FavoriteExercise>,
    private readonly cloudinaryService: CloudinaryService,
  ) {}

  async getExercises(query: ExerciseQueryDto, userId?: string) {
    const exercises = await this.exerciseRepo.findAll(query);
    if (!userId) return exercises.map((e) => toExerciseMobileDto(e));

    const favs = await this.favoriteRepo.find({ where: { userId } });
    const favSet = new Set(favs.map((f) => f.exerciseId));
    return exercises.map((e) => toExerciseMobileDto(e, favSet.has(e.id)));
  }

  async getExerciseById(id: string, userId?: string) {
    const exercise = await this.exerciseRepo.findById(id);
    if (!exercise || !exercise.isActive) throw new NotFoundException('Exercise not found');
    if (!userId) return toExerciseMobileDto(exercise);

    const fav = await this.favoriteRepo.findOne({ where: { userId, exerciseId: id } });
    return toExerciseMobileDto(exercise, !!fav);
  }

  async getPopularExercises(limit = 10, userId?: string) {
    const exercises = await this.exerciseRepo.findPopular(limit);
    if (!userId) return exercises.map((e) => toExerciseMobileDto(e));

    const favs = await this.favoriteRepo.find({ where: { userId } });
    const favSet = new Set(favs.map((f) => f.exerciseId));
    return exercises.map((e) => toExerciseMobileDto(e, favSet.has(e.id)));
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
}
