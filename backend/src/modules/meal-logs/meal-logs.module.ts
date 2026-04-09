import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MealLogsService } from './meal-logs.service';
import { MealLogsController } from './meal-logs.controller';
import { MealLog } from './entities/meal-log.entity';
import { MealLogItem } from './entities/meal-log-item.entity';
import { Food } from '../foods/entities/food.entity';
import { MealLogsRepository } from './repositories/meal-logs.repository';
import { MEAL_LOGS_REPOSITORY } from './meal-logs.constants';
import { CloudinaryModule } from '../cloudinary/cloudinary.module';
import { StreaksModule } from '../streaks/streaks.module';

@Module({
  imports: [TypeOrmModule.forFeature([MealLog, MealLogItem, Food]), CloudinaryModule, StreaksModule],
  controllers: [MealLogsController],
  providers: [
    MealLogsService,
    {
      provide: MEAL_LOGS_REPOSITORY,
      useClass: MealLogsRepository,
    },
  ],
  exports: [MealLogsService],
})
export class MealLogsModule {}
