package com.vitalai.ui.screens.metrics

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.tooling.preview.Preview
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
        containerColor = AppMutedBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                // Skeleton shimmer
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    repeat(5) {
                        SkeletonRow()
                    }
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
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
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
        MetricEventType.WEIGHT -> Icons.Default.AccessibilityNew
        MetricEventType.PHOTO -> Icons.Default.CameraAlt
        MetricEventType.MEASUREMENT -> Icons.Default.FilterList
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
                    .background(nodeColor.copy(alpha = 0.15f))
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = nodeIcon,
                    contentDescription = null,
                    tint = nodeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Event card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
            elevation = CardDefaults.cardElevation(0.dp)
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
                            Icon(Icons.Default.FilterList, contentDescription = "Lọc", tint = Ink700)
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
                                    color = Ink700
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
