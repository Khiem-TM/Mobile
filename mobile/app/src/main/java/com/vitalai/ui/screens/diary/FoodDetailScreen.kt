package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@Composable
fun FoodDetailScreen(
    foodId: String,
    mealType: String = "",
    date: String = "",
    navController: NavController,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var quantity by remember { mutableFloatStateOf(1f) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(foodId) { viewModel.loadFoodById(foodId) }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            snackbarHostState.showSnackbar("Đã thêm vào bữa ăn!")
            viewModel.clearAddSuccess()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (err != null && uiState.selectedFood != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppSurface
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null && uiState.selectedFood == null -> ErrorState(
                message = uiState.error ?: "Không tìm thấy món ăn",
                onRetry = { viewModel.loadFoodById(foodId) },
                modifier = Modifier.padding(padding)
            )
            else -> {
                val food = uiState.selectedFood ?: return@Scaffold
                val servingG = food.servingSizeG.takeIf { it > 0f } ?: 100f
                val factor = quantity * servingG / 100f
                val calories = (food.caloriesPer100g * factor).toInt()
                val carbs = food.carbsPer100g * factor
                val protein = food.proteinPer100g * factor
                val fat = food.fatPer100g * factor

                val totalMacros = carbs + protein + fat
                val carbPct = if (totalMacros > 0) ((carbs / totalMacros) * 100).toInt() else 0
                val proPct = if (totalMacros > 0) ((protein / totalMacros) * 100).toInt() else 0
                val fatPct = if (totalMacros > 0) ((fat / totalMacros) * 100).toInt() else 0

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 100.dp)
                    ) {
                        // Image Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = food.imageUrl,
                                contentDescription = food.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { Box(modifier = Modifier.background(AppSurface2)) },
                                error = { Box(modifier = Modifier.background(AppSurface2)) }
                            )
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.White),
                                            startY = 0f,
                                            endY = 850f
                                        )
                                    )
                            )
                            // Top Bar Icons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 50.dp, start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { navController.popBackStack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink900)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { /* toggle favorite */ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Ink900, modifier = Modifier.size(20.dp))
                                    }
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { /* more */ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Ink900, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Content
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(
                                text = food.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Ink900
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(VitalRadius.Pill)).background(AppSurface2).padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌐", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(food.brand ?: food.category ?: "Thực phẩm", fontSize = 13.sp, color = Ink900, fontWeight = FontWeight.Medium)
                                }
                                if (food.isVerified) {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(VitalRadius.Pill)).background(Mint50).padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✓", fontSize = 12.sp, color = Mint700, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đã xác thực", fontSize = 13.sp, color = Mint700, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Portion Selector
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VitalRadius.Xl),
                                colors = CardDefaults.cardColors(containerColor = AppSurface2)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Khẩu phần", fontSize = 12.sp, color = Ink500)
                                        Text(
                                            text = if (quantity == quantity.toLong().toFloat()) "${quantity.toInt()} phần (${servingG.toInt()}g)" else "$quantity phần (${(servingG*quantity).toInt()}g)",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink900
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(AppLine).clickable { if (quantity > 0.5f) quantity -= 0.5f },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Ink900)
                                        }
                                        Text(
                                            text = if (quantity == quantity.toLong().toFloat()) "${quantity.toInt()}" else "$quantity",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Ink900
                                        )
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Ink900).clickable { quantity += 0.5f },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Calories Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VitalRadius.Xl),
                                colors = CardDefaults.cardColors(containerColor = AppSurface),
                                border = BorderStroke(1.dp, AppLine)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Tổng năng lượng", fontSize = 13.sp, color = Ink500)
                                    Text(
                                        text = calories.toString(),
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Mint500
                                    )
                                    Text("kcal", fontSize = 14.sp, color = Ink500)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Macros
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MacroBox(label = "Carbs", value = "${carbs.toInt()}g", pct = "$carbPct%", color = MacroCarbs, modifier = Modifier.weight(1f))
                                MacroBox(label = "Protein", value = "${protein.toInt()}g", pct = "$proPct%", color = MacroProtein, modifier = Modifier.weight(1f))
                                MacroBox(label = "Fat", value = "${fat.toInt()}g", pct = "$fatPct%", color = MacroFat, modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Micronutrients
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VitalRadius.Xl),
                                colors = CardDefaults.cardColors(containerColor = AppSurface),
                                border = BorderStroke(1.dp, AppLine)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Vi chất dinh dưỡng", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    food.sodiumPer100g?.let {
                                        NutrientRow("Sodium", "${(it * factor).toInt()} mg", progress = 1f, color = Mint500)
                                    }
                                    food.sugarPer100g?.let {
                                        NutrientRow("Sugar", "${(it * factor).toInt()} g", progress = 1f, color = Mint500)
                                    }
                                    food.fiberPer100g?.let {
                                        NutrientRow("Fiber", "${(it * factor).toInt()} g", progress = 1f, color = Mint500)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(AppSurface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(VitalRadius.Lg))
                                .background(AppSurface2)
                                .clickable { /* view receipt/details */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Log", tint = Ink900)
                        }
                        
                        val mealTypeLabel = if (mealType.isNotBlank()) "vào bữa ${
                            when(mealType.lowercase()) {
                                "breakfast" -> "Sáng"
                                "lunch" -> "Trưa"
                                "dinner" -> "Tối"
                                else -> "Phụ"
                            }
                        }" else ""
                        
                        Button(
                            onClick = {
                                if (mealType.isNotBlank() && date.isNotBlank()) {
                                    viewModel.addToMealLog(
                                        mealType = mealType,
                                        date = date,
                                        foodId = food.id,
                                        quantity = quantity,
                                        servingUnit = food.servingUnit
                                    )
                                }
                            },
                            enabled = !uiState.isAdding,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(VitalRadius.Pill),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink900)
                        ) {
                            if (uiState.isAdding) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Thêm $mealTypeLabel +", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBox(label: String, value: String, pct: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface2)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = Ink500)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
            Text(pct, fontSize = 11.sp, color = Ink500)
        }
    }
}

@Composable
fun NutrientRow(label: String, value: String, progress: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Ink900)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(VitalRadius.Pill)).background(AppSurface2)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(100)).background(color))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink900, modifier = Modifier.width(70.dp))
        }
    }
}
