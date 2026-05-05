package com.vitalai.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFoodScreen(
    navController: NavController,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("100") }
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
        topBar = {
            TopAppBar(
                title = { Text("Tạo món ăn mới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Mint50)
                    .border(1.dp, Mint200, RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 24.sp)
                    Text("+ Thêm ảnh", fontSize = 11.sp, color = Mint500)
                }
            }

            FoodTextField("Tên món ăn *", name, { name = it })
            FoodTextField("Thương hiệu (tuỳ chọn)", brand, { brand = it })
            FoodTextField("Khẩu phần (g)", servingSize, { servingSize = it }, KeyboardType.Number)

            Text("Thông tin dinh dưỡng (trên 100g)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink900)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodTextField("Calories (kcal) *", calories, { calories = it }, KeyboardType.Number, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodTextField("Carbs (g) *", carbs, { carbs = it }, KeyboardType.Number, Modifier.weight(1f))
                FoodTextField("Protein (g) *", protein, { protein = it }, KeyboardType.Number, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodTextField("Chất béo (g) *", fat, { fat = it }, KeyboardType.Number, Modifier.weight(1f))
                FoodTextField("Chất xơ (g)", fiber, { fiber = it }, KeyboardType.Number, Modifier.weight(1f))
            }
            FoodTextField("Đường (g)", sugar, { sugar = it }, KeyboardType.Number)

            uiState.error?.let {
                Text(text = it, color = MacroProtein, fontSize = 13.sp)
            }

            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint900),
                enabled = name.isNotBlank() && calories.isNotBlank()
            ) {
                Text("Lưu món ăn", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun FoodTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mint500,
            unfocusedBorderColor = Ink200,
            focusedContainerColor = AppSurface,
            unfocusedContainerColor = AppSurface
        )
    )
}

