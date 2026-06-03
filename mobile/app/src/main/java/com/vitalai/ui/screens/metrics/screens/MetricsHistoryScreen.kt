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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.vitalai.ui.screens.metrics.viewmodels.MetricEventType
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

    val grouped = uiState.filteredEvents.groupBy { it.monthGroup }
    val measurementMetrics = uiState.filteredEvents
        .filter { it.type == MetricEventType.MEASUREMENT && it.rawMetric != null }
        .mapNotNull { it.rawMetric }
    val weightMetrics = uiState.filteredEvents
        .filter { it.type == MetricEventType.WEIGHT && it.rawMetric != null }
        .mapNotNull { it.rawMetric }

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
                item {
                    val summary = uiState.summary
                    val changeVal = summary?.weightChange ?: 0f
                    val changeText = if (changeVal == 0f) "0 kg" else "%+.1f kg".format(changeVal)
                    val changeColor = when {
                        changeVal < 0f -> ForestGreen
                        changeVal > 0f -> Color(0xFFF87171)
                        else -> Ink500
                    }
                    val currentWeight = if ((summary?.currentWeight ?: 0f) > 0f)
                        "%.1f kg".format(summary!!.currentWeight) else "--"

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
                        StatCell(label = "Số bản ghi", value = "${summary?.totalRecords ?: 0}", color = ForestGreen)
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
                            Text(month, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
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
                            val previousMetric = when (event.type) {
                                MetricEventType.MEASUREMENT -> {
                                    val idx = measurementMetrics.indexOfFirst { it.id == event.rawMetric.id }
                                    measurementMetrics.getOrNull(idx + 1)
                                }
                                MetricEventType.WEIGHT -> {
                                    val idx = weightMetrics.indexOfFirst { it.id == event.rawMetric.id }
                                    weightMetrics.getOrNull(idx + 1)
                                }
                                else -> null
                            }
                            MetricDetailDialog(
                                metric = event.rawMetric,
                                photoUrls = event.photoUrls,
                                isMeasurementCard = event.type == MetricEventType.MEASUREMENT,
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
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) },
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
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
                        Icon(Icons.Default.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
