package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.VitalDisplayFontFamily
import com.vitalai.ui.theme.VitalFontFamily
import kotlin.math.roundToInt

@Composable
fun FoodDetailScreen(
    foodId: String,
    mealType: String = "",
    date: String = "",
    navController: NavController,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(foodId) {
        viewModel.loadFoodById(foodId)
        viewModel.loadFavorites()
    }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            snackbarHostState.showSnackbar("Đã thêm vào bữa ăn")
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
        containerColor = FoodModule.Cream
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FoodModule.Cream)
        ) {
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                uiState.error != null && uiState.selectedFood == null -> ErrorState(
                    message = uiState.error ?: "Không tìm thấy món ăn",
                    onRetry = { viewModel.loadFoodById(foodId) },
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val food = uiState.selectedFood ?: return@Scaffold
                    val isFavorite = uiState.favorites.any { it.id == food.id }
                    FoodDetailContent(
                        food = food,
                        isFavorite = isFavorite,
                        isFavoriteUpdating = uiState.isFavoriteUpdating,
                        isAdding = uiState.isAdding,
                        canAddToMeal = mealType.isNotBlank() && date.isNotBlank(),
                        mealType = mealType,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { viewModel.toggleFavorite(food.id) },
                        onAdd = { quantity, unit ->
                            viewModel.addToMealLog(
                                mealType = mealType,
                                date = date,
                                foodId = food.id,
                                quantity = quantity,
                                servingUnit = unit
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodDetailContent(
    food: FoodDto,
    isFavorite: Boolean,
    isFavoriteUpdating: Boolean,
    isAdding: Boolean,
    canAddToMeal: Boolean,
    mealType: String,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAdd: (Float, String) -> Unit
) {
    val servingG = food.servingSizeG.takeIf { it > 0f } ?: 100f
    val units = remember(food.id) {
        listOf(food.servingUnit.ifBlank { "phần" }, "g").distinct()
    }
    var unit by remember(food.id) { mutableStateOf(units.first()) }
    var amount by remember(food.id) { mutableFloatStateOf(if (unit == "g") servingG else 1f) }
    val grams = if (unit == "g") amount else amount * servingG
    val factor = grams / 100f
    val calories = food.caloriesPer100g * factor
    val carbs = food.carbsPer100g * factor
    val protein = food.proteinPer100g * factor
    val fat = food.fatPer100g * factor
    val energyTotal = carbs * 4f + protein * 4f + fat * 9f
    val carbsPct = if (energyTotal > 0f) (carbs * 4f / energyTotal * 100f).roundToInt() else 0
    val proteinPct = if (energyTotal > 0f) (protein * 4f / energyTotal * 100f).roundToInt() else 0
    val fatPct = if (energyTotal > 0f) (fat * 9f / energyTotal * 100f).roundToInt() else 0

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            FoodImageHeader(
                food = food,
                isFavorite = isFavorite,
                isFavoriteUpdating = isFavoriteUpdating,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite
            )
            Column(
                modifier = Modifier.padding(start = 21.dp, end = 21.dp, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    Text(
                        text = food.name,
                        color = FoodModule.Forest,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontFamily = VitalDisplayFontFamily
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FoodPill(
                            text = food.category ?: "Thực phẩm",
                            color = FoodModule.Charcoal,
                            borderColor = FoodModule.Border
                        )
                        if (food.isVerified) {
                            FoodPill(text = "Đã xác thực", background = FoodModule.Mint, icon = Icons.Default.Check)
                        }
                        FoodPill(
                            text = food.brand ?: "Generic",
                            color = FoodModule.Charcoal,
                            borderColor = FoodModule.Border
                        )
                    }
                }

                PortionCard(
                    unit = unit,
                    units = units,
                    amount = amount,
                    grams = grams,
                    onDecrease = { amount = (amount - if (unit == "g") 10f else 0.5f).coerceAtLeast(if (unit == "g") 10f else 0.5f) },
                    onIncrease = { amount += if (unit == "g") 10f else 0.5f },
                    onUnitChange = {
                        unit = it
                        amount = if (it == "g") grams.coerceAtLeast(10f) else (grams / servingG).coerceAtLeast(0.5f)
                    }
                )

                EnergyCard(calories = calories)

                Row(horizontalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxWidth()) {
                    FoodMacroCard("Carbs", carbs, carbsPct, Modifier.weight(1f))
                    FoodMacroCard("Protein", protein, proteinPct, Modifier.weight(1f))
                    FoodMacroCard("Fat", fat, fatPct, Modifier.weight(1f))
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(FoodModule.Cream)
                .border(1.dp, FoodModule.Border)
                .padding(start = 21.dp, end = 21.dp, top = 14.dp, bottom = 30.dp)
        ) {
            val cta = if (canAddToMeal) {
                "Thêm vào ${mealLabel(mealType)} · ${calories.roundToInt()} kcal"
            } else {
                "Chọn từ bữa ăn để thêm"
            }
            if (isAdding) {
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FoodModule.Forest)
                }
            } else {
                FoodPrimaryButton(
                    text = cta,
                    enabled = canAddToMeal && grams > 0f,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val requestQuantity = if (unit == "g") grams else amount
                        onAdd(requestQuantity, unit)
                    }
                )
            }
        }
    }
}

@Composable
private fun FoodImageHeader(
    food: FoodDto,
    isFavorite: Boolean,
    isFavoriteUpdating: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(FoodModule.Keylime)
    ) {
        SubcomposeAsyncImage(
            model = food.imageUrl,
            contentDescription = food.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FoodModule.Keylime),
                    contentAlignment = Alignment.Center
                ) {
                    FoodThumbFallback(name = food.name, size = 104.dp, circle = true)
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FoodModule.Keylime),
                    contentAlignment = Alignment.Center
                ) {
                    FoodThumbFallback(name = food.name, size = 104.dp, circle = true)
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Transparent,
                            FoodModule.Cream.copy(alpha = 0.96f)
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 21.dp, end = 21.dp, top = 56.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCircleButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = FoodModule.Forest, modifier = Modifier.size(20.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderCircleButton(enabled = !isFavoriteUpdating, onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = FoodModule.Forest,
                        modifier = Modifier.size(19.dp)
                    )
                }
                HeaderCircleButton(onClick = {}) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Thêm", tint = FoodModule.Forest, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PortionCard(
    unit: String,
    units: List<String>,
    amount: Float,
    grams: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onUnitChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = FoodModule.Keylime)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Khẩu phần", color = FoodModule.Charcoal, fontSize = 12.sp, fontFamily = VitalFontFamily)
                    Text(
                        text = if (unit == "g") "${grams.roundToInt()} g" else "${amount.cleanNumber()} phần · ${grams.roundToInt()} g",
                        color = FoodModule.Forest,
                        fontSize = 17.sp,
                        fontFamily = VitalFontFamily
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepButton(onClick = onDecrease, filled = false) {
                        Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = FoodModule.Forest, modifier = Modifier.size(18.dp))
                    }
                    Text(amount.cleanNumber(), color = FoodModule.Forest, fontSize = 19.sp, fontFamily = VitalFontFamily)
                    StepButton(onClick = onIncrease, filled = true) {
                        Icon(Icons.Default.Add, contentDescription = "Tăng", tint = FoodModule.Cream, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(1.dp)
                    .background(FoodModule.Forest.copy(alpha = 0.12f))
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                units.forEach { option ->
                    val selected = option == unit
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) FoodModule.Forest else FoodModule.Cream)
                            .border(1.dp, FoodModule.Border, RoundedCornerShape(999.dp))
                            .clickable { onUnitChange(option) }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, color = if (selected) FoodModule.Cream else FoodModule.Charcoal, fontSize = 13.sp, fontFamily = VitalFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyCard(calories: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = FoodModule.Cream),
        border = BorderStroke(1.dp, FoodModule.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 21.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tổng năng lượng", color = FoodModule.Charcoal, fontSize = 13.sp, fontFamily = VitalFontFamily)
            Text(
                calories.roundToInt().toString(),
                color = FoodModule.Forest,
                fontSize = 60.sp,
                lineHeight = 63.sp,
                fontFamily = VitalDisplayFontFamily
            )
            Text("kcal · theo khẩu phần", color = FoodModule.Charcoal, fontSize = 14.sp, fontFamily = VitalFontFamily)
        }
    }
}

@Composable
private fun FoodMacroCard(label: String, grams: Float, pct: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = FoodModule.Keylime)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(label, color = FoodModule.Charcoal, fontSize = 12.sp, fontFamily = VitalFontFamily)
            Text(
                "${grams.roundToInt()}g",
                color = FoodModule.Forest,
                fontSize = 19.sp,
                fontFamily = VitalFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$pct%", color = FoodModule.Charcoal.copy(alpha = 0.75f), fontSize = 12.sp, fontFamily = VitalFontFamily)
        }
    }
}

@Composable
private fun HeaderCircleButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(FoodModule.Cream)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun StepButton(onClick: () -> Unit, filled: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) FoodModule.Forest else Color.Transparent)
            .border(1.dp, FoodModule.Forest, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun mealLabel(mealType: String): String {
    return when (mealType.lowercase()) {
        "breakfast" -> "Bữa sáng"
        "lunch" -> "Bữa trưa"
        "dinner" -> "Bữa tối"
        "snack" -> "Bữa phụ"
        else -> "bữa ăn"
    }
}
