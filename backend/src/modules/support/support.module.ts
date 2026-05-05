import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { CloudinaryProvider } from './cloudinary/cloudinary.provider';
import { CloudinaryService } from './cloudinary/cloudinary.service';
import { MailerService } from './mailer/mailer.service';

@Module({
  imports: [ConfigModule],
  providers: [CloudinaryProvider, CloudinaryService, MailerService],
  exports: [CloudinaryService, MailerService],
})
export class SupportModule {}
