import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { StreaksService } from './streaks.service';

@Injectable()
export class StreaksScheduler {
  private readonly logger = new Logger(StreaksScheduler.name);

  constructor(private readonly streaksService: StreaksService) {}

  @Cron(CronExpression.EVERY_DAY_AT_MIDNIGHT)
  async handleNightlyStreakReset() {
    this.logger.log('Executing nightly streak reset scheduler...');
    await this.streaksService.resetExpiredStreaks();
  }
}
