package com.vitalai.ui.screens.metrics.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.ForestGreen
import com.vitalai.ui.theme.Ink400
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp
) {
    // Thêm 1 item rỗng đầu/cuối để item đầu/cuối có thể lên giữa khi snap
    val paddedItems = remember(items) { listOf("") + items + listOf("") }
    val safeIndex = selectedIndex.coerceIn(0, maxOf(0, items.size - 1))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(listState)
    val currentOnSelectionChanged by rememberUpdatedState(onSelectionChanged)

    // Sync vị trí khi selectedIndex thay đổi từ ngoài (mở sheet với filter active, "Bỏ lọc")
    LaunchedEffect(safeIndex) {
        if (listState.firstVisibleItemIndex != safeIndex) {
            listState.scrollToItem(safeIndex)
        }
    }

    // Gọi callback khi scroll dừng (bỏ qua lần đầu khởi tạo)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .drop(1)
            .filter { !it }
            .collect { currentOnSelectionChanged(listState.firstVisibleItemIndex) }
    }

    Box(
        modifier = modifier.height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(paddedItems) { index, item ->
                // item giữa = firstVisibleItemIndex + 1
                val isSelected by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 == index } }
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isNotEmpty()) {
                        Text(
                            text = item,
                            color = if (isSelected) ForestGreen else Ink400,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.graphicsLayer {
                                val scale = if (isSelected) 1.1f else 1f
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                }
            }
        }

        // Gradient mờ trên
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
        )

        // Gradient mờ dưới
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
        )
    }
}
