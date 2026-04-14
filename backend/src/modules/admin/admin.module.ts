import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { User } from '../users/entities/user.entity';
import { Food } from '../foods/entities/food.entity';
import { Exercise } from '../training/entities/exercise.entity';
import { WorkoutSession } from '../training/entities/workout-session.entity';
import { Blog } from './entities/blog.entity';
import { SportTip } from '../training/entities/sport-tip.entity';
import { FoodRecipe } from '../foods/entities/food-recipe.entity';
import { FoodRecipeStep } from '../foods/entities/food-recipe-step.entity';
import { AdminService } from './admin.service';
import { AdminController } from './admin.controller';
import { AdminAuthController } from './admin-auth.controller';
import { BlogController } from './blog.controller';
import { SportTipAdminController } from './sport-tip.controller';
import { RecipeAdminController } from './recipe.controller';
import { JwtStrategy } from '../auth/strategies/jwt.strategy';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, Food, Exercise, WorkoutSession, Blog, SportTip, FoodRecipe, FoodRecipeStep]),
    PassportModule,
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'khiemhehe',
      signOptions: { expiresIn: '7d' },
    }),
  ],
  controllers: [AdminController, AdminAuthController, BlogController, SportTipAdminController, RecipeAdminController],
  providers: [AdminService, JwtStrategy],
})
export class AdminModule {}
