package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.ui.components.LoadingState

@Composable
fun CreateFoodScreen(
    navController: NavController,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) navController.popBackStack()
    }

    Scaffold(
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                }
                Text("Món của tôi", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF38C182))
                        .clickable {
                            if (name.isNotBlank() && calories.isNotBlank()) {
                                viewModel.createFood(
                                    CreateFoodRequest(
                                        name = name,
                                        brand = brand.ifBlank { null },
                                        servingSizeG = servingSize.toFloatOrNull() ?: 100f,
                                        caloriesPer100g = calories.toFloatOrNull() ?: 0f,
                                        carbsPer100g = carbs.toFloatOrNull() ?: 0f,
                                        proteinPer100g = protein.toFloatOrNull() ?: 0f,
                                        fatPer100g = fat.toFloatOrNull() ?: 0f,
                                        fiberPer100g = fiber.toFloatOrNull(),
                                        sugarPer100g = sugar.toFloatOrNull()
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Add Image Box
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp)) // Blue border
                        .clickable { /* Handle Image Upload */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Thêm ảnh", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Basic Info Fields
                FormField("TÊN MÓN", "VD: Salad cá ngừ tự làm", name) { name = it }
                Spacer(modifier = Modifier.height(16.dp))
                FormField("THƯƠNG HIỆU (TUỲ CHỌN)", "VD: Homemade", brand) { brand = it }
                Spacer(modifier = Modifier.height(16.dp))
                FormField("KHẨU PHẦN", "VD: 1 phần · 250g", servingSize) { servingSize = it }

                Spacer(modifier = Modifier.height(24.dp))
                Text("THÔNG TIN DINH DƯỠNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // Nutrition Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        NutritionField("Năng lượng", calories, "kcal") { calories = it }
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        NutritionField("Carbs", carbs, "g") { carbs = it }
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        NutritionField("Protein", protein, "g") { protein = it }
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        NutritionField("Fat", fat, "g") { fat = it }
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        NutritionField("Fiber", fiber, "g") { fiber = it }
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        NutritionField("Sugar", sugar, "g") { sugar = it }
                    }
                }

                uiState.error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = Color(0xFFEF4444), fontSize = 13.sp)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun FormField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE5E7EB),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF38C182)
            )
        )
    }
}

@Composable
fun NutritionField(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = androidx.compose.ui.text.style.TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(60.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text("0", color = Color.Gray, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(unit, fontSize = 13.sp, color = Color.Gray)
        }
    }
}
