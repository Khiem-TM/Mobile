package com.vitalai.ui.screens.metrics.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.screens.metrics.components.*
import com.vitalai.ui.screens.metrics.viewmodels.MetricsHistoryViewModel
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MetricsHistoryScreen(
    navController: NavController,
    viewModel: MetricsHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val totalItems = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.hasMore) {
            viewModel.loadMore()
        }
    }

    val filteredEvents = uiState.filteredEvents
    val grouped = remember(filteredEvents) { filteredEvents.groupBy { it.monthGroup } }
    val allMetrics = remember(filteredEvents) { filteredEvents.mapNotNull { it.rawMetric } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử thay đổi", fontWeight = FontWeight.Bold, color = ForestGreen) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = ForestGreen)
                    }
                },
                actions = {
                    val filterLabel = when {
                        uiState.selectedYear != null && uiState.selectedMonth != null ->
                            "Tháng ${uiState.selectedMonth}/${uiState.selectedYear}"
                        uiState.selectedYear != null -> "${uiState.selectedYear}"
                        uiState.selectedMonth != null -> "Tháng ${uiState.selectedMonth}"
                        else -> null
                    }
                    val isFiltering = filterLabel != null
                    if (filterLabel != null) {
                        Text(
                            text = filterLabel,
                            color = ForestGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = {
                        if (isFiltering) viewModel.setDateFilter(null, null)
                        else showFilterSheet = true
                    }) {
                        Icon(
                            if (isFiltering) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                            contentDescription = if (isFiltering) "Tắt lọc" else "Lọc",
                            tint = ForestGreen
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppMutedBackground,
                    scrolledContainerColor = AppMutedBackground
                )
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                repeat(5) { SkeletonRow() }
            }
        } else if (uiState.events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error ?: "Chưa có lịch sử số liệu", color = Ink500, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
            ) {
                item {
                    val summary = uiState.summary
                    val changeVal = summary?.weightChange ?: 0f
                    val changeText = when {
                        changeVal == 0f -> "0 kg"
                        changeVal < 0f -> "↓${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(changeVal))} kg"
                        else -> "↑${String.format(java.util.Locale.US, "%.1f", changeVal)} kg"
                    }
                    val changeColor = when {
                        changeVal < 0f -> ForestGreen
                        changeVal > 0f -> Color(0xFFF87171)
                        else -> Ink500
                    }
                    val currentWeight = if ((summary?.currentWeight ?: 0f) > 0f) {
                        val w = summary!!.currentWeight
                        if (w % 1f == 0f) "${w.toInt()} kg" else String.format(java.util.Locale.US, "%.1f kg", w)
                    } else "--"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(VitalRadius.Lg),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = VitalElevation.Level1)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCell(label = "Cân nặng hiện tại", value = currentWeight, color = ForestGreen)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppLine).align(Alignment.CenterVertically))
                            StatCell(label = "Tổng thay đổi", value = changeText, color = changeColor)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppLine).align(Alignment.CenterVertically))
                            StatCell(label = "Số bản ghi", value = "${filteredEvents.size}", color = ForestGreen)
                        }
                    }
                }

                if (uiState.photos.isNotEmpty()) {
                    item {
                        Text(
                            "Ảnh tiến độ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Ink900,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.photos, key = { it.id }) { photo ->
                                AsyncImage(
                                    model = photo.photoUrl,
                                    contentDescription = photo.photoType,
                                    modifier = Modifier
                                        .size(width = 120.dp, height = 150.dp)
                                        .clip(RoundedCornerShape(VitalRadius.Lg)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                grouped.forEach { (month, events) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppMutedBackground)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(month, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MacroWater)
                        }
                    }

                    itemsIndexed(events) { idx, event ->
                        var showDetail by remember { mutableStateOf(false) }
                        TimelineEventCard(
                            event = event,
                            isFirst = idx == 0,
                            isLast = idx == events.size - 1,
                            onDoubleClick = { showDetail = true }
                        )
                        if (showDetail && event.rawMetric != null) {
                            val idx = allMetrics.indexOfFirst { it.id == event.rawMetric.id }
                            val previousMetric = allMetrics.getOrNull(idx + 1)
                            MetricDetailDialog(
                                metric = event.rawMetric,
                                photoUrls = event.photoUrls,
                                previousMetric = previousMetric,
                                onDismiss = { showDetail = false }
                            )
                        }
                    }
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Mint500, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            selectedYear = uiState.selectedYear,
            selectedMonth = uiState.selectedMonth,
            availableYears = uiState.availableYears,
            onApply = { y, m -> viewModel.setDateFilter(y, m) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    selectedYear: Int?,
    selectedMonth: Int?,
    availableYears: List<Int>,
    onApply: (Int?, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentYear by remember { mutableStateOf(selectedYear) }
    var currentMonth by remember { mutableStateOf(selectedMonth) }

    val yearItems = listOf("Tất cả") + availableYears.map { it.toString() }
    val monthItems = listOf("Tất cả") + (1..12).map { "Tháng $it" }

    val yearIndex = if (selectedYear == null) 0
        else (availableYears.indexOf(selectedYear) + 1).coerceAtLeast(0)
    val monthIndex = selectedMonth ?: 0

    val hasFilter = currentYear != null || currentMonth != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) },
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Lọc bản ghi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ForestGreen
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(30.dp)
                        .alpha(if (hasFilter) 1f else 0f)
                        .background(Color(0xFFF87171), CircleShape)
                        .clickable(enabled = hasFilter) { onApply(null, null); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Bỏ lọc",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(44.dp * 3)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .height(44.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScrollPicker(
                        items = yearItems,
                        selectedIndex = yearIndex,
                        onSelectionChanged = { idx ->
                            currentYear = if (idx == 0) null else availableYears.getOrNull(idx - 1)
                            onApply(currentYear, currentMonth)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ScrollPicker(
                        items = monthItems,
                        selectedIndex = monthIndex,
                        onSelectionChanged = { idx ->
                            currentMonth = if (idx == 0) null else idx
                            onApply(currentYear, currentMonth)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
