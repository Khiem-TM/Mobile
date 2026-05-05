import { forwardRef } from '@nestjs/common';
import { UserModule } from '../user/user.module';
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { User } from '../user/entities/user.entity';
import { Food } from '../food/entities/food.entity';
import { Exercise } from '../train/entities/exercise.entity';
import { WorkoutSession } from '../train/entities/workout-session.entity';
import { Blog } from '../blog/entities/blog.entity';
import { FoodRecipe } from '../food/entities/food-recipe.entity';
import { FoodRecipeStep } from '../food/entities/food-recipe-step.entity';
import { AdminService } from './admin.service';
import { AdminController } from './admin.controller';
import { AdminAuthController } from './admin-auth.controller';
import { RecipeAdminController } from './recipe.controller';
import { JwtStrategy } from '../user/strategies/jwt.strategy';

@Module({
  imports: [
    forwardRef(() => UserModule),
    
    TypeOrmModule.forFeature([User, Food, Exercise, WorkoutSession, Blog, FoodRecipe, FoodRecipeStep]),
    PassportModule,
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'khiemhehe',
      signOptions: { expiresIn: '7d' },
    }),
  ],
  controllers: [AdminController, AdminAuthController, RecipeAdminController],
  providers: [AdminService, JwtStrategy],
})
export class AdminModule {}
