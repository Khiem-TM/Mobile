import { Injectable, Logger } from '@nestjs/common';
import { createHash } from 'crypto';
import { UsersService } from '../user/services/users.service';
import { DashboardService } from '../user/services/dashboard.service';

@Injectable()
export class AiContextService {
  private readonly logger = new Logger(AiContextService.name);

  constructor(
    private readonly usersService: UsersService,
    private readonly dashboardService: DashboardService,
  ) {}

  /** Hash userId -> user_ref ổn định (không lộ id thật trong DB/log của AI). */
  static userRef(userId: string): string {
    return (
      'u_' + createHash('sha256').update(userId).digest('hex').slice(0, 24)
    );
  }

  async buildUserContext(userId: string): Promise<Record<string, unknown>> {
    const today = new Date().toISOString().split('T')[0];

    const [profile, dashboard] = await Promise.all([
      this.usersService.getHealthProfile(userId).catch((e) => {
        this.logger.warn(`getHealthProfile failed: ${e?.message}`);
        return null;
      }),
      this.dashboardService.getUserDailyDashboard(userId, today).catch((e) => {
        this.logger.warn(`getUserDailyDashboard failed: ${e?.message}`);
        return null;
      }),
    ]);

    const ctx: Record<string, unknown> = {
      schema_version: '1.0',
      user_ref: AiContextService.userRef(userId),
    };

    if (profile) {
      ctx.profile = {
        age: this.ageFromBirthDate(profile.birthDate),
        gender: profile.gender ?? null,
        height_cm: this.num(profile.heightCm),
        activity_level: profile.activityLevel ?? null,
        diet_type: profile.dietType ?? null,
        allergies: profile.foodAllergies ?? [],
        goal_type: profile.goalType ?? null,
        target_weight_kg: this.num(profile.targetWeightKg),
        daily_calories_goal: this.num(
          profile.dailyCaloriesGoal ?? profile.caloriesGoal,
        ),
        macro_goal_g: {
          protein: this.num(profile.proteinGoalG),
          carbs: this.num(profile.carbsGoalG),
          fat: this.num(profile.fatGoalG),
        },
        weekly_rate_kg: this.num(profile.weeklyRateKg),
        water_goal_ml: profile.waterGoalMl ?? null,
        step_goal: profile.stepGoal ?? null,
      };
    }

    if (dashboard) {
      const d = dashboard as any;
      ctx.today = {
        date: d.date,
        calories_in: this.round(d.nutrition?.total_calories),
        macros_g: {
          protein: this.round(d.nutrition?.total_protein),
          carbs: this.round(d.nutrition?.total_carbs),
          fat: this.round(d.nutrition?.total_fat),
        },
        water_ml: d.activity?.water_ml ?? 0,
        steps: d.activity?.steps ?? 0,
        sleep_h: d.activity?.sleep_hours ?? null,
        mood: d.activity?.mood ?? null,
      };
      ctx.latest_body = {
        weight_kg: d.body?.current_weight ?? null,
        bmi: d.body?.bmi ?? null,
      };
    }

    return ctx;
  }

  private ageFromBirthDate(birthDate?: string | null): number | null {
    if (!birthDate) return null;
    const dob = new Date(birthDate);
    if (Number.isNaN(dob.getTime())) return null;
    const diff = Date.now() - dob.getTime();
    return Math.floor(diff / (365.25 * 24 * 3600 * 1000));
  }

  private num(v: unknown): number | null {
    if (v === null || v === undefined) return null;
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  }

  private round(v: unknown): number {
    const n = Number(v);
    return Number.isFinite(n) ? Math.round(n) : 0;
  }
}
