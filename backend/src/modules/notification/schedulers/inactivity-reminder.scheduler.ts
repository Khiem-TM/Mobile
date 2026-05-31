import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { MoreThanOrEqual, Repository } from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { MealLog } from '../../food/entities/meal-log.entity';
import { ActivityLog } from '../../train/entities/activity-log.entity';
import { TrainingSession } from '../../train/entities/training-session.entity';
import { NotificationType } from '../../user/entities/notification.entity';
import { NotificationDispatchService } from '../services/notification-dispatch.service';

/**
 * Nhắc nhở user 2 ngày không cập nhật bất kỳ Meal/Activity/Training nào.
 * Chạy 19:00 hằng ngày. Chỉ nhắm user còn hoạt động (đăng nhập trong 7 ngày).
 */
@Injectable()
export class InactivityReminderScheduler {
  private readonly logger = new Logger(InactivityReminderScheduler.name);

  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(MealLog)
    private readonly mealRepo: Repository<MealLog>,
    @InjectRepository(ActivityLog)
    private readonly activityRepo: Repository<ActivityLog>,
    @InjectRepository(TrainingSession)
    private readonly trainingRepo: Repository<TrainingSession>,
    private readonly dispatch: NotificationDispatchService,
  ) {}

  @Cron('0 19 * * *')
  async sendInactivityReminders(): Promise<void> {
    // cutoff = 2 ngày trước (gồm hôm nay) -> coi như "không cập nhật 2 ngày"
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - 1);
    const cutoff = cutoffDate.toISOString().split('T')[0];

    const activeSince = new Date();
    activeSince.setDate(activeSince.getDate() - 7);

    const [activeUsers, mealUsers, activityUsers, trainingUsers] =
      await Promise.all([
        this.userRepo.find({
          where: { is_active: true, updated_at: MoreThanOrEqual(activeSince) },
          select: ['id'],
        }),
        this.distinctUsersSince(this.mealRepo, 'm', 'm.user_id', 'm.log_date', cutoff),
        this.distinctUsersSince(this.activityRepo, 'a', 'a.userId', 'a.logDate', cutoff),
        this.distinctUsersSince(this.trainingRepo, 't', 't.userId', 't.sessionDate', cutoff),
      ]);

    const logged = new Set<string>([
      ...mealUsers,
      ...activityUsers,
      ...trainingUsers,
    ]);
    const targets = activeUsers
      .map((u) => u.id)
      .filter((id) => !logged.has(id));

    if (!targets.length) {
      this.logger.log('Không có user nào cần nhắc nhở hôm nay.');
      return;
    }

    for (const userId of targets) {
      await this.dispatch.dispatch(
        userId,
        NotificationType.REMINDER,
        'Đừng quên cập nhật hôm nay 💪',
        'Bạn chưa ghi nhật ký ăn uống, hoạt động hay buổi tập gần đây. Cập nhật để VitalAI theo dõi mục tiêu chính xác hơn nhé!',
        { route: 'home' },
        { oncePerDay: true },
      );
    }
    this.logger.log(`Đã gửi ${targets.length} nhắc nhở "2 ngày không hoạt động".`);
  }

  private async distinctUsersSince<T extends object>(
    repo: Repository<T>,
    alias: string,
    userField: string,
    dateField: string,
    cutoff: string,
  ): Promise<string[]> {
    const rows = await repo
      .createQueryBuilder(alias)
      .select(`DISTINCT ${userField}`, 'userId')
      .where(`${dateField} >= :cutoff`, { cutoff })
      .getRawMany<{ userId: string }>();
    return rows.map((r) => r.userId);
  }
}
