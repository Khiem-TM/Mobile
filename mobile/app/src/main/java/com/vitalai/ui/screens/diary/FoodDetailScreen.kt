package com.vitalai.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedFood?.name ?: "Chi tiết món ăn") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground
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

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 80.dp)
                    ) {
                        if (food.imageUrl != null) {
                            AsyncImage(
                                model = food.imageUrl,
                                contentDescription = food.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Mint50),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍽️", fontSize = 60.sp)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = food.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink900
                            )
                            food.brand?.let {
                                Text(text = it, fontSize = 14.sp, color = Ink500)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AppSurface),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (quantity > 0.5f) quantity -= 0.5f },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Ink100)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = Ink700)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (quantity == quantity.toLong().toFloat())
                                                "${quantity.toInt()} phần"
                                            else
                                                "$quantity phần",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Ink900
                                        )
                                        Text(
                                            text = "(${(quantity * servingG).toInt()}g)",
                                            fontSize = 12.sp,
                                            color = Ink500
                                        )
                                    }
                                    IconButton(
                                        onClick = { quantity += 0.5f },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Mint500)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tăng", tint = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = calories.toString(),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Mint500
                                    )
                                    Text(text = "kcal", fontSize = 16.sp, color = Ink500)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MacroChip("Carbs", "${carbs.toInt()}g", MacroCarbs, modifier = Modifier.weight(1f))
                                MacroChip("Protein", "${protein.toInt()}g", MacroProtein, modifier = Modifier.weight(1f))
                                MacroChip("Chất béo", "${fat.toInt()}g", MacroFat, modifier = Modifier.weight(1f))
                            }

                            food.fiberPer100g?.let {
                                Spacer(modifier = Modifier.height(12.dp))
                                NutrientRow("Chất xơ", "${(it * factor).toInt()}g")
                            }
                            food.sugarPer100g?.let {
                                NutrientRow("Đường", "${(it * factor).toInt()}g")
                            }
                            food.sodiumPer100g?.let {
                                NutrientRow("Natri", "${(it * factor).toInt()}mg")
                            }
                        }
                    }

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
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint900)
                    ) {
                        if (uiState.isAdding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (mealType.isNotBlank()) "Thêm vào bữa" else "Xem chi tiết",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(text = label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun NutrientRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Ink700)
        Text(text = value, fontSize = 13.sp, color = Ink900, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = Ink200, thickness = 0.5.dp)
}
