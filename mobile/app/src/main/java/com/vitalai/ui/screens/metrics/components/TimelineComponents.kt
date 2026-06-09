package com.vitalai.ui.screens.metrics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.ui.screens.metrics.formatCompact
import com.vitalai.ui.screens.metrics.viewmodels.MetricEventType
import com.vitalai.ui.screens.metrics.viewmodels.MetricTimelineEvent
import com.vitalai.ui.theme.*
import kotlin.math.abs

@Composable
internal fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Ink500, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun TimelineEventCard(
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
        Box(modifier = Modifier.width(40.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 19.dp)
            ) {
                drawRect(
                    color = Ink200,
                    topLeft = Offset(0f, if (isFirst) size.height * 0.4f else 0f),
                    size = Size(2f, if (isLast) size.height * 0.4f else size.height)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .align(Alignment.TopCenter),
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

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp)
                .clickable(onClick = onDoubleClick),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = VitalElevation.Level1)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "${event.value} kg",
                        fontSize = 14.sp,
                        color = nodeColor,
                        fontWeight = FontWeight.Bold
                    )
                    InlineDelta(event.weightDelta)
                    Text("·", fontSize = 14.sp, color = Ink300)
                    val bodyFatPct = event.rawMetric?.bodyFatPct
                    Text(
                        if (bodyFatPct != null) "${bodyFatPct.formatCompact()}% mỡ" else "--% mỡ",
                        fontSize = 14.sp,
                        color = nodeColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (bodyFatPct != null) InlineDelta(event.bodyFatDelta)
                }
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
private fun InlineDelta(delta: Float?) {
    if (delta == null || abs(delta) < 0.05f) return
    val isUp = delta > 0f
    Text(
        "(${if (isUp) "↑" else "↓"}${abs(delta).formatCompact()})",
        fontSize = 12.sp,
        color = if (isUp) Color(0xFFF87171) else ForestGreen,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun SkeletonRow() {
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
