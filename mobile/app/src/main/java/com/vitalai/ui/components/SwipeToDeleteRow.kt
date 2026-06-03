package com.vitalai.ui.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.VitalRadius
import kotlin.math.roundToInt

private enum class SwipeDeleteValue {
    Closed,
    Revealed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 88.dp,
    actionColor: Color = MacroProtein,
    shape: Shape = RoundedCornerShape(VitalRadius.Md),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val anchors = remember(actionWidthPx) {
        DraggableAnchors {
            SwipeDeleteValue.Closed at 0f
            SwipeDeleteValue.Revealed at -actionWidthPx
        }
    }
    val state = remember(density, anchors) {
        AnchoredDraggableState(
            initialValue = SwipeDeleteValue.Closed,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.45f },
            velocityThreshold = { with(density) { 120.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    Box(
        modifier = modifier.clip(shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(actionColor),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .clickable(onClick = onDelete),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Xóa", color = Color.White, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .anchoredDraggable(state = state, orientation = Orientation.Horizontal)
                .offsetX(state.requireOffset())
        ) {
            content()
        }
    }
}

private fun Modifier.offsetX(offset: Float): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(
                x = if (offset.isNaN()) 0 else offset.roundToInt(),
                y = 0
            )
        }
    )
