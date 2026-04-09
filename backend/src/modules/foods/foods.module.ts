import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { FoodsController } from './foods.controller';
import { FoodsService } from './foods.service';
import { Food } from './entities/food.entity';
import { FoodBarcode } from './entities/food-barcode.entity';
import { FoodUserFavorite } from './entities/food-user-favorite.entity';
import { CloudinaryModule } from '../cloudinary/cloudinary.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Food, FoodBarcode, FoodUserFavorite]),
    CloudinaryModule,
  ],
  controllers: [FoodsController],
  providers: [FoodsService],
  exports: [FoodsService],
})
export class FoodsModule {}
