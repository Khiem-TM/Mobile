import { BodyProgressPhoto } from '../entities/body-progress-photo.entity';

export interface IBodyProgressPhotosRepository {
  savePhoto(data: Partial<BodyProgressPhoto>): Promise<BodyProgressPhoto>;
  findPhotosByUser(userId: string, limit: number): Promise<BodyProgressPhoto[]>;
  findPhotoById(id: string): Promise<BodyProgressPhoto | null>;
  deletePhoto(id: string): Promise<void>;
}
