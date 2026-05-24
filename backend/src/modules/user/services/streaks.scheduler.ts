import { Inject, Injectable, Logger, forwardRef } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { MoreThanOrEqual, Repository } from 'typeorm';
import { StreaksService } from './streaks.service';
import { User } from '../entities/user.entity';
import { MealLogsService } from '../../food/services/meal-logs.service';
import { NotificationsService } from './notifications.service';
import { NotificationType } from '../entities/notification.entity';

@Injectable()
export class StreaksScheduler {
  private readonly logger = new Logger(StreaksScheduler.name);

  constructor(
    private readonly streaksService: StreaksService,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @Inject(forwardRef(() => MealLogsService))
    private readonly mealLogsService: MealLogsService,
    private readonly notificationsService: NotificationsService,
  ) {}

  @Cron(CronExpression.EVERY_DAY_AT_MIDNIGHT)
  async handleNightlyStreakReset() {
    this.logger.log('Executing nightly streak reset scheduler...');
    await this.streaksService.resetExpiredStreaks();
  }

  @Cron('0 20 * * *')
  async sendDailyMealReminders(): Promise<void> {
    const today = new Date().toISOString().split('T')[0];
    const activeSince = new Date();
    activeSince.setDate(activeSince.getDate() - 7);

    const [activeUsers, usersWithMealLogs] = await Promise.all([
      this.userRepository.find({
        where: {
          is_active: true,
          updated_at: MoreThanOrEqual(activeSince),
        },
        select: ['id'],
      }),
      this.mealLogsService.getUserIdsWithLogsOnDate(today),
    ]);

    const loggedSet = new Set(usersWithMealLogs);
    const targetUserIds = activeUsers
      .map((user) => user.id)
      .filter((userId) => !loggedSet.has(userId));

    if (!targetUserIds.length) return;

    await this.notificationsService.createMany(
      targetUserIds,
      NotificationType.REMINDER,
      'Đừng quên ghi lại bữa ăn hôm nay',
      'Cập nhật nhật ký ăn uống giúp VitalAI theo dõi mục tiêu của bạn chính xác hơn.',
    );
    this.logger.log(`Sent ${targetUserIds.length} daily meal reminders.`);
  }
}
