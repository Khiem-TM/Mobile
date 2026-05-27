-- Seed 20 basic Vietnamese dishes
INSERT INTO foods (
    id, name, name_en, category, description, food_type, 
    serving_size_g, serving_unit, calories_per_100g, 
    protein_per_100g, fat_per_100g, carbs_per_100g, 
    fiber_per_100g,
    image_urls, is_verified, is_active, created_at, updated_at
) VALUES 
(gen_random_uuid(), 'Phở Bò', 'Beef Pho', 'Noodle Soup', 'Traditional Vietnamese beef noodle soup with aromatic herbs.', 'dish', 500, 'bowl', 85, 5.0, 3.0, 9.5, NULL, ARRAY['/images/foods/pho-bo.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bánh Mì Thịt', 'Vietnamese Meat Baguette', 'Bread', 'Crispy baguette filled with savory meats, pate, and pickled vegetables.', 'dish', 250, 'piece', 245, 8.5, 11.0, 28.0, NULL, ARRAY['/images/foods/banh-mi.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bún Chả Hà Nội', 'Hanoi Grilled Pork Noodles', 'Noodle', 'Grilled pork patties served with vermicelli and dipping sauce.', 'dish', 400, 'serving', 140, 7.5, 6.5, 13.0, NULL, ARRAY['/images/foods/bun-cha.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Cơm Tấm Sườn', 'Broken Rice with Grilled Pork', 'Rice', 'Broken rice served with marinated grilled pork chop.', 'dish', 450, 'plate', 165, 7.0, 8.5, 15.0, NULL, ARRAY['/images/foods/com-tam.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Gỏi Cuốn', 'Fresh Spring Rolls', 'Appetizer', 'Fresh rolls with shrimp, pork, herbs, and rice vermicelli.', 'dish', 60, 'roll', 110, 6.0, 2.5, 16.0, NULL, ARRAY['/images/foods/goi-cuon.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bún Bò Huế', 'Hue Spicy Beef Noodle Soup', 'Noodle Soup', 'Spicy beef noodle soup from the central city of Hue.', 'dish', 550, 'bowl', 75, 4.5, 3.5, 6.0, NULL, ARRAY['/images/foods/bun-bo-hue.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Chả Giò', 'Fried Spring Rolls', 'Appetizer', 'Crispy fried rolls filled with minced pork, shrimp, and vegetables.', 'dish', 50, 'piece', 280, 6.5, 18.0, 23.0, NULL, ARRAY['/images/foods/cha-gio.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bánh Xèo', 'Sizzling Pancake', 'Pancake', 'Crispy pancake filled with shrimp, pork, and bean sprouts.', 'dish', 300, 'piece', 195, 5.5, 12.0, 16.5, NULL, ARRAY['/images/foods/banh-xeo.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bún Riêu Cua', 'Crab Noodle Soup', 'Noodle Soup', 'Noodle soup with a tomato-based broth and crab paste.', 'dish', 500, 'bowl', 68, 4.2, 3.2, 5.5, NULL, ARRAY['/images/foods/bun-rieu.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bánh Cuốn', 'Steamed Rice Rolls', 'Breakfast', 'Thin steamed rice sheets filled with minced pork and mushrooms.', 'dish', 200, 'plate', 125, 3.5, 4.2, 18.5, NULL, ARRAY['/images/foods/banh-cuon.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Hủ Tiếu Nam Vang', 'Nam Vang Noodle Soup', 'Noodle Soup', 'Pork-based noodle soup with shrimp and quail eggs.', 'dish', 500, 'bowl', 72, 4.5, 2.8, 7.2, NULL, ARRAY['/images/foods/hu-tieu.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Cơm Gà Hội An', 'Hoi An Chicken Rice', 'Rice', 'Turmeric rice with shredded chicken and fresh herbs.', 'dish', 400, 'plate', 155, 8.2, 6.2, 17.5, NULL, ARRAY['/images/foods/com-ga.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bún Đậu Mắm Tôm', 'Vermicelli with Fried Tofu', 'Platter', 'Vermicelli served with fried tofu and fermented shrimp paste.', 'dish', 450, 'set', 135, 7.2, 9.5, 5.5, NULL, ARRAY['/images/foods/bun-dau.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Cá Kho Tộ', 'Caramelized Fish in Clay Pot', 'Main Dish', 'Fish braised in a savory and sweet caramel sauce.', 'dish', 150, 'serving', 125, 16.0, 6.5, 2.5, NULL, ARRAY['/images/foods/ca-kho-to.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Canh Chua Cá Lóc', 'Sour Snakehead Fish Soup', 'Soup', 'Refreshing sour soup with fish and tropical fruits.', 'dish', 400, 'bowl', 42, 3.5, 1.2, 4.8, NULL, ARRAY['/images/foods/canh-chua.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bánh Canh Cua', 'Crab Thick Noodle Soup', 'Noodle Soup', 'Thick noodles in a rich crab-flavored broth.', 'dish', 500, 'bowl', 78, 5.2, 3.4, 6.8, NULL, ARRAY['/images/foods/banh-canh.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Mì Quảng', 'Quang Style Noodles', 'Noodle', 'Specialty noodles from Quang Nam with shrimp and pork.', 'dish', 400, 'bowl', 135, 6.5, 5.5, 14.5, NULL, ARRAY['/images/foods/mi-quang.png'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Bún Thịt Nướng', 'Vermicelli with Grilled Pork', 'Noodle', 'Cold vermicelli bowl with grilled marinated pork.', 'dish', 400, 'bowl', 145, 7.5, 6.8, 14.0, NULL, ARRAY['https://images.unsplash.com/photo-1512058560366-cd242d4ba351?q=80&w=1000'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Xôi Xéo', 'Sticky Rice with Mung Bean', 'Breakfast', 'Yellow sticky rice with mung bean and fried shallots.', 'dish', 200, 'serving', 215, 4.2, 6.5, 36.5, NULL, ARRAY['https://images.unsplash.com/photo-1619451334792-150fd785ee74?q=80&w=1000'], true, true, NOW(), NOW()),
(gen_random_uuid(), 'Phở Gà', 'Chicken Pho', 'Noodle Soup', 'Traditional Vietnamese chicken noodle soup.', 'dish', 500, 'bowl', 78, 6.0, 2.5, 8.5, NULL, ARRAY['https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?q=80&w=1000'], true, true, NOW(), NOW());
