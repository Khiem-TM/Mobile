import { Injectable, NotFoundException, Inject, forwardRef } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../entities/user.entity';
import { UserHealthProfile } from '../entities/user-health-profile.entity';
import { RefreshToken } from '../entities/refresh-token.entity';
import { BodyMetricsService } from '../../train/services/body-metrics.service';
import { RagEmbedService } from '../../support/rag/rag-embed.service';
import { TDEEUtil } from '../../../common/utils/tdee.util';
import { NotificationsService } from './notifications.service';
import { NotificationType } from '../entities/notification.entity';
import { OnboardingDto } from '../dto/onboarding.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(UserHealthProfile)
    private readonly healthProfileRepository: Repository<UserHealthProfile>,
    @InjectRepository(RefreshToken)
    private readonly refreshTokenRepository: Repository<RefreshToken>,
    @Inject(forwardRef(() => BodyMetricsService))
    private readonly bodyMetricsService: BodyMetricsService,
    private readonly notificationsService: NotificationsService,
    private readonly ragEmbedService: RagEmbedService,
  ) {}

  async findById(id: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { email } });
  }

  async updateProfile(
    id: string,
    updateData: { display_name?: string; avatar_url?: string },
  ): Promise<User> {
    const user = await this.findById(id);
    if (updateData.display_name) user.display_name = updateData.display_name;
    if (updateData.avatar_url !== undefined)
      user.avatar_url = updateData.avatar_url;
    return this.userRepository.save(user);
  }

  async verifyEmail(id: string): Promise<User> {
    const user = await this.findById(id);
    user.is_verified = true;
    return this.userRepository.save(user);
  }

  async deactivateAccount(id: string): Promise<void> {
    const user = await this.findById(id);
    user.is_active = false;
    await this.userRepository.save(user);
    await this.refreshTokenRepository.delete({ user: { id } });
  }

  async getAllUsers(
    limit = 10,
    offset = 0,
  ): Promise<{ users: User[]; total: number }> {
    const [users, total] = await this.userRepository.findAndCount({
      skip: offset,
      take: limit,
      order: { created_at: 'DESC' },
    });
    return { users, total };
  }

  async getHealthProfile(userId: string): Promise<UserHealthProfile | null> {
    return this.healthProfileRepository.findOne({
      where: { user: { id: userId } },
    });
  }

  async updateHealthProfile(
    userId: string,
    data: Partial<UserHealthProfile>,
  ): Promise<UserHealthProfile> {
    const profile = await this.getHealthProfile(userId);

    const merged: Partial<UserHealthProfile> = profile
      ? { ...profile, ...data }
      : { ...data };

    // Auto-calculate macros when goalType is set and no manual macro values provided
    if (data.goalType && !data.proteinGoalG && !data.fatGoalG && !data.carbsGoalG) {
      const latestMetric = await this.bodyMetricsService.getLatest(userId);
      let tdee: number | null = latestMetric?.tdee ? Number(latestMetric.tdee) : null;

      if (!tdee) {
        const p = merged as Partial<UserHealthProfile>;
        if (
          p.initialWeightKg &&
          p.heightCm &&
          p.birthDate &&
          p.gender &&
          p.activityLevel
        ) {
          const age = Math.floor(
            (Date.now() - new Date(p.birthDate).getTime()) /
              (365.25 * 24 * 3600 * 1000),
          );
          const bmr = TDEEUtil.calculateBMR(
            Number(p.initialWeightKg),
            Number(p.heightCm),
            age,
            p.gender,
          );
          tdee = TDEEUtil.calculateTDEE(bmr, p.activityLevel);
        }
      }

      if (tdee) {
        let calories = tdee;
        if (data.goalType === 'lose_weight') calories = tdee - 500;
        else if (data.goalType === 'gain_weight') calories = tdee + 300;
        else if (data.goalType === 'gain_muscle') calories = tdee + 300;
        else if (data.goalType === 'bulking') calories = tdee + 500;
        else if (data.goalType === 'cutting') calories = tdee - 500;

        merged.dailyCaloriesGoal = Number(calories.toFixed(2));
        merged.caloriesGoal = merged.dailyCaloriesGoal;
        merged.proteinGoalG = Number(((calories * 0.3) / 4).toFixed(2));
        merged.fatGoalG = Number(((calories * 0.3) / 9).toFixed(2));
        merged.carbsGoalG = Number(((calories * 0.4) / 4).toFixed(2));

        if (!data.waterGoalMl && merged.initialWeightKg) {
          merged.waterGoalMl = Math.max(
            2000,
            Math.round(Number(merged.initialWeightKg) * 35),
          );
        }
      }
    }

    let saved: UserHealthProfile;
    if (profile) {
      Object.assign(profile, merged);
      saved = await this.healthProfileRepository.save(profile);
    } else {
      const newProfile = this.healthProfileRepository.create({
        user: { id: userId },
        ...merged,
      });
      saved = await this.healthProfileRepository.save(newProfile);
    }

    this.ragEmbedService.triggerUserEmbed(userId);
    return saved;
  }

  async completeOnboarding(
    userId: string,
    dto: OnboardingDto,
  ): Promise<UserHealthProfile> {
    const profile = await this.updateHealthProfile(userId, {
      birthDate: dto.birthDate,
      gender: dto.gender,
      heightCm: dto.heightCm,
      initialWeightKg: dto.initialWeightKg,
      activityLevel: dto.activityLevel,
      goalType: dto.goalType,
      targetWeightKg: dto.targetWeightKg,
      dietType: dto.dietType,
      foodAllergies: dto.foodAllergies ?? [],
    });

    await this.bodyMetricsService.upsert(userId, {
      recordedAt: new Date().toISOString().split('T')[0],
      weightKg: dto.initialWeightKg,
      notes: 'Initial weight from onboarding',
    });

    if (profile.dailyCaloriesGoal) {
      await this.notificationsService.createOncePerDay(
        userId,
        NotificationType.SYSTEM,
        'Mục tiêu đã được thiết lập! 🎯',
        `Mục tiêu calo hàng ngày của bạn: ${Math.round(
          Number(profile.dailyCaloriesGoal),
        )} kcal`,
      );
    }

    return profile;
  }
}
