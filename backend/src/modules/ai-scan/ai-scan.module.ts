import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AiScanController } from './ai-scan.controller';
import { AiScanService } from './ai-scan.service';
import { AiScanLog } from './entities/ai-scan-log.entity';
import { Food } from '../foods/entities/food.entity';
import { CloudinaryModule } from '../cloudinary/cloudinary.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([AiScanLog, Food]),
    CloudinaryModule,
  ],
  controllers: [AiScanController],
  providers: [AiScanService],
})
export class AiScanModule {}
