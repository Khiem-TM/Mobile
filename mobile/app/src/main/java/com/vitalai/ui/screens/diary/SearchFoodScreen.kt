package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.BottomSheetGrabber
import com.vitalai.ui.theme.VitalDisplayFontFamily
import com.vitalai.ui.theme.VitalFontFamily
import kotlin.math.roundToInt

@Composable
fun SearchFoodScreen(
    navController: NavController,
    mealType: String = "",
    date: String = "",
    initialTab: Int = 0,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember(initialTab) {
        mutableIntStateOf(
            when (initialTab) {
                2 -> 2
                3 -> 1
                else -> 0
            }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val canQuickAdd = mealType.isNotBlank() && date.isNotBlank()

    LaunchedEffect(Unit) {
        viewModel.loadAllFoods()
        viewModel.loadCustomFoods()
        viewModel.loadExploreFoods()
        viewModel.loadFavorites()
    }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            viewModel.clearAddSuccess()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FoodModule.Forest.copy(alpha = 0.28f)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FoodModule.Forest.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { navController.popBackStack() }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            SearchBottomSheet(
                uiState = uiState,
                selectedTab = selectedTab,
                onTabChange = {
                    selectedTab = it
                    viewModel.search("")
                },
                onQueryChange = viewModel::search,
                canQuickAdd = canQuickAdd,
                mealType = mealType,
                onClose = { navController.popBackStack() },
                onScan = { navController.navigate(Screen.Scan) },
                onCreateCustom = { navController.navigate(Screen.CreateFood) },
                onFoodClick = { food ->
                    navController.navigate(Screen.FoodDetail(id = food.id, mealType = mealType, date = date))
                },
                onQuickAdd = { food ->
                    if (canQuickAdd) viewModel.addToMealLog(mealType, date, food.id, 1f, "phần")
                    else navController.navigate(Screen.FoodDetail(id = food.id))
                },
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            )
        }
    }
}

@Composable
private fun SearchBottomSheet(
    uiState: FoodUiState,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    canQuickAdd: Boolean,
    mealType: String,
    onClose: () -> Unit,
    onScan: () -> Unit,
    onCreateCustom: () -> Unit,
    onFoodClick: (FoodDto) -> Unit,
    onQuickAdd: (FoodDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val mealLabel = when (mealType.lowercase()) {
        "breakfast" -> "Bữa sáng"
        "lunch" -> "Bữa trưa"
        "dinner" -> "Bữa tối"
        "snack" -> "Bữa phụ"
        else -> "bữa ăn"
    }
    val displayItems = when {
        uiState.query.isNotBlank() -> uiState.searchResults
        selectedTab == 1 -> uiState.favorites
        selectedTab == 2 -> uiState.customFoods
        else -> uiState.exploreFoods.ifEmpty { uiState.allFoods }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(FoodModule.Cream)
    ) {
        Column(modifier = Modifier.padding(start = 21.dp, end = 21.dp, top = 10.dp)) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(BottomSheetGrabber)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (canQuickAdd) "Thêm vào $mealLabel" else "Tìm món ăn",
                    color = FoodModule.Forest,
                    fontSize = 24.sp,
                    fontFamily = VitalDisplayFontFamily
                )
                RoundSheetIconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = FoodModule.Forest, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            SearchInputRow(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onScan = onScan
            )
            Spacer(Modifier.height(14.dp))
            SearchTabs(selectedTab = selectedTab, onTabChange = onTabChange)
        }

        when {
            selectedTab == 2 && uiState.query.isBlank() && uiState.customFoods.isEmpty() -> {
                CustomFoodsEmpty(onCreateCustom = onCreateCustom, modifier = Modifier.weight(1f))
            }
            uiState.isSearching || (selectedTab == 0 && uiState.query.isBlank() && uiState.isLoadingAll) -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FoodModule.Forest)
                }
            }
            displayItems.isEmpty() -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.query.isBlank()) "Chưa có món ăn" else "Không tìm thấy \"${uiState.query}\".",
                        color = FoodModule.Charcoal,
                        fontSize = 14.sp,
                        fontFamily = VitalFontFamily
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 16.dp, bottom = 28.dp)
                ) {
                    item {
                        Text(
                            text = when {
                                uiState.query.isNotBlank() -> "${displayItems.size} kết quả"
                                selectedTab == 1 -> "Món yêu thích"
                                selectedTab == 2 -> "Món của tôi"
                                else -> "Gần đây"
                            }.uppercase(),
                            color = FoodModule.Charcoal,
                            fontSize = 12.sp,
                            fontFamily = VitalFontFamily,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(displayItems, key = { it.id }) { food ->
                        FoodResultRow(
                            food = food,
                            onClick = { onFoodClick(food) },
                            onQuickAdd = { onQuickAdd(food) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInputRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(FoodModule.CardRadius))
            .border(1.dp, FoodModule.Border, RoundedCornerShape(FoodModule.CardRadius))
            .background(FoodModule.Cream)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = FoodModule.Forest, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            singleLine = true,
            textStyle = TextStyle(color = FoodModule.Charcoal, fontSize = 15.sp),
            cursorBrush = SolidColor(FoodModule.Forest),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Tìm món ăn, thương hiệu...",
                            color = FoodModule.Charcoal.copy(alpha = 0.68f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(FoodModule.Border)
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "AI Scan", tint = FoodModule.Forest, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SearchTabs(selectedTab: Int, onTabChange: (Int) -> Unit) {
    val tabs = listOf("Gần đây", "Yêu thích", "Món của tôi")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) FoodModule.Forest else FoodModule.Cream)
                    .border(1.dp, if (selected) FoodModule.Forest else FoodModule.Border, RoundedCornerShape(999.dp))
                    .clickable { onTabChange(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) FoodModule.Cream else FoodModule.Charcoal,
                    fontSize = 13.sp,
                    fontFamily = VitalFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FoodResultRow(food: FoodDto, onClick: () -> Unit, onQuickAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FoodThumb(name = food.name, imageUrl = food.displayImageUrl, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                food.name,
                color = FoodModule.Ink,
                fontSize = 14.sp,
                fontFamily = VitalFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                food.brand ?: food.category ?: "Thực phẩm",
                color = FoodModule.Charcoal.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontFamily = VitalFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            FoodPill(
                text = "${food.servingSizeG.roundToInt()} ${food.servingUnit} · ${((food.caloriesPer100g * food.servingSizeG) / 100f).roundToInt()} kcal",
                background = FoodModule.MintKiss,
                color = FoodModule.Forest
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(FoodModule.Forest)
                .clickable(onClick = onQuickAdd),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm", tint = FoodModule.Cream, modifier = Modifier.size(18.dp))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(FoodModule.Border))
}

@Composable
private fun CustomFoodsEmpty(onCreateCustom: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Chưa có món tự tạo", color = FoodModule.Forest, fontSize = 16.sp, fontFamily = VitalFontFamily)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tạo công thức hoặc lưu món ăn riêng\nkhông có trong cơ sở dữ liệu.",
            color = FoodModule.Charcoal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = VitalFontFamily
        )
        Spacer(Modifier.height(20.dp))
        FoodPrimaryButton(text = "Tạo món mới", modifier = Modifier.width(180.dp), onClick = onCreateCustom)
    }
}

@Composable
private fun RoundSheetIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(FoodModule.Cream)
            .border(1.dp, FoodModule.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
