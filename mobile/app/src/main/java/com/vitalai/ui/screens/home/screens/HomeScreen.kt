package com.vitalai.ui.screens.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.screens.home.viewmodels.HomeViewModel
import com.vitalai.ui.screens.home.components.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    val dateLabel = remember(uiState.selectedDate) {
        try {
            val d = java.time.LocalDate.parse(uiState.selectedDate)
            d.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMMM", java.util.Locale("vi")))
        } catch (e: Exception) { uiState.selectedDate }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.dashboard != null) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val listState = rememberLazyListState()
    val fadeOutOffsetPx = with(LocalDensity.current) { 100.dp.toPx() }
    val fastFadeOutOffsetPx = with(LocalDensity.current) { 50.dp.toPx() } // Mờ nhanh gấp đôi
    val fadeInOffsetPx = with(LocalDensity.current) { 200.dp.toPx() } // Chậm hơn 2 lần
    
    val fadeOutProgressProvider = remember {
        {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset / fadeOutOffsetPx).coerceIn(0f, 1f)
            } else 1f
        }
    }
    
    val fastFadeOutProgressProvider = remember {
        {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset / fastFadeOutOffsetPx).coerceIn(0f, 1f)
            } else 1f
        }
    }
    

    val fadeInProgressProvider = remember {
        {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset / fadeInOffsetPx).coerceIn(0f, 1f)
            } else 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when {
            uiState.isLoading && uiState.dashboard == null -> LoadingState(
                modifier = Modifier.fillMaxSize()
            )
            uiState.error != null && uiState.dashboard == null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadData() },
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 40.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppSurface)
                        ) {
                            Spacer(Modifier.statusBarsPadding())
                            Spacer(Modifier.height(38.dp)) // Profile nhô lên gần smallheader hơn
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { alpha = 1f - fadeOutProgressProvider() }
                            ) {
                                HomeHeader(navController, uiState)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { alpha = 1f - fadeInProgressProvider() }
                            ) {
                                WeekStrip(uiState.selectedDate) { date -> viewModel.selectDate(date) }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    
                    item { DailyCaloriesCard(uiState) }
                    item {
                        DailyActivitySummaryCard(
                            uiState = uiState,
                            onOpenActivityLog = {
                                navController.navigate(Screen.Activity(uiState.selectedDate))
                            }
                        )
                    }
                    item {
                        ActivityWeekSummaryCard(
                            uiState = uiState,
                            onDateClick = { date -> navController.navigate(Screen.Activity(date)) }
                        )
                    }
                    item { TrainFoodCarouselCard(uiState, navController) }
                    item { MealsSection(uiState.mealLogs, navController, uiState.selectedDate) }
                    item { DashboardTrendCard(uiState) }
                    item {
                        val streak = uiState.streaks?.loginStreak ?: 0
                        if (streak > 0) StreakBanner(streak)
                    }
                }

                // Bỏ qua Spacer an toàn phụ này vì đã sử dụng statusBarsPadding

                com.vitalai.ui.components.VitalSmallHeader(
                    title = "Trang chủ",
                    textAlphaProvider = fadeInProgressProvider,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Spacer(Modifier.statusBarsPadding())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 24.dp)
                            .graphicsLayer { alpha = 1f - fastFadeOutProgressProvider() },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = dateLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }


            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            contentColor = Mint500
        )
    }
}
