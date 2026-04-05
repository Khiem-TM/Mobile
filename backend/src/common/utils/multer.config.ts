import { BadRequestException } from '@nestjs/common';
import { memoryStorage } from 'multer';

const ALLOWED_IMAGE_TYPES = /^image\/(jpeg|jpg|png|webp)$/;
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

export function buildMulterOptions(_subfolder?: string) {
  return {
    storage: memoryStorage(),
    fileFilter: (_req: any, file: Express.Multer.File, cb: any) => {
      if (!ALLOWED_IMAGE_TYPES.test(file.mimetype)) {
        return cb(
          new BadRequestException('Only JPEG, PNG and WebP images are allowed'),
          false,
        );
      }
      cb(null, true);
    },
    limits: { fileSize: MAX_FILE_SIZE },
  };
}
