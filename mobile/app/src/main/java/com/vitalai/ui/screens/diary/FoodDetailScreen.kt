package com.vitalai.ui.screens.diary

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.screens.metrics.components.ScrollPicker
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

    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FoodModule.Cream,
        contentWindowInsets = WindowInsets(0)
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
    var showFullScreenImage by remember(food.displayImageUrl) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 300.dp)
        ) {
            FoodImageHeader(
                food = food,
                isFavorite = isFavorite,
                isFavoriteUpdating = isFavoriteUpdating,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite,
                onImageClick = { if (food.displayImageUrl != null) showFullScreenImage = true }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FoodHashTag(food.category ?: "Thực phẩm")
                        if (food.isVerified) {
                            FoodPill(text = "Đã xác thực", background = FoodModule.Mint, icon = Icons.Default.Check)
                        }
                        FoodHashTag(food.brand ?: "Generic")
                    }
                }

                NutritionDonutCard(
                    calories = calories,
                    carbs = carbs,
                    protein = protein,
                    fat = fat,
                    carbsPct = carbsPct,
                    proteinPct = proteinPct,
                    fatPct = fatPct
                )
            }
        }

        PortionPanel(
            unit = unit,
            units = units,
            amount = amount,
            grams = grams,
            mealType = mealType,
            canAddToMeal = canAddToMeal,
            isAdding = isAdding,
            onAmountChange = { amount = it },
            onUnitChange = { newUnit ->
                if (newUnit != unit) {
                    val currentGrams = grams
                    unit = newUnit
                    amount = if (newUnit == "g") {
                        (currentGrams / 5f).roundToInt().coerceAtLeast(1) * 5f
                    } else {
                        ((currentGrams / servingG) / 0.5f).roundToInt().coerceAtLeast(1) * 0.5f
                    }
                }
            },
            onAdd = {
                val requestQuantity = if (unit == "g") grams else amount
                onAdd(requestQuantity, unit)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFullScreenImage) {
        FullScreenFoodImage(
            imageUrl = food.displayImageUrl,
            contentDescription = food.name,
            onBack = { showFullScreenImage = false }
        )
    }
}

@Composable
private fun FoodImageHeader(
    food: FoodDto,
    isFavorite: Boolean,
    isFavoriteUpdating: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onImageClick: () -> Unit
) {
    val imageUrl = food.displayImageUrl
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(FoodModule.Keylime)
            .clickable(enabled = imageUrl != null, onClick = onImageClick)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = food.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FoodModule.Keylime),
                contentAlignment = Alignment.Center
            ) {
                FoodThumbFallback(name = food.name, size = 104.dp, circle = true)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 420f
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
            HeaderCircleButton(enabled = !isFavoriteUpdating, onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = FoodModule.Forest,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun FoodHashTag(text: String) {
    Text(
        text = "#$text",
        color = FoodModule.Forest,
        fontSize = 13.sp,
        fontFamily = VitalFontFamily
    )
}

@Composable
private fun NutritionDonutCard(
    calories: Float,
    carbs: Float,
    protein: Float,
    fat: Float,
    carbsPct: Int,
    proteinPct: Int,
    fatPct: Int
) {
    val segments = listOf(
        MacroSegment("Carbs", carbs, carbsPct, Color(0xFF49B85D)),
        MacroSegment("Chất đạm", protein, proteinPct, Color(0xFFFF6255)),
        MacroSegment("Chất béo", fat, fatPct, Color(0xFFFFCC5C))
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroDonut(
                calories = calories,
                segments = segments,
                modifier = Modifier.size(92.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                segments.forEach { segment ->
                    MacroLegendRow(segment)
                }
            }
        }
    }
}

@Composable
private fun MacroDonut(
    calories: Float,
    segments: List<MacroSegment>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(
                color = FoodModule.Border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            var startAngle = -90f
            segments.forEach { segment ->
                val sweep = 360f * (segment.percent / 100f)
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = calories.roundToInt().toString(),
                color = Color.Black,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontFamily = VitalDisplayFontFamily,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Kcal",
                color = Color.Black.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontFamily = VitalFontFamily
            )
        }
    }
}

@Composable
private fun MacroLegendRow(segment: MacroSegment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(segment.color)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${segment.label}:",
                modifier = Modifier.weight(1f, fill = false),
                color = Color.Black,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = VitalFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${String.format("%.2f", segment.grams)}g",
                color = Color.Black.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = VitalFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${segment.percent}%",
            color = Color.Black,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontFamily = VitalFontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FullScreenFoodImage(
    imageUrl: String?,
    contentDescription: String,
    onBack: () -> Unit
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            val previousStatusBarColor = dialogWindow?.statusBarColor
            val previousNavigationBarColor = dialogWindow?.navigationBarColor
            val controller = dialogWindow?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, dialogView).also { insetsController ->
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                dialogWindow?.let { window ->
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    if (previousStatusBarColor != null) window.statusBarColor = previousStatusBarColor
                    if (previousNavigationBarColor != null) window.navigationBarColor = previousNavigationBarColor
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 21.dp, top = 12.dp)
            ) {
                HeaderCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Thoát xem ảnh",
                        tint = FoodModule.Forest,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
private fun PortionPanel(
    unit: String,
    units: List<String>,
    amount: Float,
    grams: Float,
    mealType: String,
    canAddToMeal: Boolean,
    isAdding: Boolean,
    onAmountChange: (Float) -> Unit,
    onUnitChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amountStep = if (unit == "g") 5f else 0.5f
    val amountMax = if (unit == "g") 2000f else 20f
    val amountItems = remember(unit) {
        val steps = (amountMax / amountStep).roundToInt()
        (1..steps).map { i ->
            val value = i * amountStep
            if (unit == "g") value.roundToInt().toString() else value.cleanNumber()
        }
    }
    val amountIndex = ((amount / amountStep).roundToInt() - 1).coerceIn(0, amountItems.size - 1)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FoodModule.Cream,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 21.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Lựa chọn khẩu phần",
                color = FoodModule.Forest,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = VitalFontFamily
            )
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(44.dp * 3)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .height(44.dp)
                        .background(FoodModule.Keylime, RoundedCornerShape(12.dp))
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScrollPicker(
                        items = amountItems,
                        selectedIndex = amountIndex,
                        onSelectionChanged = { idx -> onAmountChange((idx + 1) * amountStep) },
                        modifier = Modifier.weight(1f)
                    )
                    ScrollPicker(
                        items = units,
                        selectedIndex = units.indexOf(unit).coerceAtLeast(0),
                        onSelectionChanged = { idx -> onUnitChange(units[idx]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            val cta = if (canAddToMeal) "Thêm vào ${mealLabel(mealType)}" else "Chọn từ bữa ăn để thêm"
            if (isAdding) {
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FoodModule.Forest)
                }
            } else {
                FoodPrimaryButton(
                    text = cta,
                    enabled = canAddToMeal && grams > 0f,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd
                )
            }
        }
    }
}

private data class MacroSegment(
    val label: String,
    val grams: Float,
    val percent: Int,
    val color: Color
)

private fun mealLabel(mealType: String): String {
    return when (mealType.lowercase()) {
        "breakfast" -> "Bữa sáng"
        "lunch" -> "Bữa trưa"
        "dinner" -> "Bữa tối"
        "snack" -> "Bữa phụ"
        else -> "bữa ăn"
    }
}
