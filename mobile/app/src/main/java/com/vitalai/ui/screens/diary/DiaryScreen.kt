package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SwipeToDeleteRow
import com.vitalai.ui.theme.VitalDisplayFontFamily
import com.vitalai.ui.theme.VitalFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val DailyCalorieGoal = 2200f
private const val ProteinGoal = 140f
private const val CarbGoal = 248f
private const val FatGoal = 73f

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingItem by remember { mutableStateOf<Triple<String, MealLogItemDto, String>?>(null) }
    var showCoach by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodModule.Cream)
    ) {
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadMealLogs,
                modifier = Modifier.fillMaxSize()
            )
            else -> DiaryContent(
                selectedDate = uiState.selectedDate,
                mealLogs = uiState.mealLogs,
                summary = uiState.summary,
                onPrevDate = {
                    val prev = LocalDate.parse(uiState.selectedDate).minusDays(1)
                    viewModel.selectDate(prev.format(DateTimeFormatter.ISO_LOCAL_DATE))
                },
                onNextDate = {
                    val next = LocalDate.parse(uiState.selectedDate).plusDays(1)
                    viewModel.selectDate(next.format(DateTimeFormatter.ISO_LOCAL_DATE))
                },
                onAddMeal = { mealType ->
                    navController.navigate(Screen.SearchFood(mealType = mealType, date = uiState.selectedDate))
                },
                onOpenItem = { mealLogId, item -> editingItem = Triple(mealLogId, item, item.servingUnit) },
                onDeleteItem = viewModel::deleteItem,
                onOpenCoach = { showCoach = true }
            )
        }

        if (showCoach) {
            CoachTipModal(onClose = { showCoach = false })
        }
    }

    editingItem?.let { (mealLogId, item, _) ->
        var quantityText by remember(item.id) { mutableStateOf(item.quantity.cleanNumber()) }
        var unitText by remember(item.id) { mutableStateOf(item.servingUnit) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Chỉnh khẩu phần") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Số lượng") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Đơn vị") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = quantityText.toFloatOrNull()
                        if (quantity != null && quantity > 0f) {
                            viewModel.editItem(mealLogId, item.id, quantity, unitText.ifBlank { item.servingUnit })
                            editingItem = null
                        }
                    }
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun DiaryContent(
    selectedDate: String,
    mealLogs: List<MealLogDto>,
    summary: MealLogSummaryDto?,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onAddMeal: (String) -> Unit,
    onOpenItem: (String, MealLogItemDto) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    onOpenCoach: () -> Unit
) {
    val mealTypes = listOf(
        MealSpec("breakfast", "Bữa sáng"),
        MealSpec("lunch", "Bữa trưa"),
        MealSpec("dinner", "Bữa tối"),
        MealSpec("snack", "Bữa phụ")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodModule.Cream),
        contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 28.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp)
    ) {
        item {
            DiaryDateHeader(
                selectedDate = selectedDate,
                onPrevDate = onPrevDate,
                onNextDate = onNextDate
            )
        }
        item {
            DiarySummaryCard(summary = summary)
        }
        item {
            CoachNudgeCard(summary = summary, onClick = onOpenCoach)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                mealTypes.forEach { meal ->
                    val log = mealLogs.find { it.mealType.equals(meal.key, ignoreCase = true) }
                    FoodMealSection(
                        meal = meal,
                        mealLog = log,
                        onAdd = { onAddMeal(meal.key) },
                        onOpenItem = onOpenItem,
                        onDeleteItem = onDeleteItem
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryDateHeader(
    selectedDate: String,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit
) {
    val date = LocalDate.parse(selectedDate)
    val today = LocalDate.now()
    val label = if (date == today) {
        "Hôm nay, ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale("vi")))}"
    } else {
        date.format(DateTimeFormatter.ofPattern("d MMM, yyyy", Locale("vi")))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Nhật ký dinh dưỡng",
                color = FoodModule.Charcoal,
                fontSize = 12.sp,
                fontFamily = VitalFontFamily
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = FoodModule.Forest,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontFamily = VitalDisplayFontFamily,
                fontWeight = FontWeight.Normal
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundOutlineIconButton(onClick = onPrevDate) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Ngày trước", tint = FoodModule.Forest, modifier = Modifier.size(17.dp))
            }
            RoundOutlineIconButton(onClick = onNextDate) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Ngày sau", tint = FoodModule.Forest, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun DiarySummaryCard(summary: MealLogSummaryDto?) {
    val consumed = summary?.totalCalories ?: 0f
    Card(
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = FoodModule.Keylime),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(21.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CalorieRing(consumed = consumed, goal = DailyCalorieGoal)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    SummaryRow("Mục tiêu", DailyCalorieGoal.roundToInt())
                    Hairline()
                    SummaryRow("Đã nạp", consumed.roundToInt())
                }
            }
            Spacer(Modifier.height(18.dp))
            Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MacroChip("Protein", summary?.totalProtein ?: 0f, ProteinGoal, Modifier.weight(1f))
                MacroChip("Carbs", summary?.totalCarbs ?: 0f, CarbGoal, Modifier.weight(1f))
                MacroChip("Fat", summary?.totalFat ?: 0f, FatGoal, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(label, color = FoodModule.Charcoal, fontSize = 13.sp, fontFamily = VitalFontFamily)
        Text(
            text = "$value kcal",
            color = FoodModule.Forest,
            fontSize = 16.sp,
            fontFamily = VitalFontFamily
        )
    }
}

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FoodModule.Forest.copy(alpha = 0.12f))
    )
}

@Composable
private fun MacroChip(label: String, value: Float, goal: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = FoodModule.Charcoal, fontSize = 11.sp, fontFamily = VitalFontFamily)
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${value.roundToInt()}/${goal.roundToInt()}g",
            color = FoodModule.Forest,
            fontSize = 16.sp,
            fontFamily = VitalFontFamily
        )
        Spacer(Modifier.height(6.dp))
        FoodProgressBar(progress = value / goal, height = 5.dp)
    }
}

@Composable
private fun CoachNudgeCard(summary: MealLogSummaryDto?, onClick: () -> Unit) {
    val proteinLeft = (ProteinGoal - (summary?.totalProtein ?: 0f)).coerceAtLeast(0f).roundToInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FoodModule.CardRadius))
            .background(FoodModule.Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(FoodModule.Cream),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = FoodModule.Forest, fontSize = 13.sp, fontFamily = VitalFontFamily)
        }
        Column(Modifier.weight(1f)) {
            Text("Vital Coach có gợi ý", color = FoodModule.Forest, fontSize = 14.sp, fontFamily = VitalFontFamily)
            Text(
                text = "Bạn còn thiếu khoảng ${proteinLeft}g protein hôm nay ->",
                color = FoodModule.Charcoal,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = VitalFontFamily
            )
        }
    }
}

@Composable
private fun FoodMealSection(
    meal: MealSpec,
    mealLog: MealLogDto?,
    onAdd: () -> Unit,
    onOpenItem: (String, MealLogItemDto) -> Unit,
    onDeleteItem: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = CardDefaults.cardColors(containerColor = FoodModule.Cream),
        border = BorderStroke(1.dp, FoodModule.Border)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.label, color = FoodModule.Ink, fontSize = 16.sp, fontFamily = VitalFontFamily)
                    if ((mealLog?.items?.size ?: 0) > 0) {
                        Text(
                            text = " · ${mealLog!!.totalCalories.roundToInt()} kcal",
                            color = FoodModule.Charcoal.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontFamily = VitalFontFamily
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, FoodModule.Forest, CircleShape)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm món", tint = FoodModule.Forest, modifier = Modifier.size(17.dp))
                }
            }

            if (mealLog == null || mealLog.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(0.dp, FoodModule.Border)
                        .clickable(onClick = onAdd)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Thêm món đầu tiên...", color = FoodModule.Charcoal, fontSize = 13.sp, fontFamily = VitalFontFamily)
                }
            } else {
                mealLog.items.forEach { item ->
                    MealFoodRow(
                        mealLogId = mealLog.id,
                        item = item,
                        onOpenItem = onOpenItem,
                        onDeleteItem = onDeleteItem
                    )
                }
            }
        }
    }
}

@Composable
private fun MealFoodRow(
    mealLogId: String,
    item: MealLogItemDto,
    onOpenItem: (String, MealLogItemDto) -> Unit,
    onDeleteItem: (String, String) -> Unit
) {
    SwipeToDeleteRow(
        onDelete = { onDeleteItem(mealLogId, item.id) },
        modifier = Modifier
            .fillMaxWidth()
            .border(0.dp, FoodModule.Border),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FoodModule.Cream)
                .clickable { onOpenItem(mealLogId, item) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FoodThumb(name = item.foodName.ifBlank { "Món ăn" }, imageUrl = item.imageUrl, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    item.foodName.ifBlank { "Món ăn" },
                    color = FoodModule.Ink,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = VitalFontFamily
                )
                Text(
                    "${item.quantity.cleanNumber()} ${item.servingUnit}",
                    color = FoodModule.Charcoal.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontFamily = VitalFontFamily
                )
            }
            Text(
                text = "${item.calories.roundToInt()} kcal",
                color = FoodModule.Forest,
                fontSize = 14.sp,
                fontFamily = VitalFontFamily
            )
        }
    }
}

@Composable
private fun CoachTipModal(onClose: () -> Unit) {
    val tips = listOf(
        CoachTip("Sữa chua Hy Lạp", "150g · khoảng 90 kcal · giàu protein", "Phù hợp cho bữa phụ khi cần tăng protein nhẹ."),
        CoachTip("Ức gà áp chảo", "120g · khoảng 198 kcal · protein nạc", "Giúp cân bằng bữa tối mà không vượt nhiều calorie."),
        CoachTip("Rau xanh", "60g · ít calorie", "Thêm chất xơ và vi chất cho đĩa ăn hôm nay.")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodModule.Forest.copy(alpha = 0.34f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FoodModule.Cream)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FoodModule.Slate),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AI", color = FoodModule.Forest, fontSize = 13.sp, fontFamily = VitalFontFamily)
                        }
                        Column {
                            Text("Vital Coach", color = FoodModule.Forest, fontSize = 26.sp, lineHeight = 26.sp, fontFamily = VitalDisplayFontFamily)
                            Text("Gợi ý cho hôm nay", color = FoodModule.Charcoal, fontSize = 12.sp, fontFamily = VitalFontFamily)
                        }
                    }
                    RoundOutlineIconButton(size = 32.dp, onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = FoodModule.Forest, modifier = Modifier.size(17.dp))
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(FoodModule.CardRadius))
                            .background(FoodModule.Slate)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Bạn có thể ưu tiên protein nạc và thêm rau xanh để hoàn thiện bữa ăn trong ngày.",
                            color = FoodModule.Charcoal,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontFamily = VitalFontFamily
                        )
                    }
                    Text("Đề xuất", color = FoodModule.Charcoal, fontSize = 12.sp, fontFamily = VitalFontFamily)
                    tips.forEach { tip ->
                        CoachTipRow(tip)
                    }
                    Text(
                        text = "Gợi ý chỉ mang tính tham khảo, không thay thế tư vấn y tế.",
                        color = FoodModule.Charcoal.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = VitalFontFamily,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachTipRow(tip: CoachTip) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FoodModule.CardRadius))
            .border(1.dp, FoodModule.Border, RoundedCornerShape(FoodModule.CardRadius))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FoodThumbFallback(name = tip.title, size = 44.dp, circle = true)
            Column(Modifier.weight(1f)) {
                Text(tip.title, color = FoodModule.Ink, fontSize = 15.sp, fontFamily = VitalFontFamily)
                Text(tip.meta, color = FoodModule.Charcoal, fontSize = 12.sp, fontFamily = VitalFontFamily)
            }
            FoodPill(text = "Gợi ý", background = FoodModule.Keylime)
        }
        Text(tip.reason, color = FoodModule.Charcoal, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = VitalFontFamily)
        FoodPill(text = "An toàn hồ sơ", background = FoodModule.Mint, icon = Icons.Default.Check)
    }
}

@Composable
private fun RoundOutlineIconButton(size: androidx.compose.ui.unit.Dp = 34.dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(FoodModule.Cream)
            .border(1.dp, FoodModule.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class MealSpec(val key: String, val label: String)
private data class CoachTip(val title: String, val meta: String, val reason: String)
