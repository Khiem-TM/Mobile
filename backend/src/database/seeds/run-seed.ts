// @ts-nocheck
import { NestFactory } from '@nestjs/core';
import { AppModule } from '../../app.module';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Food } from '../../modules/food/entities/food.entity';
import * as fs from 'fs';
import * as path from 'path';

async function bootstrap() {
  const app = await NestFactory.createApplicationContext(AppModule);
  console.log('Application Context created');

  const foodRepo = app.get(getRepositoryToken(Food));
  
  const rawData = fs.readFileSync(path.join(__dirname, 'raw_foods.json'), 'utf8');
  const foods = JSON.parse(rawData);

  // find actual image paths
  const imgDir = '/Users/UET/DACN/Project/assets/food_images';
  let availableImages: string[] = [];
  if (fs.existsSync(imgDir)) {
    availableImages = fs.readdirSync(imgDir).filter(f => f.endsWith('.png'));
  }

  console.log(`Starting to seed ${foods.length} items...`);
  
  const batchSize = 100;
  for (let i = 0; i < foods.length; i += batchSize) {
    const batch = foods.slice(i, i + batchSize);
    
    const entities = batch.map((f: any) => {
      let finalImgPath = null;
      if (f._img_keyword) {
        const matchingImg = availableImages.find(img => img.includes(f._img_keyword));
        if (matchingImg) {
          finalImgPath = `http://localhost:5173/assets/food_images/${matchingImg}`; // assuming frontend/public or backend static serve. Wait, normally just '/assets/food_images/...'
        }
      }
      
      const newFood = foodRepo.create({
        name: f.name,
        name_en: f.name_en,
        category: f.category,
        food_type: f.food_type,
        serving_size_g: f.serving_size_g,
        serving_unit: f.serving_unit,
        calories_per_100g: f.calories_per_100g,
        protein_per_100g: f.protein_per_100g,
        fat_per_100g: f.fat_per_100g,
        carbs_per_100g: f.carbs_per_100g,
        fiber_per_100g: f.fiber_per_100g,
        sugar_per_100g: f.sugar_per_100g,
        sodium_per_100g: f.sodium_per_100g,
        cholesterol_per_100g: f.cholesterol_per_100g,
        is_verified: f.is_verified,
        is_custom: f.is_custom,
        image_urls: finalImgPath ? [finalImgPath] : []
      });
      return newFood;
    });

    await foodRepo.save(entities);
    console.log(`Saved batch ${i / batchSize + 1}`);
  }

  console.log('Seed completed successfully!');
  await app.close();
  process.exit(0);
}

bootstrap().catch(err => {
  console.error(err);
  process.exit(1);
});
