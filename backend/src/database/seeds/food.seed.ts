import { DataSource } from 'typeorm';

const foods = [
  // ─── Tinh bột ───────────────────────────────────────────────
  { name: 'Cơm trắng', name_en: 'White Rice', category: 'Tinh bột', food_type: 'dish', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 130, protein_per_100g: 2.7, fat_per_100g: 0.3, carbs_per_100g: 28.2, fiber_per_100g: 0.4 },
  { name: 'Cơm gạo lứt', name_en: 'Brown Rice', category: 'Tinh bột', food_type: 'dish', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 111, protein_per_100g: 2.6, fat_per_100g: 0.9, carbs_per_100g: 23.0, fiber_per_100g: 1.8 },
  { name: 'Bún tươi', name_en: 'Rice Vermicelli', category: 'Tinh bột', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 109, protein_per_100g: 2.5, fat_per_100g: 0.2, carbs_per_100g: 24.5, fiber_per_100g: 0.3 },
  { name: 'Phở bò', name_en: 'Beef Pho', category: 'Tinh bột', food_type: 'dish', serving_size_g: 500, serving_unit: 'tô', calories_per_100g: 74, protein_per_100g: 5.0, fat_per_100g: 2.5, carbs_per_100g: 8.5, fiber_per_100g: 0.5 },
  { name: 'Bánh mì', name_en: 'Baguette', category: 'Tinh bột', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 270, protein_per_100g: 9.0, fat_per_100g: 3.2, carbs_per_100g: 50.0, fiber_per_100g: 2.3 },
  { name: 'Xôi trắng', name_en: 'Sticky Rice', category: 'Tinh bột', food_type: 'dish', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 180, protein_per_100g: 3.5, fat_per_100g: 0.5, carbs_per_100g: 39.0, fiber_per_100g: 0.5 },
  { name: 'Mì gói', name_en: 'Instant Noodles', category: 'Tinh bột', food_type: 'product', serving_size_g: 75, serving_unit: 'gói', calories_per_100g: 440, protein_per_100g: 8.5, fat_per_100g: 17.0, carbs_per_100g: 62.0, fiber_per_100g: 2.5, sodium_per_100g: 1200 },
  { name: 'Bánh phở khô', name_en: 'Dried Pho Noodles', category: 'Tinh bột', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 360, protein_per_100g: 7.0, fat_per_100g: 0.5, carbs_per_100g: 82.0, fiber_per_100g: 1.2 },
  { name: 'Khoai lang', name_en: 'Sweet Potato', category: 'Tinh bột', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 86, protein_per_100g: 1.6, fat_per_100g: 0.1, carbs_per_100g: 20.0, fiber_per_100g: 3.0 },
  { name: 'Khoai tây', name_en: 'Potato', category: 'Tinh bột', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 77, protein_per_100g: 2.0, fat_per_100g: 0.1, carbs_per_100g: 17.0, fiber_per_100g: 2.2 },
  // ─── Thịt & hải sản ─────────────────────────────────────────
  { name: 'Thịt lợn nạc', name_en: 'Lean Pork', category: 'Thịt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 143, protein_per_100g: 21.5, fat_per_100g: 6.0, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Ức gà', name_en: 'Chicken Breast', category: 'Thịt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 165, protein_per_100g: 31.0, fat_per_100g: 3.6, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Đùi gà', name_en: 'Chicken Thigh', category: 'Thịt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 209, protein_per_100g: 26.0, fat_per_100g: 11.0, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Thịt bò nạc', name_en: 'Lean Beef', category: 'Thịt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 158, protein_per_100g: 26.0, fat_per_100g: 5.5, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Cá hồi', name_en: 'Salmon', category: 'Hải sản', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 208, protein_per_100g: 20.0, fat_per_100g: 13.0, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Cá ngừ đóng hộp', name_en: 'Canned Tuna', category: 'Hải sản', food_type: 'product', serving_size_g: 85, serving_unit: 'hộp nhỏ', calories_per_100g: 116, protein_per_100g: 26.0, fat_per_100g: 1.0, carbs_per_100g: 0, fiber_per_100g: 0, sodium_per_100g: 400 },
  { name: 'Tôm thẻ', name_en: 'White Shrimp', category: 'Hải sản', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 85, protein_per_100g: 18.0, fat_per_100g: 0.9, carbs_per_100g: 0.9, fiber_per_100g: 0 },
  { name: 'Cá rô phi', name_en: 'Tilapia', category: 'Hải sản', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 96, protein_per_100g: 20.0, fat_per_100g: 1.7, carbs_per_100g: 0, fiber_per_100g: 0 },
  // ─── Trứng & sữa ────────────────────────────────────────────
  { name: 'Trứng gà', name_en: 'Chicken Egg', category: 'Trứng & sữa', food_type: 'ingredient', serving_size_g: 60, serving_unit: 'quả', calories_per_100g: 155, protein_per_100g: 13.0, fat_per_100g: 11.0, carbs_per_100g: 1.1, fiber_per_100g: 0 },
  { name: 'Sữa tươi không đường', name_en: 'Fresh Milk Unsweetened', category: 'Trứng & sữa', food_type: 'product', serving_size_g: 240, serving_unit: 'hộp', calories_per_100g: 42, protein_per_100g: 3.4, fat_per_100g: 1.0, carbs_per_100g: 5.0, fiber_per_100g: 0, sugar_per_100g: 5.0 },
  { name: 'Sữa chua không đường', name_en: 'Plain Yogurt', category: 'Trứng & sữa', food_type: 'product', serving_size_g: 100, serving_unit: 'hộp', calories_per_100g: 59, protein_per_100g: 3.5, fat_per_100g: 3.3, carbs_per_100g: 4.7, fiber_per_100g: 0 },
  { name: 'Đậu hũ (tàu hũ)', name_en: 'Tofu', category: 'Trứng & sữa', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 76, protein_per_100g: 8.0, fat_per_100g: 4.2, carbs_per_100g: 1.9, fiber_per_100g: 0.3 },
  // ─── Rau củ ──────────────────────────────────────────────────
  { name: 'Rau muống', name_en: 'Water Spinach', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 19, protein_per_100g: 2.6, fat_per_100g: 0.2, carbs_per_100g: 1.8, fiber_per_100g: 2.1 },
  { name: 'Bắp cải', name_en: 'Cabbage', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 25, protein_per_100g: 1.3, fat_per_100g: 0.1, carbs_per_100g: 5.8, fiber_per_100g: 2.5 },
  { name: 'Cà chua', name_en: 'Tomato', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 18, protein_per_100g: 0.9, fat_per_100g: 0.2, carbs_per_100g: 3.9, fiber_per_100g: 1.2 },
  { name: 'Dưa leo', name_en: 'Cucumber', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 15, protein_per_100g: 0.7, fat_per_100g: 0.1, carbs_per_100g: 3.6, fiber_per_100g: 0.5 },
  { name: 'Cà rốt', name_en: 'Carrot', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 41, protein_per_100g: 0.9, fat_per_100g: 0.2, carbs_per_100g: 9.6, fiber_per_100g: 2.8 },
  { name: 'Bông cải xanh', name_en: 'Broccoli', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 34, protein_per_100g: 2.8, fat_per_100g: 0.4, carbs_per_100g: 6.6, fiber_per_100g: 2.6 },
  { name: 'Cải xanh', name_en: 'Bok Choy', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 13, protein_per_100g: 1.5, fat_per_100g: 0.2, carbs_per_100g: 2.2, fiber_per_100g: 1.0 },
  { name: 'Nấm đông cô', name_en: 'Shiitake Mushroom', category: 'Rau củ', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 34, protein_per_100g: 2.2, fat_per_100g: 0.5, carbs_per_100g: 6.8, fiber_per_100g: 2.5 },
  // ─── Trái cây ────────────────────────────────────────────────
  { name: 'Chuối tiêu', name_en: 'Banana', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 120, serving_unit: 'quả', calories_per_100g: 89, protein_per_100g: 1.1, fat_per_100g: 0.3, carbs_per_100g: 23.0, fiber_per_100g: 2.6, sugar_per_100g: 12.0 },
  { name: 'Xoài', name_en: 'Mango', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 200, serving_unit: 'quả', calories_per_100g: 60, protein_per_100g: 0.8, fat_per_100g: 0.4, carbs_per_100g: 15.0, fiber_per_100g: 1.6, sugar_per_100g: 13.7 },
  { name: 'Cam', name_en: 'Orange', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 130, serving_unit: 'quả', calories_per_100g: 47, protein_per_100g: 0.9, fat_per_100g: 0.1, carbs_per_100g: 11.8, fiber_per_100g: 2.4, sugar_per_100g: 9.4 },
  { name: 'Táo', name_en: 'Apple', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 182, serving_unit: 'quả', calories_per_100g: 52, protein_per_100g: 0.3, fat_per_100g: 0.2, carbs_per_100g: 14.0, fiber_per_100g: 2.4, sugar_per_100g: 10.0 },
  { name: 'Dưa hấu', name_en: 'Watermelon', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 30, protein_per_100g: 0.6, fat_per_100g: 0.2, carbs_per_100g: 7.6, fiber_per_100g: 0.4, sugar_per_100g: 6.2 },
  { name: 'Bưởi', name_en: 'Pomelo', category: 'Trái cây', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 32, protein_per_100g: 0.6, fat_per_100g: 0.1, carbs_per_100g: 7.9, fiber_per_100g: 1.1 },
  // ─── Đậu & hạt ───────────────────────────────────────────────
  { name: 'Đậu đen', name_en: 'Black Bean', category: 'Đậu & hạt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 132, protein_per_100g: 8.9, fat_per_100g: 0.5, carbs_per_100g: 24.0, fiber_per_100g: 8.7 },
  { name: 'Đậu xanh', name_en: 'Mung Bean', category: 'Đậu & hạt', food_type: 'ingredient', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 105, protein_per_100g: 7.0, fat_per_100g: 0.4, carbs_per_100g: 19.0, fiber_per_100g: 7.6 },
  { name: 'Hạt điều', name_en: 'Cashew Nuts', category: 'Đậu & hạt', food_type: 'ingredient', serving_size_g: 30, serving_unit: 'nắm', calories_per_100g: 553, protein_per_100g: 18.0, fat_per_100g: 44.0, carbs_per_100g: 30.0, fiber_per_100g: 3.3 },
  { name: 'Hạnh nhân', name_en: 'Almond', category: 'Đậu & hạt', food_type: 'ingredient', serving_size_g: 30, serving_unit: 'nắm', calories_per_100g: 579, protein_per_100g: 21.0, fat_per_100g: 50.0, carbs_per_100g: 22.0, fiber_per_100g: 12.5 },
  // ─── Chất béo & gia vị ───────────────────────────────────────
  { name: 'Dầu ăn (dầu thực vật)', name_en: 'Vegetable Oil', category: 'Chất béo', food_type: 'ingredient', serving_size_g: 14, serving_unit: 'muỗng canh', calories_per_100g: 884, protein_per_100g: 0, fat_per_100g: 100, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Dầu ô liu', name_en: 'Olive Oil', category: 'Chất béo', food_type: 'ingredient', serving_size_g: 14, serving_unit: 'muỗng canh', calories_per_100g: 884, protein_per_100g: 0, fat_per_100g: 100, carbs_per_100g: 0, fiber_per_100g: 0 },
  { name: 'Đường trắng', name_en: 'White Sugar', category: 'Đường', food_type: 'ingredient', serving_size_g: 5, serving_unit: 'muỗng cà phê', calories_per_100g: 387, protein_per_100g: 0, fat_per_100g: 0, carbs_per_100g: 100, fiber_per_100g: 0, sugar_per_100g: 100 },
  // ─── Đồ uống ─────────────────────────────────────────────────
  { name: 'Nước dừa', name_en: 'Coconut Water', category: 'Đồ uống', food_type: 'product', serving_size_g: 240, serving_unit: 'ly', calories_per_100g: 19, protein_per_100g: 0.7, fat_per_100g: 0.2, carbs_per_100g: 3.7, fiber_per_100g: 1.1, sugar_per_100g: 2.6 },
  { name: 'Sữa đậu nành không đường', name_en: 'Unsweetened Soy Milk', category: 'Đồ uống', food_type: 'product', serving_size_g: 240, serving_unit: 'ly', calories_per_100g: 33, protein_per_100g: 3.3, fat_per_100g: 1.8, carbs_per_100g: 1.7, fiber_per_100g: 0.4 },
  // ─── Đồ ăn phổ biến ──────────────────────────────────────────
  { name: 'Bánh cuốn', name_en: 'Steamed Rice Rolls', category: 'Tinh bột', food_type: 'dish', serving_size_g: 150, serving_unit: 'phần', calories_per_100g: 120, protein_per_100g: 5.0, fat_per_100g: 3.5, carbs_per_100g: 18.0, fiber_per_100g: 0.5 },
  { name: 'Bánh bao nhân thịt', name_en: 'Steamed Pork Bun', category: 'Tinh bột', food_type: 'dish', serving_size_g: 80, serving_unit: 'cái', calories_per_100g: 230, protein_per_100g: 8.0, fat_per_100g: 6.5, carbs_per_100g: 36.0, fiber_per_100g: 1.0 },
  { name: 'Salad rau trộn', name_en: 'Garden Salad', category: 'Rau củ', food_type: 'dish', serving_size_g: 100, serving_unit: 'g', calories_per_100g: 20, protein_per_100g: 1.5, fat_per_100g: 0.3, carbs_per_100g: 3.5, fiber_per_100g: 2.0 },
  { name: 'Protein whey', name_en: 'Whey Protein', category: 'Supplement', food_type: 'product', serving_size_g: 30, serving_unit: 'muỗng', calories_per_100g: 400, protein_per_100g: 80.0, fat_per_100g: 5.0, carbs_per_100g: 10.0, fiber_per_100g: 0 },
];

export async function seedFoods(dataSource: DataSource): Promise<void> {
  const repo = dataSource.getRepository('foods');
  const officialCount = await repo.count({ where: { is_custom: false } });
  if (officialCount > 0) {
    console.log(`[Seed] foods table already has ${officialCount} official records. Skipping.`);
    return;
  }

  const records = foods.map((f) => ({
    ...f,
    is_custom: false,
    is_verified: true,
    is_active: true,
  }));

  await repo.save(records);
  console.log(`[Seed] Inserted ${records.length} foods.`);
}
