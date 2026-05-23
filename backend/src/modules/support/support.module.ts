import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { CloudinaryProvider } from './cloudinary/cloudinary.provider';
import { CloudinaryService } from './cloudinary/cloudinary.service';
import { MailerService } from './mailer/mailer.service';
import { RedisService } from './redis/redis.service';
import { RagEmbedService } from './rag/rag-embed.service';

@Module({
  imports: [ConfigModule],
  providers: [CloudinaryProvider, CloudinaryService, MailerService, RedisService, RagEmbedService],
  exports: [CloudinaryService, MailerService, RedisService, RagEmbedService],
})
export class SupportModule {}
