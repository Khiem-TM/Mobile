import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { BodyProgressPhoto } from '../entities/body-progress-photo.entity';
import { IBodyProgressPhotosRepository } from './body-progress-photos.repository.interface';

@Injectable()
export class BodyProgressPhotosRepository implements IBodyProgressPhotosRepository {
  constructor(
    @InjectRepository(BodyProgressPhoto)
    private readonly photoRepo: Repository<BodyProgressPhoto>,
  ) {}

  async savePhoto(data: Partial<BodyProgressPhoto>): Promise<BodyProgressPhoto> {
    const photo = this.photoRepo.create(data);
    return this.photoRepo.save(photo);
  }

  async findPhotosByUser(userId: string, limit: number): Promise<BodyProgressPhoto[]> {
    return this.photoRepo.find({
      where: { userId },
      order: { takenAt: 'DESC' },
      take: limit,
    });
  }

  async findPhotoById(id: string): Promise<BodyProgressPhoto | null> {
    return this.photoRepo.findOne({ where: { id } });
  }

  async deletePhoto(id: string): Promise<void> {
    await this.photoRepo.delete(id);
  }
}
