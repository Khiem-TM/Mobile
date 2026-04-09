import { Module } from '@nestjs/common';
import { DashboardService } from './dashboard.service';
import { DashboardController } from './dashboard.controller';
import { MealLogsModule } from '../meal-logs/meal-logs.module';
import { ActivityLogsModule } from '../activity-logs/activity-logs.module';
import { BodyMetricsModule } from '../body-metrics/body-metrics.module';
import { StreaksModule } from '../streaks/streaks.module';
import { TrainingModule } from '../training/training.module';

@Module({
  imports: [
    MealLogsModule,
    ActivityLogsModule,
    BodyMetricsModule,
    StreaksModule,
    TrainingModule,
  ],
  controllers: [DashboardController],
  providers: [DashboardService],
})
export class DashboardModule {}
