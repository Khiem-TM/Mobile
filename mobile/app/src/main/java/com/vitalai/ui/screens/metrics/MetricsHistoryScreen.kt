package com.vitalai.ui.screens.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MetricsHistoryScreen(
    navController: NavController,
    viewModel: MetricsHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

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

    // Group events by month
    val grouped = uiState.events.groupBy { it.monthGroup }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử thay đổi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FilterList, contentDescription = "Lọc", tint = Ink700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tổng quan", "Lịch sử").forEachIndexed { i, label ->
                    val isSelected = uiState.selectedTab == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(if (isSelected) Mint500 else Ink100)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Ink700,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatItem(
                        label = "Cân nặng hiện tại",
                        value = "${uiState.currentWeightKg} kg",
                        color = Mint500
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Ink200)
                    SummaryStatItem(
                        label = "90 ngày qua",
                        value = "${if (uiState.delta90Days < 0) "" else "+"}${uiState.delta90Days} kg",
                        color = if (uiState.delta90Days < 0) Mint500 else MacroProtein
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Ink200)
                    SummaryStatItem(
                        label = "Sự kiện",
                        value = "${uiState.totalEvents}",
                        color = MacroCarbs
                    )
                }
            }

            if (uiState.isLoading) {
                // Skeleton shimmer
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    repeat(5) {
                        SkeletonRow()
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    grouped.forEach { (month, events) ->
                        // Sticky month header
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppBackground)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    month,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Ink700
                                )
                            }
                        }

                        itemsIndexed(events) { idx, event ->
                            TimelineEventCard(
                                event = event,
                                isFirst = idx == 0,
                                isLast = idx == events.size - 1
                            )
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
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = Ink500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun TimelineEventCard(
    event: MetricTimelineEvent,
    isFirst: Boolean,
    isLast: Boolean
) {
    val nodeColor = when (event.type) {
        MetricEventType.WEIGHT -> Mint500
        MetricEventType.PHOTO -> MacroFat
        MetricEventType.MEASUREMENT -> MacroWater
        MetricEventType.WORKOUT -> MacroCarbs
        MetricEventType.BADGE -> Color(0xFFD97706)
    }
    val nodeIcon = when (event.type) {
        MetricEventType.WEIGHT -> "⚖️"
        MetricEventType.PHOTO -> "📷"
        MetricEventType.MEASUREMENT -> "📏"
        MetricEventType.WORKOUT -> "🏋️"
        MetricEventType.BADGE -> "🏆"
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.12f))
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(nodeIcon, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Event card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(event.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
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
