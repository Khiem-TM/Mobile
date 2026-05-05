import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SupportModule } from '../support/support.module';
import { UserModule } from '../user/user.module';
// entities
import { Food } from './entities/food.entity';
import { FoodBarcode } from './entities/food-barcode.entity';
import { FoodUserFavorite } from './entities/food-user-favorite.entity';
import { FoodRecipe } from './entities/food-recipe.entity';
import { FoodRecipeStep } from './entities/food-recipe-step.entity';
import { FoodIngredient } from './entities/food-ingredient.entity';
import { MealLog } from './entities/meal-log.entity';
import { MealLogItem } from './entities/meal-log-item.entity';
import { AiScanLog } from './entities/ai-scan-log.entity';
// controllers
import { FoodsController } from './controllers/foods.controller';
import { MealLogsController } from './controllers/meal-logs.controller';
import { AiScanController } from './controllers/ai-scan.controller';
// services
import { FoodsService } from './services/foods.service';
import { MealLogsService } from './services/meal-logs.service';
import { AiScanService } from './services/ai-scan.service';
// repositories
import { MEAL_LOGS_REPOSITORY } from './food.constants';
import { MealLogsRepository } from './repositories/meal-logs.repository';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Food,
      FoodBarcode,
      FoodUserFavorite,
      FoodRecipe,
      FoodRecipeStep,
      FoodIngredient,
      MealLog,
      MealLogItem,
      AiScanLog,
    ]),
    SupportModule,
    forwardRef(() => UserModule),
  ],
  controllers: [FoodsController, MealLogsController, AiScanController],
  providers: [
    FoodsService,
    MealLogsService,
    AiScanService,
    { provide: MEAL_LOGS_REPOSITORY, useClass: MealLogsRepository },
  ],
  exports: [FoodsService, MealLogsService, AiScanService],
})
export class FoodModule {}
