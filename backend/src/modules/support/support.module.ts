import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { CloudinaryProvider } from './cloudinary/cloudinary.provider';
import { CloudinaryService } from './cloudinary/cloudinary.service';
import { MailerService } from './mailer/mailer.service';
import { RedisService } from './redis/redis.service';

@Module({
  imports: [ConfigModule],
  providers: [CloudinaryProvider, CloudinaryService, MailerService, RedisService],
  exports: [CloudinaryService, MailerService, RedisService],
})
export class SupportModule {}
