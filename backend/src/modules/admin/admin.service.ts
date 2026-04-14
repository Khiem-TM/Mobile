import {
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, ILike } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { User } from '../users/entities/user.entity';
import { Food } from '../foods/entities/food.entity';
import { Exercise } from '../training/entities/exercise.entity';
import { WorkoutSession } from '../training/entities/workout-session.entity';
import { Blog } from './entities/blog.entity';
import { SportTip } from '../training/entities/sport-tip.entity';
import { FoodRecipe } from '../foods/entities/food-recipe.entity';
import { FoodRecipeStep } from '../foods/entities/food-recipe-step.entity';
import { CreateFoodAdminDto } from './dto/create-food-admin.dto';
import { UpdateFoodAdminDto } from './dto/update-food-admin.dto';
import { CreateExerciseAdminDto } from './dto/create-exercise-admin.dto';
import { UpdateExerciseAdminDto } from './dto/update-exercise-admin.dto';
import { CreateBlogDto } from './dto/create-blog.dto';
import { UpdateBlogDto } from './dto/update-blog.dto';
import {
  CreateRecipeDto,
  AddRecipeStepDto,
} from '../foods/dto/create-recipe.dto';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(Food)
    private readonly foodRepo: Repository<Food>,
    @InjectRepository(Exercise)
    private readonly exerciseRepo: Repository<Exercise>,
    @InjectRepository(WorkoutSession)
    private readonly workoutSessionRepo: Repository<WorkoutSession>,
    @InjectRepository(Blog)
    private readonly blogRepo: Repository<Blog>,
    @InjectRepository(SportTip)
    private readonly sportTipRepo: Repository<SportTip>,
    @InjectRepository(FoodRecipe)
    private readonly foodRecipeRepo: Repository<FoodRecipe>,
    @InjectRepository(FoodRecipeStep)
    private readonly foodRecipeStepRepo: Repository<FoodRecipeStep>,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  // ─── Admin Auth ────────────────────────────────────────────────────────────

  async adminLogin(
    email: string,
    password: string,
  ): Promise<{ access_token: string }> {
    const adminEmail = this.configService.get<string>(
      'ADMIN_EMAIL',
      'admin@gmail.com',
    );
    const adminPassword = this.configService.get<string>(
      'ADMIN_PASSWORD',
      'admin',
    );
    if (email !== adminEmail || password !== adminPassword) {
      throw new UnauthorizedException('Invalid admin credentials');
    }
    const access_token = this.jwtService.sign(
      { sub: 'admin', email, role: 'admin' },
      { secret: process.env.JWT_SECRET || 'khiemhehe', expiresIn: '7d' },
    );
    return { access_token };
  }

  // ─── Stats ─────────────────────────────────────────────────────────────────

  async getStats() {
    const [
      totalUsers,
      activeUsers,
      totalFoods,
      pendingFoods,
      totalBlogs,
      totalExercises,
    ] = await Promise.all([
      this.userRepo.count(),
      this.userRepo.count({ where: { is_active: true } }),
      this.foodRepo.count({ where: { is_active: true } }),
      this.foodRepo.count({ where: { is_verified: false, is_active: true } }),
      this.blogRepo.count(),
      this.exerciseRepo.count(),
    ]);
    return {
      totalUsers,
      activeUsers,
      totalFoods,
      pendingFoods,
      totalBlogs,
      totalExercises,
    };
  }

  // ─── Users ────────────────────────────────────────────────────────────────

  async getUsers(page = 1, limit = 20, search?: string) {
    const [users, total] = await this.userRepo.findAndCount({
      where: search
        ? [
            { email: ILike(`%${search}%`) },
            { display_name: ILike(`%${search}%`) },
          ]
        : {},
      order: { created_at: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { users, total, page, limit };
  }

  async getUserById(id: string) {
    const user = await this.userRepo.findOne({
      where: { id },
      relations: ['healthProfile'],
    });
    if (!user) throw new NotFoundException('User not found');

    const recentWorkouts = await this.workoutSessionRepo.find({
      where: { userId: id },
      order: { createdAt: 'DESC' },
      take: 5,
      relations: ['exercise'],
    });

    return { ...user, recentWorkouts };
  }

  async banUser(id: string): Promise<User> {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    user.is_active = false;
    return this.userRepo.save(user);
  }

  async unbanUser(id: string): Promise<User> {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    user.is_active = true;
    return this.userRepo.save(user);
  }

  async forceVerifyEmail(id: string): Promise<User> {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    user.is_verified = true;
    return this.userRepo.save(user);
  }

  // ─── Foods ────────────────────────────────────────────────────────────────

  async getFoods(page = 1, limit = 20, search?: string) {
    const [foods, total] = await this.foodRepo.findAndCount({
      where: search
        ? [{ name: ILike(`%${search}%`) }, { category: ILike(`%${search}%`) }]
        : {},
      order: { created_at: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { foods, total, page, limit };
  }

  async createFood(dto: CreateFoodAdminDto): Promise<Food> {
    const food = this.foodRepo.create({
      name: dto.name,
      name_en: dto.nameEn,
      brand: dto.brand,
      category: dto.category,
      food_type: dto.foodType ?? 'ingredient',
      serving_size_g: dto.servingSizeG ?? 100,
      calories_per_100g: dto.caloriesPer100g,
      protein_per_100g: dto.proteinPer100g ?? 0,
      fat_per_100g: dto.fatPer100g ?? 0,
      carbs_per_100g: dto.carbsPer100g ?? 0,
      is_verified: dto.isVerified ?? true,
      is_active: true,
    });
    return this.foodRepo.save(food);
  }

  async updateFood(id: string, dto: UpdateFoodAdminDto): Promise<Food> {
    const food = await this.foodRepo.findOne({ where: { id } });
    if (!food) throw new NotFoundException('Food not found');
    if (dto.name !== undefined) food.name = dto.name;
    if (dto.nameEn !== undefined) food.name_en = dto.nameEn;
    if (dto.brand !== undefined) food.brand = dto.brand;
    if (dto.category !== undefined) food.category = dto.category;
    if (dto.foodType !== undefined) food.food_type = dto.foodType;
    if (dto.servingSizeG !== undefined) food.serving_size_g = dto.servingSizeG;
    if (dto.caloriesPer100g !== undefined)
      food.calories_per_100g = dto.caloriesPer100g;
    if (dto.proteinPer100g !== undefined)
      food.protein_per_100g = dto.proteinPer100g;
    if (dto.fatPer100g !== undefined) food.fat_per_100g = dto.fatPer100g;
    if (dto.carbsPer100g !== undefined) food.carbs_per_100g = dto.carbsPer100g;
    if (dto.isVerified !== undefined) food.is_verified = dto.isVerified;
    if (dto.isActive !== undefined) food.is_active = dto.isActive;
    return this.foodRepo.save(food);
  }

  // logic duyệt food từ user đóng góp lên cho cộng đồng --> Check lại user đã có logic này chưa

  async getPendingFoods(page = 1, limit = 20) {
    const [foods, total] = await this.foodRepo.findAndCount({
      where: { is_verified: false, is_active: true },
      order: { created_at: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { foods, total, page, limit };
  }

  async verifyFood(id: string): Promise<Food> {
    const food = await this.foodRepo.findOne({ where: { id } });
    if (!food) throw new NotFoundException('Food not found');
    food.is_verified = true;
    return this.foodRepo.save(food);
  }

  async rejectFood(id: string): Promise<void> {
    const food = await this.foodRepo.findOne({ where: { id } });
    if (!food) throw new NotFoundException('Food not found');
    food.is_active = false;
    await this.foodRepo.save(food);
  }

  async deleteFood(id: string): Promise<void> {
    const food = await this.foodRepo.findOne({ where: { id } });
    if (!food) throw new NotFoundException('Food not found');
    await this.foodRepo.delete(id);
  }

  // ─── Exercises ────────────────────────────────────────────────────────────

  async getExercises(page = 1, limit = 20, search?: string) {
    const [exercises, total] = await this.exerciseRepo.findAndCount({
      where: search ? { name: ILike(`%${search}%`) } : {},
      order: { createdAt: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { exercises, total, page, limit };
  }

  async createExercise(dto: CreateExerciseAdminDto): Promise<Exercise> {
    const exercise = this.exerciseRepo.create({
      name: dto.name,
      description: dto.description,
      primaryMuscleGroup: dto.primaryMuscleGroup,
      intensity: dto.intensity,
      metValue: dto.metValue ?? 0,
      instructions: dto.instructions,
      videoUrl: dto.videoUrl,
    });
    return this.exerciseRepo.save(exercise);
  }

  async updateExercise(
    id: string,
    dto: UpdateExerciseAdminDto,
  ): Promise<Exercise> {
    const exercise = await this.exerciseRepo.findOne({ where: { id } });
    if (!exercise) throw new NotFoundException('Exercise not found');
    Object.assign(exercise, dto);
    return this.exerciseRepo.save(exercise);
  }

  async deleteExercise(id: string): Promise<void> {
    const exercise = await this.exerciseRepo.findOne({ where: { id } });
    if (!exercise) throw new NotFoundException('Exercise not found');
    await this.exerciseRepo.delete(id);
  }

  // ─── Blogs ────────────────────────────────────────────────────────────────

  async getBlogs(page = 1, limit = 20) {
    const [blogs, total] = await this.blogRepo.findAndCount({
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { blogs, total, page, limit };
  }

  async getOneBlog(id: string): Promise<Blog> {
    const blog = await this.blogRepo.findOne({ where: { id } });
    if (!blog) throw new NotFoundException('Blog not found');
    return blog;
  }

  async createBlog(dto: CreateBlogDto): Promise<Blog> {
    const blog = this.blogRepo.create({
      title: dto.title,
      content: dto.content,
      author: dto.author ?? 'Admin',
      thumbnailUrl: dto.thumbnailUrl ?? null,
    });
    return this.blogRepo.save(blog);
  }

  async updateBlog(id: string, dto: UpdateBlogDto): Promise<Blog> {
    const blog = await this.blogRepo.findOne({ where: { id } });
    if (!blog) throw new NotFoundException('Blog not found');
    Object.assign(blog, {
      ...(dto.title !== undefined && { title: dto.title }),
      ...(dto.content !== undefined && { content: dto.content }),
      ...(dto.author !== undefined && { author: dto.author }),
      ...(dto.thumbnailUrl !== undefined && { thumbnailUrl: dto.thumbnailUrl }),
    });
    return this.blogRepo.save(blog);
  }

  async deleteBlog(id: string): Promise<void> {
    const blog = await this.blogRepo.findOne({ where: { id } });
    if (!blog) throw new NotFoundException('Blog not found');
    await this.blogRepo.delete(id);
  }

  // ─── Sport Tips ───────────────────────────────────────────────────────────

  async getAllSportTips(page = 1, limit = 20) {
    const [items, total] = await this.sportTipRepo.findAndCount({
      order: { created_at: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { items, total, page, limit };
  }

  async createSportTip(dto: Partial<SportTip>): Promise<SportTip> {
    const tip = this.sportTipRepo.create(dto);
    return this.sportTipRepo.save(tip);
  }

  async updateSportTip(id: string, dto: Partial<SportTip>): Promise<SportTip> {
    const tip = await this.sportTipRepo.findOne({ where: { id } });
    if (!tip) throw new NotFoundException('Sport tip not found');
    Object.assign(tip, dto);
    return this.sportTipRepo.save(tip);
  }

  async deleteSportTip(id: string): Promise<void> {
    const tip = await this.sportTipRepo.findOne({ where: { id } });
    if (!tip) throw new NotFoundException('Sport tip not found');
    await this.sportTipRepo.delete(id);
  }

  // ─── Food Recipes (Admin) ─────────────────────────────────────────────────

  async upsertFoodRecipe(
    foodId: string,
    dto: CreateRecipeDto,
  ): Promise<FoodRecipe> {
    const food = await this.foodRepo.findOne({ where: { id: foodId } });
    if (!food) throw new NotFoundException('Food not found');

    let recipe = await this.foodRecipeRepo.findOne({
      where: { food_id: foodId },
    });
    if (!recipe) {
      recipe = this.foodRecipeRepo.create({ food_id: foodId });
    }
    if (dto.prep_time_min !== undefined)
      recipe.prep_time_min = dto.prep_time_min;
    if (dto.cook_time_min !== undefined)
      recipe.cook_time_min = dto.cook_time_min;
    if (dto.servings !== undefined) recipe.servings = dto.servings;
    await this.foodRecipeRepo.save(recipe);

    if (dto.steps?.length) {
      await this.foodRecipeStepRepo.delete({ recipe_id: recipe.id });
      const steps = dto.steps.map((s) =>
        this.foodRecipeStepRepo.create({ recipe_id: recipe.id, ...s }),
      );
      await this.foodRecipeStepRepo.save(steps);
    }

    return this.foodRecipeRepo.findOne({
      where: { id: recipe.id },
      relations: ['steps'],
      order: { steps: { step_number: 'ASC' } },
    }) as Promise<FoodRecipe>;
  }

  async addFoodRecipeStep(
    foodId: string,
    dto: AddRecipeStepDto,
  ): Promise<FoodRecipeStep> {
    const food = await this.foodRepo.findOne({ where: { id: foodId } });
    if (!food) throw new NotFoundException('Food not found');

    let recipe = await this.foodRecipeRepo.findOne({
      where: { food_id: foodId },
    });
    if (!recipe) {
      recipe = await this.foodRecipeRepo.save(
        this.foodRecipeRepo.create({ food_id: foodId }),
      );
    }
    const step = this.foodRecipeStepRepo.create({
      recipe_id: recipe.id,
      ...dto,
    });
    return this.foodRecipeStepRepo.save(step);
  }

  async deleteFoodRecipeStep(stepId: string): Promise<void> {
    const step = await this.foodRecipeStepRepo.findOne({
      where: { id: stepId },
    });
    if (!step) throw new NotFoundException('Recipe step not found');
    await this.foodRecipeStepRepo.delete(stepId);
  }
}
