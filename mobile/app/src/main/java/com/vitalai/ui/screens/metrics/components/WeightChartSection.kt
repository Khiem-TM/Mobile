package com.vitalai.ui.screens.metrics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalCard
import com.vitalai.ui.screens.metrics.formatDateDisplay
import com.vitalai.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun PeriodChartSection(
    selectedPeriod: String,
    periodData: BodyMetricsPeriodDto?,
    onSelectPeriod: (String) -> Unit
) {
    var weekRangeText   by remember { mutableStateOf("") }
    var selectedMetric  by remember { mutableStateOf<BodyMetricDto?>(null) }
    var selectedDayIdx  by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx    by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    VitalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "Biểu đồ cân nặng", color = ForestGreen)
            Spacer(Modifier.height(10.dp))

            val tabs = listOf(
                "week" to "Tuần",
                "month" to "Tháng",
                "3months" to "Quý"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(AppSurface2)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .background(if (isSelected) ForestGreen else Color.Transparent)
                            .clickable { onSelectPeriod(key) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Ink700
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (periodData != null && periodData.data.size >= 2) {
                val idx    = selectedDayIdx
                val metric = selectedMetric
                val showTooltip = selectedPeriod == "week" && idx != null && metric != null && chartWidthPx > 0f
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.alpha(0f).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("99.9 kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                        Text("99/99/9999", fontSize = 8.sp, lineHeight = 11.sp)
                    }

                    if (showTooltip) {
                        val parentWDp = with(density) { chartWidthPx.toDp() }
                        val chartWDp  = parentWDp - 8.dp - 46.dp
                        val colWDp    = chartWDp / 7f
                        val cardWDp   = chartWDp * 2f / 7f
                        val centerX   = 8.dp + colWDp * (idx!! + 0.5f)
                        val cardLeft  = (centerX - cardWDp / 2f).coerceIn(0.dp, parentWDp - cardWDp)
                        Box(
                            modifier = Modifier
                                .offset(x = cardLeft)
                                .width(cardWDp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Ink900)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Column {
                                Text(
                                    "%.1f kg".format(metric!!.weightKg),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    formatDateDisplay(metric.date),
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 8.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().matchParentSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TB: ${"%.1f".format(periodData.avgWeight)} kg", fontSize = 12.sp, color = Ink500)
                            if (selectedPeriod == "week" && weekRangeText.isNotEmpty()) {
                                Text(weekRangeText, fontSize = 11.sp, color = Ink400)
                            }
                            Text("↓%.1f / ↑%.1f kg".format(periodData.minWeight, periodData.maxWeight), fontSize = 12.sp, color = Ink500)
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    val selIdx = selectedDayIdx
                    if (selIdx != null && size.width > 0f) {
                        val lp    = 8.dp.toPx()
                        val rp    = 46.dp.toPx()
                        val connX = lp + (selIdx + 0.5f) * ((size.width - lp - rp) / 7f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(connX, 0f),
                            end   = Offset(connX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                WeightLineChart(
                    data = periodData.data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .onSizeChanged { chartWidthPx = it.width.toFloat() },
                    period = selectedPeriod,
                    onWeekRangeChange = { weekRangeText = it },
                    onDaySelected = { i, m ->
                        selectedDayIdx = i
                        selectedMetric = m
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cần ít nhất 2 điểm dữ liệu để hiển thị biểu đồ",
                        color = Ink400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightLineChart(
    data: List<BodyMetricDto>,
    modifier: Modifier = Modifier,
    period: String = "week",
    onWeekRangeChange: ((String) -> Unit)? = null,
    onDaySelected: ((Int?, BodyMetricDto?) -> Unit)? = null
) {
    val pointsByDate = remember(data) {
        data.mapNotNull { metric ->
            metric.metricLocalDateOrNull()?.let { it to metric }
        }.distinctBy { it.first }.sortedBy { it.first }
    }
    if (pointsByDate.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Cần ít nhất 2 điểm dữ liệu", color = Ink500, fontSize = 12.sp)
        }
        return
    }

    val weights = pointsByDate.map { it.second.weightKg }
    val minW = weights.minOrNull() ?: 0f
    val maxW = weights.maxOrNull() ?: 0f
    val dataRange = (maxW - minW).coerceAtLeast(1f)

    val niceMin = (floor((minW - dataRange * 0.05) / 10.0) * 10).toFloat()
    val niceMax = (ceil((maxW + dataRange * 0.05) / 10.0) * 10).toFloat()
    val niceRange = (niceMax - niceMin).coerceAtLeast(10f)
    val tickStep = (ceil(niceRange / 20.0) * 10).toFloat()
    val axisMin = niceMin
    val axisMax = niceMin + 2f * tickStep
    val axisRange = axisMax - axisMin

    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(fontSize = 9.sp, color = Color(0xFF6B7280))

    if (period == "week") {
        var dayOffset by remember { mutableIntStateOf(0) }
        val dragX  = remember { Animatable(0f) }
        val scope  = rememberCoroutineScope()
        var rawDrag by remember { mutableFloatStateOf(0f) }
        val dragXValue = rawDrag + dragX.value

        val baseMonday = remember(pointsByDate) { pointsByDate.last().first.with(DayOfWeek.MONDAY) }
        val startDay   = remember(baseMonday, dayOffset) { baseMonday.plusDays(dayOffset.toLong()) }
        val prevStart  = remember(startDay) { startDay.minusDays(7) }
        val nextStart  = remember(startDay) { startDay.plusDays(7) }

        val dayLabels = remember(startDay) {
            val vn = mapOf(
                DayOfWeek.MONDAY to "T2", DayOfWeek.TUESDAY to "T3",
                DayOfWeek.WEDNESDAY to "T4", DayOfWeek.THURSDAY to "T5",
                DayOfWeek.FRIDAY to "T6", DayOfWeek.SATURDAY to "T7",
                DayOfWeek.SUNDAY to "CN"
            )
            (0..6).map { d -> vn[startDay.plusDays(d.toLong()).dayOfWeek] ?: "" }
        }

        fun buildMap(ref: LocalDate): Map<Int, BodyMetricDto> = pointsByDate
            .groupBy { (date, _) -> java.time.temporal.ChronoUnit.DAYS.between(ref, date).toInt() }
            .filterKeys { it in 0..6 }
            .mapValues { (_, list) -> list.maxByOrNull { it.first }!!.second }

        val prevMap = remember(pointsByDate, prevStart) { buildMap(prevStart) }
        val curMap  = remember(pointsByDate, startDay)  { buildMap(startDay) }
        val nextMap = remember(pointsByDate, nextStart) { buildMap(nextStart) }

        val minStartDay = remember(pointsByDate) { pointsByDate.first().first.with(DayOfWeek.MONDAY) }
        val maxStartDay = remember(pointsByDate) { pointsByDate.last().first.with(DayOfWeek.MONDAY) }
        val minStartDayState = rememberUpdatedState(minStartDay)
        val maxStartDayState = rememberUpdatedState(maxStartDay)

        val hasPrevDay = startDay > minStartDay
        val hasNextDay = startDay < maxStartDay
        val hasPrevDayState = rememberUpdatedState(hasPrevDay)
        val hasNextDayState = rememberUpdatedState(hasNextDay)

        val prevWeekMonday = remember(startDay) { startDay.with(DayOfWeek.MONDAY).minusWeeks(1) }
        val nextWeekMonday = remember(startDay) { startDay.with(DayOfWeek.MONDAY).plusWeeks(1) }
        val prevWeekMondayState = rememberUpdatedState(prevWeekMonday)
        val nextWeekMondayState = rememberUpdatedState(nextWeekMonday)
        val hasPrevState = rememberUpdatedState(prevWeekMonday >= minStartDay)
        val hasNextState = rememberUpdatedState(nextWeekMonday <= maxStartDay)

        val curMapState            = rememberUpdatedState(curMap)
        val onDaySelectedState     = rememberUpdatedState(onDaySelected)
        val onWeekRangeChangeState = rememberUpdatedState(onWeekRangeChange)
        val startDayState          = rememberUpdatedState(startDay)

        var selectedDayIdx by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(dayOffset) {
            selectedDayIdx = null
            onDaySelectedState.value?.invoke(null, null)
        }

        val weekRangeText = remember(startDay) {
            val endDay = startDay.plusDays(6)
            if (startDay.month == endDay.month)
                "${startDay.dayOfMonth}-${endDay.dayOfMonth} thg ${endDay.monthValue}, ${endDay.year}"
            else
                "${startDay.dayOfMonth} thg ${startDay.monthValue} - ${endDay.dayOfMonth} thg ${endDay.monthValue}, ${endDay.year}"
        }
        LaunchedEffect(weekRangeText) { onWeekRangeChangeState.value?.invoke(weekRangeText) }

        fun formatRange(m: LocalDate): String {
            val s = m.plusDays(6)
            return if (m.month == s.month)
                "${m.dayOfMonth}-${s.dayOfMonth} thg ${s.monthValue}, ${s.year}"
            else
                "${m.dayOfMonth} thg ${m.monthValue} - ${s.dayOfMonth} thg ${s.monthValue}, ${s.year}"
        }

        Box(
            modifier = modifier
                .pointerInput(Unit) {
                val leftPad = 8.dp.toPx()
                val rightPad = 46.dp.toPx()
                val pageW = size.width.toFloat() - leftPad - rightPad
                val colW  = pageW / 7f

                awaitEachGesture {
                    val velocityTracker = VelocityTracker()
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    var dragging = false

                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (dragging) {
                                val velocity = velocityTracker.calculateVelocity().x
                                scope.launch {
                                    val cur = rawDrag + dragX.value
                                    dragX.snapTo(cur)
                                    rawDrag = 0f
                                    val dayShift = (-cur / colW).roundToInt().coerceIn(-7, 7)
                                    when {
                                        velocity < -300f && hasNextState.value -> {
                                            val target = nextWeekMondayState.value
                                            val days   = java.time.temporal.ChronoUnit.DAYS.between(startDayState.value, target).toInt()
                                            onWeekRangeChangeState.value?.invoke(formatRange(target))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(days * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, target).toInt()
                                            dragX.snapTo(0f)
                                        }
                                        velocity > 300f && hasPrevState.value -> {
                                            val target = prevWeekMondayState.value
                                            val days   = java.time.temporal.ChronoUnit.DAYS.between(target, startDayState.value).toInt()
                                            onWeekRangeChangeState.value?.invoke(formatRange(target))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(days * colW, spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, target).toInt()
                                            dragX.snapTo(0f)
                                        }
                                        dayShift >= 1 && hasNextDayState.value -> {
                                            val maxOff = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, maxStartDayState.value).toInt()
                                            val shift  = dayShift.coerceAtMost(maxOff - dayOffset).coerceAtLeast(1)
                                            onWeekRangeChangeState.value?.invoke(formatRange(baseMonday.plusDays((dayOffset + shift).toLong())))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(shift * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset += shift; dragX.snapTo(0f)
                                        }
                                        dayShift <= -1 && hasPrevDayState.value -> {
                                            val minOff = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, minStartDayState.value).toInt()
                                            val shift  = dayShift.coerceAtLeast(minOff - dayOffset).coerceAtMost(-1)
                                            onWeekRangeChangeState.value?.invoke(formatRange(baseMonday.plusDays((dayOffset + shift).toLong())))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(shift * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset += shift; dragX.snapTo(0f)
                                        }
                                        else -> dragX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f))
                                    }
                                }
                            } else if (!dragX.isRunning) {
                                val tapDx = change.position.x - down.position.x
                                val tapDy = change.position.y - down.position.y
                                if (kotlin.math.abs(tapDx) < viewConfiguration.touchSlop &&
                                    kotlin.math.abs(tapDy) < viewConfiguration.touchSlop) {
                                    val colW = pageW / 7f
                                    val dayIdx = ((down.position.x - leftPad) / colW).toInt().coerceIn(0, 6)
                                    val map = curMapState.value
                                    val newIdx = if (map.containsKey(dayIdx))
                                        if (selectedDayIdx == dayIdx) null else dayIdx
                                    else null
                                    selectedDayIdx = newIdx
                                    onDaySelectedState.value?.invoke(newIdx, newIdx?.let { map[it] })
                                }
                            }
                            break
                        }

                        val totalDx = change.position.x - down.position.x
                        val totalDy = change.position.y - down.position.y

                        if (!dragging) {
                            when {
                                kotlin.math.abs(totalDx) > viewConfiguration.touchSlop &&
                                kotlin.math.abs(totalDx) >= kotlin.math.abs(totalDy) -> {
                                    dragging = true
                                    selectedDayIdx = null
                                    onDaySelectedState.value?.invoke(null, null)
                                    change.consume()
                                }
                                kotlin.math.abs(totalDy) > viewConfiguration.touchSlop -> break
                            }
                        }

                        if (dragging) {
                            change.consume()
                            val dx = change.position.x - change.previousPosition.x
                            val elastic = (dx > 0f && !hasPrevDayState.value) || (dx < 0f && !hasNextDayState.value)
                            rawDrag += if (elastic) dx * 0.3f else dx
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val leftPadding   = 8.dp.toPx()
                val rightPadding  = 46.dp.toPx()
                val topPadding    = 12.dp.toPx()
                val tickExtend    = 6.dp.toPx()
                val labelHeight   = 16.dp.toPx()
                val bottomPadding = tickExtend + labelHeight + 4.dp.toPx()
                val chartW        = w - leftPadding - rightPadding
                val chartH        = h - topPadding - bottomPadding
                val columnWidth   = chartW / 7f
                val pageW         = chartW
                val chartBottom   = topPadding + chartH

                fun yForWeight(wt: Float) = topPadding + (1f - (wt - axisMin) / axisRange) * chartH

                val gridColor  = Ink200.copy(alpha = 0.75f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)
                val lineColor  = ForestGreen
                val lineWidth  = 2.dp.toPx()
                val dotRadius  = 4.dp.toPx()

                repeat(3) { idx ->
                    val fraction = idx / 2f
                    val y = topPadding + fraction * chartH
                    drawLine(gridColor, Offset(leftPadding, y), Offset(w - rightPadding, y), strokeWidth = 1.dp.toPx())
                    val wLabel = axisMax - fraction * axisRange
                    val mLabel = textMeasurer.measure("%.0f".format(wLabel), axisLabelStyle)
                    drawText(mLabel, topLeft = Offset(w - rightPadding + 5.dp.toPx(), y - mLabel.size.height / 2f))
                }

                val pageOffsets = listOf(dragXValue - pageW, dragXValue, dragXValue + pageW)
                val pageMaps    = listOf(prevMap, curMap, nextMap)

                val pagePts = pageOffsets.zip(pageMaps).map { (tx, dayMap) ->
                    (0..6).mapNotNull { d ->
                        dayMap[d]?.let { m -> Offset(tx + leftPadding + (d + 0.5f) * columnWidth, yForWeight(m.weightKg)) }
                    }
                }
                val prevLastPt  = pagePts[0].lastOrNull()
                val curFirstPt  = pagePts[1].firstOrNull()
                val curLastPt   = pagePts[1].lastOrNull()
                val nextFirstPt = pagePts[2].firstOrNull()

                clipRect(leftPadding, 0f, w - rightPadding, h) {

                    pageOffsets.forEach { tx ->
                        for (i in 0..7) {
                            val x = tx + leftPadding + i * columnWidth
                            drawLine(Ink200.copy(alpha = 0.9f), Offset(x, topPadding),
                                Offset(x, chartBottom + tickExtend), 1.dp.toPx(), pathEffect = dashEffect)
                        }
                        for (d in 0..6) {
                            val cx = tx + leftPadding + (d + 0.5f) * columnWidth
                            val m  = textMeasurer.measure(dayLabels[d], axisLabelStyle)
                            drawText(m, topLeft = Offset(cx - m.size.width / 2f, chartBottom + tickExtend + 3.dp.toPx()))
                        }
                    }

                    selectedDayIdx?.let { dayIdx ->
                        val x = leftPadding + (dayIdx + 0.5f) * columnWidth
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(x, 0f),
                            end   = Offset(x, chartBottom),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    pagePts.forEach { pts ->
                        if (pts.size >= 2) drawPath(
                            Path().apply { moveTo(pts.first().x, chartBottom); pts.forEach { lineTo(it.x, it.y) }; lineTo(pts.last().x, chartBottom); close() },
                            color = lineColor.copy(alpha = 0.15f)
                        )
                    }
                    if (prevLastPt != null && curFirstPt != null) drawPath(
                        Path().apply { moveTo(prevLastPt.x, chartBottom); lineTo(prevLastPt.x, prevLastPt.y); lineTo(curFirstPt.x, curFirstPt.y); lineTo(curFirstPt.x, chartBottom); close() },
                        color = lineColor.copy(alpha = 0.15f)
                    )
                    if (curLastPt != null && nextFirstPt != null) drawPath(
                        Path().apply { moveTo(curLastPt.x, chartBottom); lineTo(curLastPt.x, curLastPt.y); lineTo(nextFirstPt.x, nextFirstPt.y); lineTo(nextFirstPt.x, chartBottom); close() },
                        color = lineColor.copy(alpha = 0.15f)
                    )

                    pagePts.forEach { pts ->
                        if (pts.size >= 2) drawPath(
                            Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } },
                            color = lineColor, style = Stroke(width = lineWidth)
                        )
                    }
                    if (prevLastPt != null && curFirstPt != null)
                        drawLine(lineColor, prevLastPt, curFirstPt, strokeWidth = lineWidth)
                    if (curLastPt != null && nextFirstPt != null)
                        drawLine(lineColor, curLastPt, nextFirstPt, strokeWidth = lineWidth)

                    pagePts.forEach { pts ->
                        pts.forEach { pt ->
                            drawCircle(lineColor, dotRadius, pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }
            }
        }
    } else {
        val firstDate = pointsByDate.first().first
        val lastDate = pointsByDate.last().first
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).toFloat().coerceAtLeast(1f)

        Column(modifier = modifier) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val w = size.width
                val h = size.height
                val leftPadding = 8.dp.toPx()
                val rightPadding = 46.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 8.dp.toPx()
                val chartW = w - leftPadding - rightPadding
                val chartH = h - topPadding - bottomPadding

                val points = pointsByDate.map { (date, metric) ->
                    val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(firstDate, date).toFloat()
                    val x = leftPadding + dayOffset / totalDays * chartW
                    val y = topPadding + (1f - (metric.weightKg - axisMin) / axisRange) * chartH
                    Offset(x, y)
                }

                val gridColor = Ink200.copy(alpha = 0.75f)

                repeat(3) { index ->
                    val fraction = index / 2f
                    val y = topPadding + fraction * chartH
                    drawLine(gridColor, Offset(leftPadding, y), Offset(w - rightPadding, y), strokeWidth = 1.dp.toPx())
                    val weightAtLine = axisMax - fraction * axisRange
                    val measured = textMeasurer.measure("%.0f".format(weightAtLine), axisLabelStyle)
                    drawText(measured, topLeft = Offset(w - rightPadding + 5.dp.toPx(), y - measured.size.height / 2f))
                }

                drawPath(
                    Path().apply {
                        moveTo(points.first().x, topPadding + chartH)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, topPadding + chartH)
                        close()
                    },
                    color = ForestGreen.copy(alpha = 0.15f)
                )
                drawPath(
                    Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    },
                    color = ForestGreen,
                    style = Stroke(width = 2.dp.toPx())
                )

                points.forEach { pt ->
                    drawCircle(color = ForestGreen, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(firstDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
                Text(lastDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
            }
        }
    }
}

private fun BodyMetricDto.metricLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
