import {
  Injectable,
  Inject,
  InternalServerErrorException,
} from '@nestjs/common';
import {
  v2 as cloudinaryV2,
  UploadApiResponse,
  UploadApiErrorResponse,
} from 'cloudinary';
import { Readable } from 'stream';
import { CLOUDINARY } from './cloudinary.provider';

export interface CloudinaryUploadResult {
  url: string;
  publicId: string;
}

const UPLOAD_OPTIONS = {
  quality: 'auto',
  fetch_format: 'auto',
  width: 1024,
  crop: 'limit',
};

@Injectable()
export class CloudinaryService {
  constructor(
    @Inject(CLOUDINARY)
    private readonly cloudinary: typeof cloudinaryV2,
  ) {}

  async uploadFile(
    file: Express.Multer.File,
    folder: string,
  ): Promise<CloudinaryUploadResult> {
    return new Promise((resolve, reject) => {
      const uploadStream = this.cloudinary.uploader.upload_stream(
        { folder, ...UPLOAD_OPTIONS },
        (
          error: UploadApiErrorResponse | undefined,
          result: UploadApiResponse | undefined,
        ) => {
          if (error || !result) {
            return reject(
              new InternalServerErrorException(
                error?.message ?? 'Cloudinary upload failed',
              ),
            );
          }
          resolve({ url: result.secure_url, publicId: result.public_id });
        },
      );

      const readable = new Readable();
      readable.push(file.buffer);
      readable.push(null);
      readable.pipe(uploadStream);
    });
  }

  async uploadBase64(
    base64String: string,
    folder: string,
  ): Promise<CloudinaryUploadResult> {
    const dataUri = base64String.startsWith('data:')
      ? base64String
      : `data:image/jpeg;base64,${base64String}`;

    const result = await this.cloudinary.uploader.upload(dataUri, {
      folder,
      ...UPLOAD_OPTIONS,
    });

    return { url: result.secure_url, publicId: result.public_id };
  }

  async deleteFile(publicId: string): Promise<void> {
    await this.cloudinary.uploader.destroy(publicId);
  }
}
