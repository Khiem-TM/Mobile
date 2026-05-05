import { Injectable, NotFoundException, Inject, forwardRef } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../entities/user.entity';
import { UserHealthProfile } from '../entities/user-health-profile.entity';
import { RefreshToken } from '../entities/refresh-token.entity';
import { BodyMetricsService } from '../../train/services/body-metrics.service';

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
      if (latestMetric?.tdee) {
        const tdee = Number(latestMetric.tdee);
        let calories = tdee;
        if (data.goalType === 'lose_weight') calories = tdee - 500;
        else if (data.goalType === 'gain_muscle') calories = tdee + 300;

        merged.dailyCaloriesGoal = Number(calories.toFixed(2));
        merged.proteinGoalG = Number(((calories * 0.3) / 4).toFixed(2));
        merged.fatGoalG = Number(((calories * 0.3) / 9).toFixed(2));
        merged.carbsGoalG = Number(((calories * 0.4) / 4).toFixed(2));
      }
    }

    if (profile) {
      Object.assign(profile, merged);
      return this.healthProfileRepository.save(profile);
    }
    const newProfile = this.healthProfileRepository.create({
      user: { id: userId },
      ...merged,
    });
    return this.healthProfileRepository.save(newProfile);
  }
}
