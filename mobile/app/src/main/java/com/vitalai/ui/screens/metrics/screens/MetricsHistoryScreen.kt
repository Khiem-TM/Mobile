package com.vitalai.ui.screens.metrics.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitalai.ui.screens.metrics.viewmodels.MetricEventType
import com.vitalai.ui.screens.metrics.viewmodels.MetricTimelineEvent
import com.vitalai.ui.screens.metrics.viewmodels.MetricsHistoryViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.theme.*
import com.vitalai.ui.theme.ForestGreen
import com.vitalai.ui.theme.AppMutedBackground

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

    // Load more when near end
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

    // Group filtered events by month
    val grouped = uiState.filteredEvents.groupBy { it.monthGroup }

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
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Lọc",
                            tint = if (uiState.activeFilter != null) ForestGreen else ForestGreen.copy(alpha = 0.5f)
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
                // Stats card as first item
                item {
                    val changeVal = uiState.weightChange
                    val changeText = if (changeVal == 0f) "0 kg" else "%+.1f kg".format(changeVal)
                    val changeColor = when {
                        changeVal < 0f -> ForestGreen
                        changeVal > 0f -> Color(0xFFF87171)
                        else -> Ink500
                    }
                    val currentWeight = if (uiState.currentWeightKg > 0f)
                        "%.1f kg".format(uiState.currentWeightKg) else "--"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(4.dp, RoundedCornerShape(VitalRadius.Lg), clip = false)
                            .clip(RoundedCornerShape(VitalRadius.Lg))
                            .background(AppSurface)
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCell(label = "Cân nặng hiện tại", value = currentWeight, color = ForestGreen)
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppLine).align(Alignment.CenterVertically))
                        StatCell(label = "Tổng thay đổi", value = changeText, color = changeColor)
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppLine).align(Alignment.CenterVertically))
                        StatCell(label = "Số bản ghi", value = "${uiState.totalRecords}", color = ForestGreen)
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
                        // Sticky month header
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppMutedBackground)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    month,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ForestGreen
                                )
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
                                MetricDetailDialog(
                                    metric = event.rawMetric,
                                    photoUrls = event.photoUrls,
                                    onDismiss = { showDetail = false }
                                )
                            }
                        }
                    }

                    // Load more indicator
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
            activeFilter = uiState.activeFilter,
            onSelect = { type ->
                viewModel.setFilter(type)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    activeFilter: MetricEventType?,
    onSelect: (MetricEventType?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = listOf(
        null to "Tất cả",
        MetricEventType.WEIGHT to "Cân nặng",
        MetricEventType.MEASUREMENT to "Số đo cơ thể"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Lọc theo loại cập nhật",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ForestGreen,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            options.forEach { (type, label) ->
                val selected = activeFilter == type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VitalRadius.Md))
                        .background(if (selected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { onSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        color = if (selected) ForestGreen else Ink700,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selected) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Ink500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun MetricDetailDialog(
    metric: com.vitalai.data.remote.model.BodyMetricDto,
    photoUrls: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chi tiết chỉ số", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (photoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Ảnh tiến độ",
                                modifier = Modifier
                                    .size(width = 112.dp, height = 138.dp)
                                    .clip(RoundedCornerShape(VitalRadius.Md)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VitalRadius.Md))
                            .background(AppSurface2)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có ảnh gắn với bản ghi này", color = Ink500, fontSize = 12.sp)
                    }
                }
                DetailRow("Ngày đo", metric.date)
                DetailRow("Cân nặng", "%.1f kg".format(metric.weightKg))
                metric.heightCm?.let { DetailRow("Chiều cao", "%.1f cm".format(it)) }
                metric.bmi?.let { DetailRow("BMI", "%.1f".format(it)) }
                metric.bmr?.let { DetailRow("BMR", "%.0f kcal/ngày".format(it)) }
                metric.tdee?.let { DetailRow("TDEE", "%.0f kcal/ngày".format(it)) }
                metric.bodyFatPct?.let { DetailRow("Tỷ lệ mỡ", "%.1f%%".format(it)) }
                metric.waistCm?.let { DetailRow("Vòng eo", "%.1f cm".format(it)) }
                metric.hipCm?.let { DetailRow("Vòng mông", "%.1f cm".format(it)) }
                metric.chestCm?.let { DetailRow("Vòng ngực", "%.1f cm".format(it)) }
                metric.armCm?.let { DetailRow("Bắp tay", "%.1f cm".format(it)) }
                metric.neckCm?.let { DetailRow("Vòng cổ", "%.1f cm".format(it)) }
                metric.notes?.let { if (it.isNotBlank()) DetailRow("Ghi chú", it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = Mint500) }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Ink500)
        Text(value, fontSize = 13.sp, color = Ink900, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineEventCard(
    event: MetricTimelineEvent,
    isFirst: Boolean,
    isLast: Boolean,
    onDoubleClick: () -> Unit = {}
) {
    val nodeColor = when (event.type) {
        MetricEventType.WEIGHT -> Mint500
        MetricEventType.PHOTO -> MacroFat
        MetricEventType.MEASUREMENT -> MacroWater
        MetricEventType.WORKOUT -> MacroCarbs
        MetricEventType.BADGE -> Color(0xFFD97706)
    }
    val nodeIcon = when (event.type) {
        MetricEventType.WEIGHT -> Icons.Default.MonitorWeight
        MetricEventType.PHOTO -> Icons.Default.CameraAlt
        MetricEventType.MEASUREMENT -> Icons.Default.AccessibilityNew
        MetricEventType.WORKOUT -> Icons.Default.FitnessCenter
        MetricEventType.BADGE -> Icons.Default.Star
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
    ) {
        // Timeline line + node
        Box(modifier = Modifier.width(40.dp)) {
            // Vertical line
            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.CenterStart).padding(start = 19.dp)) {
                drawRect(
                    color = Ink200,
                    topLeft = Offset(0f, if (isFirst) size.height * 0.4f else 0f),
                    size = Size(2f, if (isLast) size.height * 0.4f else size.height)
                )
            }
            // Node
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = nodeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Event card (double-click → detail)
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp)
                .combinedClickable(onClick = {}, onDoubleClick = onDoubleClick),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (event.type == MetricEventType.MEASUREMENT) MacroWater else ForestGreen
                    )
                    Text(event.date, fontSize = 11.sp, color = Ink500)
                }
                Spacer(Modifier.height(4.dp))
                Text(event.value, fontSize = 14.sp, color = nodeColor, fontWeight = FontWeight.Bold)
                event.note?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 11.sp, color = Ink500)
                }
                event.photoUrl?.let { url ->
                    Spacer(Modifier.height(6.dp))
                    AsyncImage(
                        model = url,
                        contentDescription = "Ảnh",
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Ink200))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Ink200))
            Box(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Ink100))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MetricsHistoryScreenPreview() {
    val mockEvents = listOf(
        MetricTimelineEvent(
            id = "1",
            type = MetricEventType.WEIGHT,
            title = "Cập nhật cân nặng",
            value = "77.1 kg",
            note = "Giảm mỡ tốt",
            photoUrl = null,
            date = "10 Thg 5, 2026",
            monthGroup = "Tháng 5, 2026"
        ),
        MetricTimelineEvent(
            id = "2",
            type = MetricEventType.PHOTO,
            title = "Ảnh tiến độ",
            value = "Mặt trước",
            note = null,
            photoUrl = "https://example.com/photo.jpg",
            date = "05 Thg 5, 2026",
            monthGroup = "Tháng 5, 2026"
        ),
        MetricTimelineEvent(
            id = "3",
            type = MetricEventType.WEIGHT,
            title = "Cập nhật cân nặng",
            value = "78.5 kg",
            note = "Bắt đầu chu trình cắt mỡ",
            photoUrl = null,
            date = "01 Thg 5, 2026",
            monthGroup = "Tháng 5, 2026"
        )
    )

    val grouped = mockEvents.groupBy { it.monthGroup }

    VitalAITheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Lịch sử thay đổi", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.FilterList, contentDescription = "Lọc", tint = ForestGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
                )
            },
            containerColor = AppMutedBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    grouped.forEach { (month, events) ->
                        // Sticky month header
                        @OptIn(ExperimentalFoundationApi::class)
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppMutedBackground)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    month,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ForestGreen
                                )
                            }
                        }

                        items(events.size) { idx ->
                            TimelineEventCard(
                                event = events[idx],
                                isFirst = idx == 0,
                                isLast = idx == events.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}
