import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Brackets, DataSource, Repository, IsNull } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import axios from 'axios';
import { existsSync } from 'fs';
import { isAbsolute, resolve } from 'path';
import { AuthService } from '../user/services/auth.service';
import { NotificationsService } from '../user/services/notifications.service';
import { NotificationType } from '../user/entities/notification.entity';
import { User } from '../user/entities/user.entity';
import { Food } from '../food/entities/food.entity';
import { Exercise } from '../train/entities/exercise.entity';
import { TrainingSession } from '../train/entities/training-session.entity';
import { Blog } from '../blog/entities/blog.entity';
import { RedisService } from '../support/redis/redis.service';
import { UserRole } from '../../common/enums/user-role.enum';
import { CreateFoodAdminDto } from './dto/create-food-admin.dto';
import { UpdateFoodAdminDto } from './dto/update-food-admin.dto';
import { CreateExerciseAdminDto } from './dto/create-exercise-admin.dto';
import { UpdateExerciseAdminDto } from './dto/update-exercise-admin.dto';
import { CreateUserAdminDto } from './dto/create-user-admin.dto';
import { UpdateUserAdminDto } from './dto/update-user-admin.dto';
import { AdminWarningDto } from './dto/admin-warning.dto';
import { AuthResponseDto } from '../user/dto/auth-response.dto';
import {
  AdminAuditContext,
  AuditLogService,
} from './services/audit-log.service';

type Granularity = 'day' | 'week' | 'month';

const ANALYTICS_TTL_SECONDS = 60;
const MAX_ANALYTICS_DAYS = 90;

// Hardcoded admin credentials. Logging in with this pair always grants admin
// access; the matching user row is auto-provisioned/repaired on first login.
const HARDCODED_ADMIN_EMAIL = 'admin@gmail.com';
const HARDCODED_ADMIN_PASSWORD = 'admin123';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(Food)
    private readonly foodRepo: Repository<Food>,
    @InjectRepository(Exercise)
    private readonly exerciseRepo: Repository<Exercise>,
    @InjectRepository(TrainingSession)
    private readonly trainingSessionRepo: Repository<TrainingSession>,
    @InjectRepository(Blog)
    private readonly blogRepo: Repository<Blog>,
    private readonly dataSource: DataSource,
    private readonly authService: AuthService,
    private readonly configService: ConfigService,
    private readonly notificationsService: NotificationsService,
    private readonly redisService: RedisService,
    private readonly auditLogService: AuditLogService,
  ) {}

  async adminLogin(
    email: string,
    password: string,
    context?: AdminAuditContext,
  ): Promise<AuthResponseDto> {
    const normalizedEmail = email.toLowerCase().trim();
    try {
      // Hardcoded admin shortcut: this exact pair always authenticates and the
      // backing user row is created/repaired on demand so token issuance works.
      const isHardcodedAdmin =
        normalizedEmail === HARDCODED_ADMIN_EMAIL &&
        password === HARDCODED_ADMIN_PASSWORD;

      let admin: User | null;
      if (isHardcodedAdmin) {
        admin = await this.ensureHardcodedAdmin();
      } else {
        admin = await this.userRepo.findOne({
          where: { email: normalizedEmail, role: UserRole.ADMIN },
        });
        if (!admin || !admin.password_hash) {
          throw new UnauthorizedException('Invalid admin credentials');
        }
        if (!admin.is_active) {
          throw new ForbiddenException('Admin account is inactive');
        }
        const isPasswordValid = await bcrypt.compare(
          password,
          admin.password_hash,
        );
        if (!isPasswordValid) {
          throw new UnauthorizedException('Invalid admin credentials');
        }
      }
      if (!admin) {
        throw new UnauthorizedException('Invalid admin credentials');
      }
      const response = await this.authService.generateAuthResponse(admin);
      await this.auditLogService.recordFromContext(context, {
        action: 'admin.login',
        targetType: 'user',
        targetId: admin.id,
        metadata: { email: normalizedEmail },
      });
      return response;
    } catch (error) {
      await this.auditLogService.recordFromContext(context, {
        action: 'admin.login',
        targetType: 'user',
        targetId: normalizedEmail,
        status: 'failure',
        metadata: { email: normalizedEmail },
        errorMessage:
          error instanceof Error ? error.message : 'Admin login failed',
      });
      throw error;
    }
  }

  // Finds the hardcoded admin user, creating it (or promoting/reactivating an
  // existing row with the same email) so token issuance always has a valid row.
  private async ensureHardcodedAdmin(): Promise<User> {
    let admin = await this.userRepo.findOne({
      where: { email: HARDCODED_ADMIN_EMAIL },
    });
    const password_hash = await bcrypt.hash(HARDCODED_ADMIN_PASSWORD, 10);
    if (admin) {
      admin.role = UserRole.ADMIN as User['role'];
      admin.is_active = true;
      admin.is_verified = true;
      admin.password_hash = password_hash;
    } else {
      admin = this.userRepo.create({
        email: HARDCODED_ADMIN_EMAIL,
        password_hash,
        display_name: 'Administrator',
        role: UserRole.ADMIN as User['role'],
        is_active: true,
        is_verified: true,
      });
    }
    return this.userRepo.save(admin);
  }

  async getStats() {
    const [
      totalUsers,
      activeUsers,
      verifiedUsers,
      totalFoods,
      pendingFoods,
      totalBlogs,
      pendingBlogs,
      rejectedBlogs,
      totalExercises,
      activeExercises,
      totalTrainingSessions,
      totalMealLogs,
      totalAiScans,
      totalChatSessions,
    ] = await Promise.all([
      this.userRepo.count(),
      this.userRepo.count({ where: { is_active: true } }),
      this.userRepo.count({ where: { is_verified: true } }),
      this.foodRepo.count({ where: { is_active: true, is_custom: false } }),
      this.foodRepo.count({
        where: { is_verified: false, is_active: true, is_custom: false },
      }),
      this.blogRepo.count({ where: { deletedAt: IsNull() } }),
      this.blogRepo.count({
        where: { status: 'pending', deletedAt: IsNull() },
      }),
      this.blogRepo.count({
        where: { status: 'rejected', deletedAt: IsNull() },
      }),
      this.exerciseRepo.count(),
      this.exerciseRepo.count({ where: { isActive: true } }),
      this.trainingSessionRepo.count(),
      this.scalarCount('meal_logs'),
      this.scalarCount('ai_scan_logs'),
      this.scalarCount('chat_sessions'),
    ]);

    return {
      totalUsers,
      activeUsers,
      inactiveUsers: totalUsers - activeUsers,
      verifiedUsers,
      totalFoods,
      pendingFoods,
      totalBlogs,
      pendingBlogs,
      rejectedBlogs,
      totalExercises,
      activeExercises,
      inactiveExercises: totalExercises - activeExercises,
      totalTrainingSessions,
      totalMealLogs,
      totalAiScans,
      totalChatSessions,
    };
  }

  async getUsers(
    page = 1,
    limit = 20,
    search?: string,
    filters: {
      role?: string;
      isActive?: string;
      isVerified?: string;
      createdFrom?: string;
      createdTo?: string;
    } = {},
  ) {
    const qb = this.userRepo
      .createQueryBuilder('user')
      .orderBy('user.created_at', 'DESC');

    if (search) {
      qb.andWhere(
        new Brackets((where) => {
          where
            .where('LOWER(user.email) LIKE :search', {
              search: `%${search.toLowerCase()}%`,
            })
            .orWhere('LOWER(user.display_name) LIKE :search', {
              search: `%${search.toLowerCase()}%`,
            });
        }),
      );
    }
    if (filters.role) qb.andWhere('user.role = :role', { role: filters.role });
    const isActive = this.parseBoolean(filters.isActive);
    if (isActive !== undefined)
      qb.andWhere('user.is_active = :isActive', { isActive });
    const isVerified = this.parseBoolean(filters.isVerified);
    if (isVerified !== undefined) {
      qb.andWhere('user.is_verified = :isVerified', { isVerified });
    }
    this.applyDateFilters(
      qb,
      'user.created_at',
      filters.createdFrom,
      filters.createdTo,
    );

    const safeLimit = this.clampLimit(limit);
    const [users, total] = await qb
      .skip((page - 1) * safeLimit)
      .take(safeLimit)
      .getManyAndCount();
    return { users, total, page, limit: safeLimit };
  }

  async getUserById(id: string) {
    const user = await this.userRepo.findOne({
      where: { id },
      relations: ['healthProfile'],
    });
    if (!user) throw new NotFoundException('User not found');

    const recentSessions = await this.trainingSessionRepo.find({
      where: { userId: id },
      order: { createdAt: 'DESC' },
      take: 5,
      relations: ['items', 'items.exercise'],
    });

    const [mealLogCount, blogCount, aiScanCount, chatSessionCount] =
      await Promise.all([
        this.countWhere('meal_logs', 'user_id = $1', [id]),
        this.countWhere('blogs', 'author_id = $1 AND deleted_at IS NULL', [id]),
        this.countWhere('ai_scan_logs', 'user_id = $1', [id]),
        this.countWhere('chat_sessions', 'user_id = $1', [id]),
      ]);

    return {
      ...user,
      recentSessions,
      adminSummary: { mealLogCount, blogCount, aiScanCount, chatSessionCount },
    };
  }

  async createUser(
    dto: CreateUserAdminDto,
    context?: AdminAuditContext,
  ): Promise<User> {
    return this.auditMutation(
      context,
      'admin.user.create',
      'user',
      null,
      {
        email: dto.email,
        role: dto.role ?? UserRole.USER,
      },
      async () => {
        const email = dto.email.toLowerCase().trim();
        const existing = await this.userRepo.findOne({ where: { email } });
        if (existing) throw new BadRequestException('Email already registered');
        const user = this.userRepo.create({
          email,
          password_hash: await bcrypt.hash(dto.password, 10),
          display_name: dto.displayName,
          avatar_url: dto.avatarUrl ?? undefined,
          role: (dto.role ?? UserRole.USER) as User['role'],
          is_verified: dto.isVerified ?? true,
          is_active: dto.isActive ?? true,
        });
        return this.userRepo.save(user);
      },
    );
  }

  async updateUser(
    id: string,
    dto: UpdateUserAdminDto,
    context?: AdminAuditContext,
  ): Promise<User> {
    return this.auditMutation(
      context,
      'admin.user.update',
      'user',
      id,
      {
        fields: Object.keys(dto),
      },
      async () => {
        const user = await this.userRepo.findOne({ where: { id } });
        if (!user) throw new NotFoundException('User not found');
        if (dto.email !== undefined)
          user.email = dto.email.toLowerCase().trim();
        if (dto.displayName !== undefined) user.display_name = dto.displayName;
        if (dto.avatarUrl !== undefined) user.avatar_url = dto.avatarUrl as any;
        if (dto.role !== undefined) user.role = dto.role as User['role'];
        if (dto.isVerified !== undefined) user.is_verified = dto.isVerified;
        if (dto.isActive !== undefined) user.is_active = dto.isActive;
        if (dto.password !== undefined) {
          user.password_hash = await bcrypt.hash(dto.password, 10);
        }
        const saved = await this.userRepo.save(user);
        if (dto.isActive === false || dto.password !== undefined) {
          await this.authService.logout(id);
        }
        return saved;
      },
    );
  }

  async banUser(id: string, context?: AdminAuditContext): Promise<User> {
    return this.auditMutation(
      context,
      'admin.user.ban',
      'user',
      id,
      null,
      async () => {
        const user = await this.userRepo.findOne({ where: { id } });
        if (!user) throw new NotFoundException('User not found');
        user.is_active = false;
        await this.userRepo.save(user);
        await this.authService.logout(id);
        await this.notificationsService.create(
          id,
          NotificationType.SYSTEM,
          'Tài khoản bị khóa',
          'Tài khoản của bạn đã bị tạm khóa.',
        );
        return user;
      },
    );
  }

  async unbanUser(id: string, context?: AdminAuditContext): Promise<User> {
    return this.auditMutation(
      context,
      'admin.user.unban',
      'user',
      id,
      null,
      async () => {
        const user = await this.userRepo.findOne({ where: { id } });
        if (!user) throw new NotFoundException('User not found');
        user.is_active = true;
        await this.userRepo.save(user);
        await this.notificationsService.create(
          id,
          NotificationType.SYSTEM,
          'Tài khoản đã mở',
          'Tài khoản của bạn đã được khôi phục.',
        );
        return user;
      },
    );
  }

  async forceVerifyEmail(
    id: string,
    context?: AdminAuditContext,
  ): Promise<User> {
    return this.auditMutation(
      context,
      'admin.user.verify_email',
      'user',
      id,
      null,
      async () => {
        const user = await this.userRepo.findOne({ where: { id } });
        if (!user) throw new NotFoundException('User not found');
        user.is_verified = true;
        return this.userRepo.save(user);
      },
    );
  }

  async warnUser(
    id: string,
    dto: AdminWarningDto,
    context?: AdminAuditContext,
  ) {
    return this.auditMutation(
      context,
      'admin.user.warning',
      'user',
      id,
      {
        reasonCode: dto.reasonCode ?? null,
      },
      async () => {
        const user = await this.userRepo.findOne({ where: { id } });
        if (!user) throw new NotFoundException('User not found');
        const notification = await this.notificationsService.create(
          id,
          NotificationType.WARNING,
          dto.title,
          dto.body,
        );
        return { notificationId: notification.id };
      },
    );
  }

  async getFoods(
    page = 1,
    limit = 20,
    search?: string,
    filters: {
      foodType?: string;
      category?: string;
      isVerified?: string;
      isActive?: string;
      createdFrom?: string;
      createdTo?: string;
    } = {},
  ) {
    const qb = this.foodRepo
      .createQueryBuilder('food')
      .where('food.is_custom = false')
      .orderBy('food.created_at', 'DESC');

    if (search) {
      qb.andWhere(
        new Brackets((where) => {
          where
            .where('LOWER(food.name) LIKE :search', {
              search: `%${search.toLowerCase()}%`,
            })
            .orWhere('LOWER(food.category) LIKE :search', {
              search: `%${search.toLowerCase()}%`,
            })
            .orWhere('LOWER(food.brand) LIKE :search', {
              search: `%${search.toLowerCase()}%`,
            });
        }),
      );
    }
    if (filters.foodType)
      qb.andWhere('food.food_type = :foodType', { foodType: filters.foodType });
    if (filters.category) {
      qb.andWhere('LOWER(food.category) LIKE :category', {
        category: `%${filters.category.toLowerCase()}%`,
      });
    }
    const isVerified = this.parseBoolean(filters.isVerified);
    if (isVerified !== undefined) {
      qb.andWhere('food.is_verified = :isVerified', { isVerified });
    }
    const isActive = this.parseBoolean(filters.isActive);
    if (isActive !== undefined)
      qb.andWhere('food.is_active = :isActive', { isActive });
    this.applyDateFilters(
      qb,
      'food.created_at',
      filters.createdFrom,
      filters.createdTo,
    );

    const safeLimit = this.clampLimit(limit);
    const [foods, total] = await qb
      .skip((page - 1) * safeLimit)
      .take(safeLimit)
      .getManyAndCount();
    return { foods, total, page, limit: safeLimit };
  }

  async createFood(
    dto: CreateFoodAdminDto,
    context?: AdminAuditContext,
  ): Promise<Food> {
    return this.auditMutation(
      context,
      'admin.food.create',
      'food',
      null,
      {
        name: dto.name,
      },
      async () => {
        const food = this.foodRepo.create({
          name: dto.name,
          name_en: dto.nameEn,
          brand: dto.brand,
          category: dto.category,
          food_type: dto.foodType ?? 'ingredient',
          serving_size_g: dto.servingSizeG ?? 100,
          serving_unit: dto.servingUnit ?? 'g',
          description: dto.description,
          calories_per_100g: dto.caloriesPer100g,
          protein_per_100g: dto.proteinPer100g ?? 0,
          fat_per_100g: dto.fatPer100g ?? 0,
          carbs_per_100g: dto.carbsPer100g ?? 0,
          fiber_per_100g: dto.fiberPer100g ?? null,
          image_urls: dto.imageUrls ?? null,
          is_custom: false,
          created_by_user_id: null,
          is_verified: dto.isVerified ?? true,
          is_active: true,
        });
        const savedFood = await this.foodRepo.save(food);
        await this.invalidateFoodCache(savedFood.id);
        const activeUsers = await this.userRepo.find({
          select: ['id'],
          where: { is_active: true },
        });
        if (activeUsers.length) {
          await this.notificationsService.createMany(
            activeUsers.map((u) => u.id),
            NotificationType.SYSTEM,
            'Thực phẩm mới',
            'Admin vừa thêm: ' + dto.name,
          );
        }
        return savedFood;
      },
    );
  }

  async updateFood(
    id: string,
    dto: UpdateFoodAdminDto,
    context?: AdminAuditContext,
  ): Promise<Food> {
    return this.auditMutation(
      context,
      'admin.food.update',
      'food',
      id,
      {
        fields: Object.keys(dto),
      },
      async () => {
        const food = await this.foodRepo.findOne({ where: { id } });
        if (!food) throw new NotFoundException('Food not found');
        this.ensureGlobalFood(food);
        if (dto.name !== undefined) food.name = dto.name;
        if (dto.nameEn !== undefined) food.name_en = dto.nameEn;
        if (dto.brand !== undefined) food.brand = dto.brand;
        if (dto.category !== undefined) food.category = dto.category;
        if (dto.foodType !== undefined) food.food_type = dto.foodType;
        if (dto.servingSizeG !== undefined)
          food.serving_size_g = dto.servingSizeG;
        if (dto.servingUnit !== undefined) food.serving_unit = dto.servingUnit;
        if (dto.description !== undefined) food.description = dto.description;
        if (dto.caloriesPer100g !== undefined)
          food.calories_per_100g = dto.caloriesPer100g;
        if (dto.proteinPer100g !== undefined)
          food.protein_per_100g = dto.proteinPer100g;
        if (dto.fatPer100g !== undefined) food.fat_per_100g = dto.fatPer100g;
        if (dto.carbsPer100g !== undefined)
          food.carbs_per_100g = dto.carbsPer100g;
        if (dto.fiberPer100g !== undefined)
          food.fiber_per_100g = dto.fiberPer100g;
        if (dto.imageUrls !== undefined) food.image_urls = dto.imageUrls;
        if (dto.isVerified !== undefined) food.is_verified = dto.isVerified;
        if (dto.isActive !== undefined) food.is_active = dto.isActive;
        const saved = await this.foodRepo.save(food);
        await this.invalidateFoodCache(id);
        return saved;
      },
    );
  }

  async getPendingFoods(page = 1, limit = 20) {
    const safeLimit = this.clampLimit(limit);
    const [foods, total] = await this.foodRepo.findAndCount({
      where: { is_verified: false, is_active: true, is_custom: false },
      order: { created_at: 'ASC' },
      skip: (page - 1) * safeLimit,
      take: safeLimit,
    });
    return { foods, total, page, limit: safeLimit };
  }

  async verifyFood(
    id: string,
    context?: AdminAuditContext,
  ): Promise<Food> {
    return this.auditMutation(
      context,
      'admin.food.verify',
      'food',
      id,
      null,
      async () => {
        const food = await this.foodRepo.findOne({ where: { id } });
        if (!food) throw new NotFoundException('Food not found');
        this.ensureGlobalFood(food);
        food.is_verified = true;
        food.is_active = true;
        const saved = await this.foodRepo.save(food);
        await this.invalidateFoodCache(id);
        return saved;
      },
    );
  }

  async rejectFood(id: string, context?: AdminAuditContext): Promise<void> {
    await this.auditMutation(
      context,
      'admin.food.reject',
      'food',
      id,
      null,
      async () => {
        const food = await this.foodRepo.findOne({ where: { id } });
        if (!food) throw new NotFoundException('Food not found');
        this.ensureGlobalFood(food);
        food.is_active = false;
        await this.foodRepo.save(food);
        await this.invalidateFoodCache(id);
      },
    );
  }

  async deleteFood(id: string, context?: AdminAuditContext): Promise<void> {
    await this.auditMutation(
      context,
      'admin.food.delete',
      'food',
      id,
      null,
      async () => {
        const food = await this.foodRepo.findOne({ where: { id } });
        if (!food) throw new NotFoundException('Food not found');
        this.ensureGlobalFood(food);
        food.is_active = false;
        await this.foodRepo.save(food);
        await this.invalidateFoodCache(id);
      },
    );
  }

  async getExercises(
    page = 1,
    limit = 20,
    search?: string,
    filters: {
      exerciseType?: string;
      category?: string;
      muscleGroup?: string;
      difficultyLevel?: string;
      isActive?: string;
    } = {},
  ) {
    const qb = this.exerciseRepo
      .createQueryBuilder('exercise')
      .orderBy('exercise.createdAt', 'DESC');

    if (search) {
      qb.andWhere('LOWER(exercise.name) LIKE :search', {
        search: `%${search.toLowerCase()}%`,
      });
    }
    if (filters.exerciseType) {
      qb.andWhere('exercise.exercise_type = :exerciseType', {
        exerciseType: filters.exerciseType,
      });
    }
    if (filters.category) {
      qb.andWhere('LOWER(exercise.category) LIKE :category', {
        category: `%${filters.category.toLowerCase()}%`,
      });
    }
    if (filters.muscleGroup) {
      qb.andWhere(
        '(LOWER(exercise.muscle_group) LIKE :muscleGroup OR LOWER(exercise.target_muscle_group) LIKE :muscleGroup)',
        { muscleGroup: `%${filters.muscleGroup.toLowerCase()}%` },
      );
    }
    if (filters.difficultyLevel) {
      qb.andWhere('exercise.difficulty_level = :difficultyLevel', {
        difficultyLevel: filters.difficultyLevel,
      });
    }
    const isActive = this.parseBoolean(filters.isActive);
    if (isActive !== undefined)
      qb.andWhere('exercise.is_active = :isActive', { isActive });

    const safeLimit = this.clampLimit(limit);
    const [exercises, total] = await qb
      .skip((page - 1) * safeLimit)
      .take(safeLimit)
      .getManyAndCount();
    return { exercises, total, page, limit: safeLimit };
  }

  async createExercise(
    dto: CreateExerciseAdminDto,
    context?: AdminAuditContext,
  ): Promise<Exercise> {
    return this.auditMutation(
      context,
      'admin.exercise.create',
      'exercise',
      null,
      {
        name: dto.name,
      },
      async () => {
        const exercise = this.exerciseRepo.create({
          name: dto.name,
          description: dto.description,
          exerciseType: dto.exerciseType,
          category: dto.category,
          muscleGroup: dto.muscleGroup,
          difficultyLevel: dto.difficultyLevel,
          metValue: dto.metValue ?? 0,
          instructions: dto.instructions,
          videoUrl: dto.videoUrl,
          imageAvtUrl: dto.imageAvtUrl,
          equipment: dto.equipment,
          formTips: dto.formTips,
          secondaryMuscleGroups: dto.secondaryMuscleGroups,
          defaultSets: dto.defaultSets,
          defaultReps: dto.defaultReps,
          defaultWeightKg: dto.defaultWeightKg,
          targetMuscleGroup: dto.targetMuscleGroup,
          restTimeSeconds: dto.restTimeSeconds,
          defaultDurationMinutes: dto.defaultDurationMinutes,
          defaultIntensityLevel: dto.defaultIntensityLevel,
          movementType: dto.movementType,
          estimatedCaloriesPerMinute: dto.estimatedCaloriesPerMinute,
          isActive: dto.isActive ?? true,
        });
        const savedExercise = await this.exerciseRepo.save(exercise);
        const activeUsers = await this.userRepo.find({
          select: ['id'],
          where: { is_active: true },
        });
        if (activeUsers.length) {
          await this.notificationsService.createMany(
            activeUsers.map((u) => u.id),
            NotificationType.SYSTEM,
            'Bài tập mới',
            'Admin vừa thêm: ' + dto.name,
          );
        }
        return savedExercise;
      },
    );
  }

  async updateExercise(
    id: string,
    dto: UpdateExerciseAdminDto,
    context?: AdminAuditContext,
  ): Promise<Exercise> {
    return this.auditMutation(
      context,
      'admin.exercise.update',
      'exercise',
      id,
      {
        fields: Object.keys(dto),
      },
      async () => {
        const exercise = await this.exerciseRepo.findOne({ where: { id } });
        if (!exercise) throw new NotFoundException('Exercise not found');
        Object.assign(exercise, dto);
        return this.exerciseRepo.save(exercise);
      },
    );
  }

  async deleteExercise(id: string, context?: AdminAuditContext): Promise<void> {
    await this.auditMutation(
      context,
      'admin.exercise.delete',
      'exercise',
      id,
      null,
      async () => {
        const exercise = await this.exerciseRepo.findOne({ where: { id } });
        if (!exercise) throw new NotFoundException('Exercise not found');
        exercise.isActive = false;
        await this.exerciseRepo.save(exercise);
      },
    );
  }

  async getAnalyticsOverview(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('overview', range, async () => ({
      range,
      stats: await this.getStats(),
      users: await this.getUserAnalytics(fromDate, toDate, granularity),
      nutrition: await this.getNutritionAnalytics(
        fromDate,
        toDate,
        granularity,
      ),
      training: await this.getTrainingAnalytics(fromDate, toDate, granularity),
      blogs: await this.getBlogAnalytics(fromDate, toDate, granularity),
      ai: await this.getAiAnalytics(fromDate, toDate, granularity),
    }));
  }

  async getUserAnalytics(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('users', range, async () => {
      const bucket = this.bucketExpression('created_at', range.granularity);
      const rows = await this.dataSource.query(
        `SELECT ${bucket} AS bucket, COUNT(*)::int AS signups
         FROM users
         WHERE created_at::date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      return {
        range,
        totals: {
          totalUsers: await this.userRepo.count(),
          activeUsers: await this.userRepo.count({
            where: { is_active: true },
          }),
          adminUsers: await this.userRepo.count({
            where: { role: UserRole.ADMIN },
          }),
        },
        series: rows,
      };
    });
  }

  async getNutritionAnalytics(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('nutrition', range, async () => {
      const bucket = this.bucketExpression('ml.log_date', range.granularity);
      const rows = await this.dataSource.query(
        `SELECT ${bucket} AS bucket,
                COUNT(DISTINCT ml.id)::int AS meal_logs,
                COUNT(DISTINCT ml.user_id)::int AS active_users,
                COALESCE(SUM(mli.calories_snapshot), 0)::float AS total_calories
         FROM meal_logs ml
         LEFT JOIN meal_log_items mli ON mli.meal_log_id = ml.id
         WHERE ml.log_date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      return { range, series: rows };
    });
  }

  async getTrainingAnalytics(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('training', range, async () => {
      const bucket = this.bucketExpression('session_date', range.granularity);
      const rows = await this.dataSource.query(
        `SELECT ${bucket} AS bucket,
                COUNT(*)::int AS sessions,
                COUNT(DISTINCT user_id)::int AS active_users,
                COALESCE(SUM(total_duration_minutes), 0)::int AS total_duration_minutes,
                COALESCE(SUM(total_calories_burned), 0)::float AS total_calories_burned
         FROM training_sessions
         WHERE session_date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      return { range, series: rows };
    });
  }

  async getBlogAnalytics(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('blogs', range, async () => {
      const bucket = this.bucketExpression('created_at', range.granularity);
      const posts = await this.dataSource.query(
        `SELECT ${bucket} AS bucket,
                COUNT(*)::int AS posts,
                COUNT(*) FILTER (WHERE status = 'pending')::int AS pending,
                COUNT(*) FILTER (WHERE status = 'approved')::int AS approved,
                COUNT(*) FILTER (WHERE status = 'rejected')::int AS rejected,
                COALESCE(SUM(view_count), 0)::int AS views,
                COALESCE(SUM(likes_count), 0)::int AS likes,
                COALESCE(SUM(comment_count), 0)::int AS comments
         FROM blogs
         WHERE created_at::date BETWEEN $1::date AND $2::date
           AND deleted_at IS NULL
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      return { range, series: posts };
    });
  }

  async getAiAnalytics(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const range = this.normalizeAnalyticsRange(fromDate, toDate, granularity);
    return this.cachedAnalytics('ai', range, async () => {
      const aiBucket = this.bucketExpression('created_at', range.granularity);
      const scans = await this.dataSource.query(
        `SELECT ${aiBucket} AS bucket,
                COUNT(*)::int AS scans,
                COUNT(DISTINCT user_id)::int AS users
         FROM ai_scan_logs
         WHERE created_at::date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      const chatSessions = await this.dataSource.query(
        `SELECT ${aiBucket} AS bucket,
                COUNT(*)::int AS sessions,
                COUNT(DISTINCT user_id)::int AS users
         FROM chat_sessions
         WHERE created_at::date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      const messages = await this.dataSource.query(
        `SELECT ${aiBucket} AS bucket,
                COUNT(*)::int AS messages,
                COUNT(*) FILTER (WHERE role = 'user')::int AS user_messages,
                COUNT(*) FILTER (WHERE role = 'assistant')::int AS assistant_messages
         FROM chat_messages
         WHERE created_at::date BETWEEN $1::date AND $2::date
         GROUP BY bucket
         ORDER BY bucket ASC`,
        [range.fromDate, range.toDate],
      );
      return { range, scans, chatSessions, messages };
    });
  }

  async getHealth() {
    const db = await this.checkDb();
    const redis = await this.checkRedis();
    const rag = await this.checkRag();
    const firebasePath = this.configService.get<string>(
      'FIREBASE_SERVICE_ACCOUNT_PATH',
    );
    const cloudinaryConfigured = Boolean(
      this.configService.get<string>('CLOUDINARY_CLOUD_NAME') &&
      this.configService.get<string>('CLOUDINARY_API_KEY') &&
      this.configService.get<string>('CLOUDINARY_API_SECRET'),
    );
    const kafkaEnabled =
      (this.configService.get<string>('KAFKA_ENABLED') ?? 'true') !== 'false';
    return {
      status: [db.status, redis.status].every((s) => s === 'ok')
        ? 'ok'
        : 'degraded',
      services: {
        db,
        redis,
        kafka: {
          status: kafkaEnabled ? 'configured' : 'disabled',
          brokers: this.configService.get<string>(
            'KAFKA_BROKERS',
            'localhost:9094',
          ),
        },
        rag,
        firebase: {
          status:
            firebasePath && this.pathExists(firebasePath)
              ? 'configured'
              : 'missing_config',
        },
        cloudinary: {
          status: cloudinaryConfigured ? 'configured' : 'missing_config',
        },
      },
    };
  }

  private async scalarCount(table: string): Promise<number> {
    const rows = await this.dataSource.query(
      `SELECT COUNT(*)::int AS count FROM ${table}`,
    );
    return rows[0]?.count ?? 0;
  }

  private async countWhere(
    whereTable: string,
    whereClause: string,
    params: unknown[],
  ) {
    const rows = await this.dataSource.query(
      `SELECT COUNT(*)::int AS count FROM ${whereTable} WHERE ${whereClause}`,
      params,
    );
    return rows[0]?.count ?? 0;
  }

  private async invalidateFoodCache(foodId?: string): Promise<void> {
    await Promise.all([
      foodId
        ? this.redisService.del(`cache:foods:one:${foodId}`)
        : Promise.resolve(),
      this.redisService.delByPattern('cache:foods:list:*'),
      this.redisService.delByPattern('cache:foods:explore:*'),
      this.redisService.delByPattern('cache:foods:barcode:*'),
    ]);
  }

  private ensureGlobalFood(food: Food): void {
    if (food.is_custom) {
      throw new ForbiddenException('Custom foods are private user data');
    }
  }

  private async auditMutation<T>(
    context: AdminAuditContext | undefined,
    action: string,
    targetType: string,
    targetId: string | null,
    metadata: Record<string, unknown> | null,
    operation: () => Promise<T>,
  ): Promise<T> {
    try {
      const result = await operation();
      await this.auditLogService.recordFromContext(context, {
        action,
        targetType,
        targetId: this.resolveTargetId(targetId, result),
        metadata,
      });
      return result;
    } catch (error) {
      await this.auditLogService.recordFromContext(context, {
        action,
        targetType,
        targetId,
        status: 'failure',
        metadata,
        errorMessage:
          error instanceof Error ? error.message : 'Admin action failed',
      });
      throw error;
    }
  }

  private resolveTargetId(
    targetId: string | null,
    result: unknown,
  ): string | null {
    if (targetId) return targetId;
    if (result && typeof result === 'object' && 'id' in result) {
      const id = (result as { id?: unknown }).id;
      return typeof id === 'string' ? id : null;
    }
    return null;
  }

  private parseBoolean(value?: string): boolean | undefined {
    if (value === undefined) return undefined;
    if (value === 'true') return true;
    if (value === 'false') return false;
    return undefined;
  }

  private clampLimit(limit: number): number {
    return Math.min(Math.max(Number(limit) || 20, 1), 100);
  }

  private applyDateFilters(
    qb: {
      andWhere: (
        condition: string,
        params?: Record<string, unknown>,
      ) => unknown;
    },
    column: string,
    from?: string,
    to?: string,
  ): void {
    if (from) qb.andWhere(`${column} >= :createdFrom`, { createdFrom: from });
    if (to)
      qb.andWhere(`${column} <= :createdTo`, { createdTo: `${to} 23:59:59` });
  }

  private normalizeAnalyticsRange(
    fromDate?: string,
    toDate?: string,
    granularity?: string,
  ) {
    const safeGranularity: Granularity =
      granularity === 'week' || granularity === 'month' ? granularity : 'day';
    const today = new Date();
    const end = toDate ? new Date(`${toDate}T00:00:00.000Z`) : today;
    const start = fromDate
      ? new Date(`${fromDate}T00:00:00.000Z`)
      : new Date(end.getTime() - 29 * 86400_000);
    const maxStart = new Date(
      end.getTime() - (MAX_ANALYTICS_DAYS - 1) * 86400_000,
    );
    const clampedStart = start < maxStart ? maxStart : start;
    if (clampedStart > end) clampedStart.setTime(end.getTime());
    return {
      fromDate: clampedStart.toISOString().slice(0, 10),
      toDate: end.toISOString().slice(0, 10),
      granularity: safeGranularity,
    };
  }

  private bucketExpression(column: string, granularity: Granularity): string {
    return `date_trunc('${granularity}', ${column})::date`;
  }

  private async cachedAnalytics<T>(
    name: string,
    range: { fromDate: string; toDate: string; granularity: Granularity },
    loader: () => Promise<T>,
  ): Promise<T> {
    const key = `cache:admin:analytics:${name}:${range.fromDate}:${range.toDate}:${range.granularity}`;
    const cached = await this.redisService.getJson<T>(key);
    if (cached) return cached;
    const result = await loader();
    await this.redisService.setJson(key, result, ANALYTICS_TTL_SECONDS);
    return result;
  }

  private async checkDb() {
    try {
      await this.dataSource.query('SELECT 1');
      return { status: 'ok' };
    } catch (error) {
      return {
        status: 'error',
        message: error instanceof Error ? error.message : 'DB check failed',
      };
    }
  }

  private async checkRedis() {
    try {
      await this.redisService.getClient().ping();
      return { status: 'ok' };
    } catch (error) {
      return {
        status: 'error',
        message: error instanceof Error ? error.message : 'Redis check failed',
      };
    }
  }

  private async checkRag() {
    const ragUrl = this.configService.get<string>('RAG_SERVICE_URL');
    if (!ragUrl) return { status: 'missing_config' };
    try {
      const res = await axios.get(`${ragUrl}/health`, { timeout: 1500 });
      return {
        status: res.status < 500 ? 'ok' : 'degraded',
        statusCode: res.status,
      };
    } catch (error) {
      return {
        status: 'error',
        message: error instanceof Error ? error.message : 'RAG check failed',
      };
    }
  }

  private pathExists(path: string): boolean {
    const abs = isAbsolute(path) ? path : resolve(process.cwd(), path);
    return existsSync(abs);
  }
}
