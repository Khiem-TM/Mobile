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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
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
import java.time.YearMonth
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun PeriodChartSection(
    selectedPeriod: String,
    periodData: BodyMetricsPeriodDto?,
    onSelectPeriod: (String) -> Unit,
    earliestDate: LocalDate? = null,
    onExpandWeekRange: ((LocalDate) -> Unit)? = null
) {
    var weekRangeText      by remember { mutableStateOf("") }
    var selectedMetric     by remember { mutableStateOf<BodyMetricDto?>(null) }
    var selectedDayIdx     by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx       by remember { mutableFloatStateOf(0f) }
    var visibleWeekWeights   by remember { mutableStateOf<List<Float>>(emptyList()) }
    var visibleMonthWeights  by remember { mutableStateOf<List<Float>>(emptyList()) }
    var selectedMonthXNorm   by remember { mutableStateOf<Float?>(null) }
    var selectedMonthMetric  by remember { mutableStateOf<BodyMetricDto?>(null) }
    var monthYearText by remember {
        val now = LocalDate.now()
        mutableStateOf("Tháng ${now.monthValue}, ${now.year}")
    }
    var selectedQuarterAvgWeight by remember { mutableStateOf<Float?>(null) }
    var selectedQuarterXNorm     by remember { mutableStateOf<Float?>(null) }
    var quarterCenterMonth       by remember { mutableStateOf<YearMonth?>(null) }
    var monthDisplayStart        by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val density = LocalDensity.current

    LaunchedEffect(selectedPeriod) {
        if (selectedPeriod != "month") {
            selectedMonthXNorm  = null
            selectedMonthMetric = null
        }
        if (selectedPeriod != "3months") {
            selectedQuarterAvgWeight = null
            selectedQuarterXNorm     = null
        }
    }

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
                val quarterRangeText = quarterCenterMonth?.let { center ->
                    "Tháng ${center.minusMonths(1).monthValue} - ${center.plusMonths(1).monthValue}, ${center.year}"
                } ?: ""
                val displayWeights = when (selectedPeriod) {
                    "week"  -> visibleWeekWeights
                    "month" -> {
                        val start = monthDisplayStart
                        val end   = start.withDayOfMonth(start.lengthOfMonth())
                        periodData.data.mapNotNull { dto ->
                            val d = runCatching { LocalDate.parse(dto.date.take(10)) }.getOrNull()
                                ?: return@mapNotNull null
                            if (!d.isBefore(start) && !d.isAfter(end)) dto.weightKg else null
                        }
                    }
                    "3months" -> {
                        val monthAvgMap: Map<YearMonth, Float> = periodData.data
                            .mapNotNull { dto ->
                                val d = runCatching { LocalDate.parse(dto.date.take(10)) }.getOrNull()
                                    ?: return@mapNotNull null
                                YearMonth.from(d) to dto.weightKg
                            }
                            .groupBy({ it.first }, { it.second })
                            .mapValues { (_, ws) -> ws.average().toFloat() }

                        val center = quarterCenterMonth ?: monthAvgMap.keys.maxOrNull()

                        if (center != null) {
                            val visible = setOf(center.minusMonths(1), center, center.plusMonths(1))
                            visible.mapNotNull { ym -> monthAvgMap[ym] }
                        } else emptyList()
                    }
                    else -> periodData.data.map { it.weightKg }
                }
                val (minText, maxText) = when {
                    displayWeights.isEmpty() -> "--" to "--"
                    displayWeights.all { it == displayWeights.first() } -> {
                        val v = String.format(Locale.US, "%.1f", displayWeights.first())
                        v to v
                    }
                    else ->
                        String.format(Locale.US, "%.1f", displayWeights.minOrNull()!!) to
                        String.format(Locale.US, "%.1f", displayWeights.maxOrNull()!!)
                }
                val avgDisplay = if (displayWeights.isEmpty()) null
                                 else displayWeights.average().toFloat()

                val idx    = selectedDayIdx
                val metric = selectedMetric
                val showTooltip =
                    (selectedPeriod == "week"     && idx != null && metric != null && chartWidthPx > 0f) ||
                    (selectedPeriod == "month"    && selectedMonthMetric != null && selectedMonthXNorm != null && chartWidthPx > 0f) ||
                    (selectedPeriod == "3months"  && selectedQuarterAvgWeight != null && selectedQuarterXNorm != null && chartWidthPx > 0f)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.alpha(0f).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("99.9 kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                        Text("99/99/9999", fontSize = 8.sp, lineHeight = 11.sp)
                    }

                    if (showTooltip) {
                        val parentWDp = with(density) { chartWidthPx.toDp() }
                        val chartWDp  = parentWDp - 8.dp - 46.dp
                        val cardWDp   = chartWDp * 2f / 7f
                        val centerX   = when (selectedPeriod) {
                            "week"    -> 8.dp + (chartWDp / 7f) * (idx!! + 0.5f)
                            "month"   -> 8.dp + chartWDp * selectedMonthXNorm!!
                            else      -> 8.dp + chartWDp * selectedQuarterXNorm!!
                        }
                        val cardLeft = (centerX - cardWDp / 2f).coerceIn(0.dp, parentWDp - cardWDp)
                        Box(
                            modifier = Modifier
                                .offset(x = cardLeft)
                                .width(cardWDp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Ink900)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (selectedPeriod == "3months") {
                                Column {
                                    Text(
                                        text = "TB tháng",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 8.sp,
                                        lineHeight = 11.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f kg", selectedQuarterAvgWeight!!),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp
                                    )
                                }
                            } else {
                                val displayMetric = if (selectedPeriod == "week") metric!! else selectedMonthMetric!!
                                Column {
                                    Text(
                                        String.format(Locale.US, "%.1f kg", displayMetric.weightKg),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp
                                    )
                                    Text(
                                        formatDateDisplay(displayMetric.date),
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 8.sp,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().matchParentSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (avgDisplay != null) "TB: ${String.format(Locale.US, "%.1f", avgDisplay)} kg" else "TB: --",
                                fontSize = 12.sp,
                                color = Ink500
                            )
                            if (selectedPeriod == "week" && weekRangeText.isNotEmpty()) {
                                Text(weekRangeText, fontSize = 12.sp, color = ForestGreen, fontWeight = SemiBold)
                            } else if (selectedPeriod == "month") {
                                Text(monthYearText, fontSize = 12.sp, color = ForestGreen, fontWeight = SemiBold)
                            } else if (selectedPeriod == "3months" && quarterRangeText.isNotEmpty()) {
                                Text(quarterRangeText, fontSize = 12.sp, color = ForestGreen, fontWeight = SemiBold)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                    Text(minText, fontSize = 12.sp, color = ForestGreen)
                                }
                                Text("/", fontSize = 12.sp, color = Ink400)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.VerticalAlignTop, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                    Text(maxText, fontSize = 12.sp, color = Color(0xFFF87171))
                                }
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    if (size.width > 0f) {
                        val lp    = 8.dp.toPx()
                        val rp    = 46.dp.toPx()
                        val connX = when {
                            selectedPeriod == "week"    && selectedDayIdx != null        ->
                                lp + (selectedDayIdx!! + 0.5f) * ((size.width - lp - rp) / 7f)
                            selectedPeriod == "month"   && selectedMonthXNorm != null    ->
                                lp + selectedMonthXNorm!! * (size.width - lp - rp)
                            selectedPeriod == "3months" && selectedQuarterXNorm != null  ->
                                lp + selectedQuarterXNorm!! * (size.width - lp - rp)
                            else -> return@Canvas
                        }
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
                    earliestDate = earliestDate,
                    onExpandWeekRange = onExpandWeekRange,
                    onWeekRangeChange = { weekRangeText = it },
                    onDaySelected = { i, m ->
                        selectedDayIdx = i
                        selectedMetric = m
                    },
                    onVisibleWeightsChange = { visibleWeekWeights = it },
                    onMonthDaySelected = { m, xNorm ->
                        selectedMonthMetric = m
                        selectedMonthXNorm  = xNorm
                    },
                    onMonthRangeChange = { monthYearText = it },
                    onVisibleMonthWeightsChange = { visibleMonthWeights = it },
                    onMonthCenterStartChange = { monthDisplayStart = it },
                    onQuarterMonthSelected = { avgW, xNorm ->
                        selectedQuarterAvgWeight = avgW
                        selectedQuarterXNorm     = xNorm
                    },
                    onQuarterCenterMonthChange = { quarterCenterMonth = it }
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
    earliestDate: LocalDate? = null,
    onExpandWeekRange: ((LocalDate) -> Unit)? = null,
    onWeekRangeChange: ((String) -> Unit)? = null,
    onDaySelected: ((Int?, BodyMetricDto?) -> Unit)? = null,
    onVisibleWeightsChange: ((List<Float>) -> Unit)? = null,
    onMonthDaySelected: ((BodyMetricDto?, Float?) -> Unit)? = null,
    onMonthRangeChange: ((String) -> Unit)? = null,
    onVisibleMonthWeightsChange: ((List<Float>) -> Unit)? = null,
    onMonthCenterStartChange: ((LocalDate) -> Unit)? = null,
    onQuarterMonthSelected: ((Float?, Float?) -> Unit)? = null,
    onQuarterCenterMonthChange: ((YearMonth) -> Unit)? = null
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
        val prevStart     = remember(startDay) { startDay.minusDays(7) }
        val nextStart     = remember(startDay) { startDay.plusDays(7) }
        val prevPrevStart = remember(prevStart) { prevStart.minusDays(7) }
        val nextNextStart = remember(nextStart) { nextStart.plusDays(7) }

        val dayLabels = remember(startDay) {
            val vn = mapOf(
                DayOfWeek.MONDAY to "T2", DayOfWeek.TUESDAY to "T3",
                DayOfWeek.WEDNESDAY to "T4", DayOfWeek.THURSDAY to "T5",
                DayOfWeek.FRIDAY to "T6", DayOfWeek.SATURDAY to "T7",
                DayOfWeek.SUNDAY to "CN"
            )
            (0..6).map { d -> vn[startDay.plusDays(d.toLong()).dayOfWeek] ?: "" }
        }

        val dateMap = remember(pointsByDate) {
            pointsByDate.associate { (date, metric) -> date to metric }
        }

        fun buildMap(ref: LocalDate): Map<Int, BodyMetricDto> =
            (0..6).mapNotNull { d -> dateMap[ref.plusDays(d.toLong())]?.let { d to it } }.toMap()

        val prevPrevMap = remember(dateMap, prevPrevStart) { buildMap(prevPrevStart) }
        val prevMap     = remember(dateMap, prevStart)     { buildMap(prevStart) }
        val curMap      = remember(dateMap, startDay)      { buildMap(startDay) }
        val nextMap     = remember(dateMap, nextStart)     { buildMap(nextStart) }
        val nextNextMap = remember(dateMap, nextNextStart) { buildMap(nextNextStart) }
        val pageStarts  = listOf(prevPrevStart, prevStart, startDay, nextStart, nextNextStart)

        val minStartDay = remember(pointsByDate, earliestDate) {
            earliestDate?.with(DayOfWeek.MONDAY)
                ?: pointsByDate.first().first.with(DayOfWeek.MONDAY)
        }
        val maxStartDay = remember { LocalDate.now().with(DayOfWeek.MONDAY) }
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

        val onExpandWeekRangeState      = rememberUpdatedState(onExpandWeekRange)
        val onVisibleWeightsChangeState = rememberUpdatedState(onVisibleWeightsChange)

        var selectedDayIdx by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(dayOffset) {
            selectedDayIdx = null
            onDaySelectedState.value?.invoke(null, null)
            onExpandWeekRangeState.value?.invoke(startDay)
        }
        LaunchedEffect(curMap) {
            onVisibleWeightsChangeState.value?.invoke(curMap.values.map { it.weightKg })
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

                val pageOffsets = listOf(
                    dragXValue - 2 * pageW,
                    dragXValue - pageW,
                    dragXValue,
                    dragXValue + pageW,
                    dragXValue + 2 * pageW
                )
                val pageMaps = listOf(prevPrevMap, prevMap, curMap, nextMap, nextNextMap)

                val pagePtsWithDates: List<List<Pair<LocalDate, Offset>>> =
                    pageOffsets.indices.map { i ->
                        val tx     = pageOffsets[i]
                        val dayMap = pageMaps[i]
                        val ref    = pageStarts[i]
                        (0..6).mapNotNull { d ->
                            dayMap[d]?.let { m ->
                                ref.plusDays(d.toLong()) to
                                    Offset(tx + leftPadding + (d + 0.5f) * columnWidth, yForWeight(m.weightKg))
                            }
                        }
                    }
                val pagePts = pagePtsWithDates.map { list -> list.map { it.second } }

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
                    for (i in 0 until pagePtsWithDates.size - 1) {
                        val (fromDate, fromPt) = pagePtsWithDates[i].lastOrNull() ?: continue
                        val (toDate, toPt) = ((i + 1) until pagePtsWithDates.size)
                            .firstNotNullOfOrNull { pagePtsWithDates[it].firstOrNull() } ?: continue
                        val weekGap = java.time.temporal.ChronoUnit.DAYS.between(
                            fromDate.with(DayOfWeek.MONDAY),
                            toDate.with(DayOfWeek.MONDAY)
                        )
                        if (weekGap > 7) continue
                        drawPath(
                            Path().apply {
                                moveTo(fromPt.x, chartBottom); lineTo(fromPt.x, fromPt.y)
                                lineTo(toPt.x, toPt.y); lineTo(toPt.x, chartBottom); close()
                            },
                            color = lineColor.copy(alpha = 0.15f)
                        )
                        drawLine(lineColor, fromPt, toPt, strokeWidth = lineWidth)
                    }

                    pagePts.forEach { pts ->
                        if (pts.size >= 2) drawPath(
                            Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } },
                            color = lineColor, style = Stroke(width = lineWidth)
                        )
                    }

                    pagePts.forEach { pts ->
                        pts.forEach { pt ->
                            drawCircle(lineColor, dotRadius, pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }
            }
        }
    } else if (period == "month") {
        var monthOffset by remember { mutableIntStateOf(0) }
        val dragX  = remember { Animatable(0f) }
        var rawDrag by remember { mutableFloatStateOf(0f) }
        val dragXValue = rawDrag + dragX.value
        val scope  = rememberCoroutineScope()

        val baseMonth      = remember { LocalDate.now().withDayOfMonth(1) }
        val curMonthStart  = remember(monthOffset) { baseMonth.plusMonths(monthOffset.toLong()) }
        val prevMonthStart = remember(curMonthStart) { curMonthStart.minusMonths(1) }
        val nextMonthStart = remember(curMonthStart) { curMonthStart.plusMonths(1) }

        fun buildMonthPts(start: LocalDate): List<Pair<LocalDate, BodyMetricDto>> {
            val end = start.withDayOfMonth(start.lengthOfMonth())
            return pointsByDate.filter { (d, _) -> !d.isBefore(start) && !d.isAfter(end) }
        }
        val prevPoints = remember(pointsByDate, prevMonthStart) { buildMonthPts(prevMonthStart) }
        val curPoints  = remember(pointsByDate, curMonthStart)  { buildMonthPts(curMonthStart) }
        val nextPoints = remember(pointsByDate, nextMonthStart) { buildMonthPts(nextMonthStart) }

        val minMonth = remember(pointsByDate, earliestDate) {
            (earliestDate ?: pointsByDate.firstOrNull()?.first ?: baseMonth).withDayOfMonth(1)
        }
        val hasPrev = curMonthStart > minMonth
        val hasNext = curMonthStart < baseMonth
        val hasPrevState = rememberUpdatedState(hasPrev)
        val hasNextState = rememberUpdatedState(hasNext)

        var selectedMonthIdx by remember(curPoints) { mutableStateOf<Int?>(null) }

        val onMonthDaySelectedState       = rememberUpdatedState(onMonthDaySelected)
        val onMonthRangeChangeState       = rememberUpdatedState(onMonthRangeChange)
        val onVisibleMonthWCState         = rememberUpdatedState(onVisibleMonthWeightsChange)
        val onMonthCenterStartChangeState = rememberUpdatedState(onMonthCenterStartChange)
        val curPointsState                = rememberUpdatedState(curPoints)
        val curMonthStartState            = rememberUpdatedState(curMonthStart)

        LaunchedEffect(curPoints) {
            selectedMonthIdx = null
            onMonthDaySelectedState.value?.invoke(null, null)
            onVisibleMonthWCState.value?.invoke(curPoints.map { it.second.weightKg })
        }
        LaunchedEffect(curMonthStart) { onMonthCenterStartChangeState.value?.invoke(curMonthStart) }
        val monthYearStr = remember(curMonthStart) { "Tháng ${curMonthStart.monthValue}, ${curMonthStart.year}" }
        LaunchedEffect(monthYearStr) { onMonthRangeChangeState.value?.invoke(monthYearStr) }

        Box(
            modifier = modifier.pointerInput(Unit) {
                val lp    = 8.dp.toPx()
                val rp    = 46.dp.toPx()
                val pageW = size.width.toFloat() - lp - rp
                val inset = 16.dp.toPx()   // must stay in sync with horizontalInset in Canvas block

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
                                    dragX.snapTo(cur); rawDrag = 0f
                                    val mShift = (-cur / pageW).roundToInt().coerceIn(-1, 1)
                                    when {
                                        (velocity < -300f || mShift >= 1) && hasNextState.value -> {
                                            onMonthDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-pageW, spring(dampingRatio = 1f, stiffness = 800f))
                                            monthOffset++; dragX.snapTo(0f)
                                        }
                                        (velocity > 300f || mShift <= -1) && hasPrevState.value -> {
                                            onMonthDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(pageW, spring(dampingRatio = 1f, stiffness = 800f))
                                            monthOffset--; dragX.snapTo(0f)
                                        }
                                        else -> dragX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f))
                                    }
                                }
                            } else if (!dragX.isRunning) {
                                val tapDx = change.position.x - down.position.x
                                val tapDy = change.position.y - down.position.y
                                if (kotlin.math.abs(tapDx) < viewConfiguration.touchSlop &&
                                    kotlin.math.abs(tapDy) < viewConfiguration.touchSlop) {
                                    val tapX    = down.position.x
                                    val pts     = curPointsState.value
                                    val daysInMo = curMonthStartState.value.lengthOfMonth()
                                    fun xLocal(day: Int) =
                                        lp + inset + (day - 1).toFloat() / (daysInMo - 1).coerceAtLeast(1) * (pageW - 2 * inset)
                                    val tapThreshold = 16.dp.toPx()
                                    val nearest = pts.indices.minByOrNull { i ->
                                        kotlin.math.abs(tapX - xLocal(pts[i].first.dayOfMonth))
                                    }?.takeIf { i ->
                                        kotlin.math.abs(tapX - xLocal(pts[i].first.dayOfMonth)) <= tapThreshold
                                    }
                                    val newIdx = if (nearest != null && selectedMonthIdx != nearest) nearest else null
                                    selectedMonthIdx = newIdx
                                    if (newIdx != null) {
                                        val day = pts[newIdx].first.dayOfMonth
                                        onMonthDaySelectedState.value?.invoke(
                                            pts[newIdx].second,
                                            (xLocal(day) - lp) / pageW.coerceAtLeast(1f)
                                        )
                                    } else {
                                        onMonthDaySelectedState.value?.invoke(null, null)
                                    }
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
                                    selectedMonthIdx = null
                                    onMonthDaySelectedState.value?.invoke(null, null)
                                    change.consume()
                                }
                                kotlin.math.abs(totalDy) > viewConfiguration.touchSlop -> break
                            }
                        }
                        if (dragging) {
                            change.consume()
                            val dx = change.position.x - change.previousPosition.x
                            val elastic = (dx > 0f && !hasPrevState.value) || (dx < 0f && !hasNextState.value)
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
                val leftPadding  = 8.dp.toPx()
                val rightPadding = 46.dp.toPx()
                val topPadding   = 12.dp.toPx()
                val tickExtend   = 6.dp.toPx()
                val labelHeight  = 16.dp.toPx()
                val bottomPadding = tickExtend + labelHeight + 4.dp.toPx()
                val chartW       = w - leftPadding - rightPadding
                val chartH       = h - topPadding - bottomPadding
                val chartBottom  = topPadding + chartH
                val dotRadius        = 4.dp.toPx()
                val horizontalInset  = dotRadius + 12.dp.toPx()

                fun yForWeight(wt: Float) = topPadding + (1f - (wt - axisMin) / axisRange) * chartH

                val gridColor  = Ink200.copy(alpha = 0.75f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)

                repeat(3) { gridIdx ->
                    val fraction = gridIdx / 2f
                    val y = topPadding + fraction * chartH
                    drawLine(gridColor, Offset(leftPadding, y), Offset(w - rightPadding, y), strokeWidth = 1.dp.toPx())
                    val lbl = textMeasurer.measure("%.0f".format(axisMax - fraction * axisRange), axisLabelStyle)
                    drawText(lbl, topLeft = Offset(w - rightPadding + 5.dp.toPx(), y - lbl.size.height / 2f))
                }

                val pageOffsets  = listOf(dragXValue - chartW, dragXValue, dragXValue + chartW)
                val pageDataList = listOf(prevPoints, curPoints, nextPoints)
                val pageMonths   = listOf(prevMonthStart, curMonthStart, nextMonthStart)

                pageOffsets.forEachIndexed { pageIdx, tx ->
                    val clipLeft  = (tx + leftPadding).coerceAtLeast(leftPadding)
                    val clipRight = (tx + leftPadding + chartW).coerceAtMost(w - rightPadding)
                    if (clipLeft >= clipRight) return@forEachIndexed

                    val month    = pageMonths[pageIdx]
                    val daysInMo = month.lengthOfMonth()
                    val pts      = pageDataList[pageIdx]

                    fun xForDayPage(day: Int) =
                        tx + leftPadding + horizontalInset +
                        (day - 1).toFloat() / (daysInMo - 1).coerceAtLeast(1) * (chartW - 2 * horizontalInset)

                    clipRect(clipLeft, 0f, clipRight, h) {
                        val weekSepOffsets = generateSequence(0) { it + 7 }.takeWhile { it < daysInMo }.toList()
                        val endOff         = daysInMo - 1
                        val allSep         = if (weekSepOffsets.last() == endOff) weekSepOffsets
                                             else weekSepOffsets + listOf(endOff)
                        allSep.forEach { off ->
                            drawLine(
                                Ink200.copy(alpha = 0.9f),
                                Offset(xForDayPage(off + 1), topPadding),
                                Offset(xForDayPage(off + 1), chartBottom + tickExtend),
                                1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }
                        weekSepOffsets.forEach { off ->
                            val lbl = textMeasurer.measure((off + 1).toString(), axisLabelStyle)
                            drawText(lbl, topLeft = Offset(
                                xForDayPage(off + 1) - lbl.size.width / 2f,
                                chartBottom + tickExtend + 3.dp.toPx()
                            ))
                        }

                        if (pageIdx == 1) {
                            selectedMonthIdx?.let { si ->
                                if (si < pts.size) {
                                    val x = xForDayPage(pts[si].first.dayOfMonth)
                                    drawLine(
                                        Color.Gray.copy(alpha = 0.35f),
                                        Offset(x, 0f), Offset(x, chartBottom),
                                        1.5.dp.toPx()
                                    )
                                }
                            }
                        }

                        val ptOffsets = pts.map { (date, m) ->
                            Offset(xForDayPage(date.dayOfMonth), yForWeight(m.weightKg))
                        }
                        if (ptOffsets.size >= 2) {
                            drawPath(
                                Path().apply {
                                    moveTo(ptOffsets.first().x, chartBottom)
                                    ptOffsets.forEach { lineTo(it.x, it.y) }
                                    lineTo(ptOffsets.last().x, chartBottom)
                                    close()
                                },
                                color = ForestGreen.copy(alpha = 0.15f)
                            )
                            drawPath(
                                Path().apply {
                                    moveTo(ptOffsets.first().x, ptOffsets.first().y)
                                    ptOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                                },
                                color = ForestGreen,
                                style = Stroke(2.dp.toPx())
                            )
                        }
                        ptOffsets.forEach { pt ->
                            drawCircle(ForestGreen, 4.dp.toPx(), pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }
            }
        }
    } else {
        val latestDataMonth = remember(pointsByDate) { YearMonth.from(pointsByDate.last().first) }
        val allMonthAvg: Map<YearMonth, Float> = remember(pointsByDate) {
            pointsByDate
                .groupBy { (date, _) -> YearMonth.from(date) }
                .mapValues { (_, pts) -> pts.map { (_, m) -> m.weightKg }.average().toFloat() }
        }

        var monthOffset by remember(latestDataMonth) { mutableIntStateOf(0) }
        val centerMonth = latestDataMonth.plusMonths(monthOffset.toLong())

        val hasPrev = allMonthAvg.containsKey(centerMonth.minusMonths(1))
        val hasNext = allMonthAvg.containsKey(centerMonth.plusMonths(1))
        val hasPrevState = rememberUpdatedState(hasPrev)
        val hasNextState = rememberUpdatedState(hasNext)

        // Visible: [center-1, center, center+1]; neighbors for line: center-2, center+2
        val stripMonths = remember(centerMonth) {
            (-2..2).map { centerMonth.plusMonths(it.toLong()) }
        }

        var selectedQuarterIdx by remember { mutableStateOf<Int?>(null) }
        val dragXAnim = remember { Animatable(0f) }
        var rawDrag by remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()
        val dragXValue = rawDrag + dragXAnim.value

        val onQuarterSelectedState          = rememberUpdatedState(onQuarterMonthSelected)
        val onQuarterCenterMonthChangeState = rememberUpdatedState(onQuarterCenterMonthChange)
        val centerMonthState                = rememberUpdatedState(centerMonth)

        LaunchedEffect(centerMonth) {
            selectedQuarterIdx = null
            onQuarterSelectedState.value?.invoke(null, null)
            onQuarterCenterMonthChangeState.value?.invoke(centerMonth)
        }

        Box(
            modifier = modifier.pointerInput(Unit) {
                val lp       = 8.dp.toPx()
                val rp       = 46.dp.toPx()
                val chartW   = size.width.toFloat() - lp - rp
                val sectionW = chartW / 3f

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
                                    val cur = rawDrag + dragXAnim.value
                                    dragXAnim.snapTo(cur); rawDrag = 0f
                                    // shift threshold: half a section width
                                    val mShift = (-cur / sectionW).roundToInt().coerceIn(-1, 1)
                                    when {
                                        (velocity < -300f || mShift >= 1) && hasNextState.value -> {
                                            onQuarterSelectedState.value?.invoke(null, null)
                                            dragXAnim.animateTo(-sectionW, spring(dampingRatio = 1f, stiffness = 800f))
                                            monthOffset++; dragXAnim.snapTo(0f)
                                        }
                                        (velocity > 300f || mShift <= -1) && hasPrevState.value -> {
                                            onQuarterSelectedState.value?.invoke(null, null)
                                            dragXAnim.animateTo(sectionW, spring(dampingRatio = 1f, stiffness = 800f))
                                            monthOffset--; dragXAnim.snapTo(0f)
                                        }
                                        else -> dragXAnim.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f))
                                    }
                                }
                            } else if (!dragXAnim.isRunning) {
                                val tapDx = change.position.x - down.position.x
                                val tapDy = change.position.y - down.position.y
                                if (kotlin.math.abs(tapDx) < viewConfiguration.touchSlop &&
                                    kotlin.math.abs(tapDy) < viewConfiguration.touchSlop) {
                                    val tapX       = down.position.x
                                    val sectionIdx = ((tapX - lp) / sectionW).toInt().coerceIn(0, 2)
                                    // stripMonths: index 0=center-2, 1=center-1, 2=center, 3=center+1, 4=center+2
                                    // visible sections 0,1,2 → strip indices 1,2,3
                                    val ym  = centerMonthState.value.plusMonths((sectionIdx - 1).toLong())
                                    val avg = allMonthAvg[ym]
                                    if (avg != null) {
                                        val newIdx = if (selectedQuarterIdx == sectionIdx) null else sectionIdx
                                        selectedQuarterIdx = newIdx
                                        val xNorm = if (newIdx != null) (sectionIdx + 0.5f) / 3f else null
                                        onQuarterSelectedState.value?.invoke(avg.takeIf { newIdx != null }, xNorm)
                                    } else {
                                        selectedQuarterIdx = null
                                        onQuarterSelectedState.value?.invoke(null, null)
                                    }
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
                                    selectedQuarterIdx = null
                                    onQuarterSelectedState.value?.invoke(null, null)
                                    change.consume()
                                }
                                kotlin.math.abs(totalDy) > viewConfiguration.touchSlop -> break
                            }
                        }
                        if (dragging) {
                            change.consume()
                            val dx = change.position.x - change.previousPosition.x
                            val elastic = (dx > 0f && !hasPrevState.value) || (dx < 0f && !hasNextState.value)
                            rawDrag += if (elastic) dx * 0.3f else dx
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w  = size.width
                val h  = size.height
                val lp = 8.dp.toPx()
                val rp = 46.dp.toPx()
                val tp = 12.dp.toPx()
                val labelH      = 16.dp.toPx()
                val bp          = labelH + 4.dp.toPx()
                val chartW      = w - lp - rp
                val chartH      = h - tp - bp
                val chartBottom = tp + chartH
                val sectionW    = chartW / 3f

                fun yForWeight(wt: Float) = tp + (1f - (wt - axisMin) / axisRange) * chartH

                // Static: horizontal grid + Y-axis labels
                val gridColor   = Ink200.copy(alpha = 0.75f)
                val sectionDash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)
                repeat(3) { index ->
                    val fraction = index / 2f
                    val y = tp + fraction * chartH
                    drawLine(gridColor, Offset(lp, y), Offset(w - rp, y), strokeWidth = 1.dp.toPx())
                    val weightAtLine = axisMax - fraction * axisRange
                    val measured = textMeasurer.measure("%.0f".format(weightAtLine), axisLabelStyle)
                    drawText(measured, topLeft = Offset(w - rp + 5.dp.toPx(), y - measured.size.height / 2f))
                }
                // Single strip: stripMonths indices 0..4 = [center-2, center-1, center, center+1, center+2]
                // At dragXValue=0: index 1 → section 0, index 2 → section 1 (center), index 3 → section 2
                // x formula: dragXValue + lp + (i - 1 + 0.5) * sectionW = dragXValue + lp + (i - 0.5) * sectionW
                val stripOffsets: List<Offset?> = stripMonths.mapIndexed { i, ym ->
                    val cx = dragXValue + lp + (i - 0.5f) * sectionW
                    allMonthAvg[ym]?.let { avg -> Offset(cx, yForWeight(avg)) }
                }

                clipRect(lp, 0f, lp + chartW, h) {
                    // Separator lines scroll with content
                    for (j in 0..3) {
                        val x = dragXValue + lp + j * sectionW
                        drawLine(Color.Gray.copy(alpha = 0.35f), Offset(x, tp), Offset(x, chartBottom),
                            strokeWidth = 1.dp.toPx(), pathEffect = sectionDash)
                    }

                    // Build line segments (consecutive non-null runs)
                    val segments = mutableListOf<List<Offset>>()
                    var seg = mutableListOf<Offset>()
                    stripOffsets.forEach { pt ->
                        if (pt != null) seg.add(pt)
                        else { if (seg.size >= 2) segments.add(seg.toList()); seg = mutableListOf() }
                    }
                    if (seg.size >= 2) segments.add(seg.toList())

                    segments.forEach { segPts ->
                        drawPath(
                            Path().apply {
                                moveTo(segPts.first().x, chartBottom)
                                segPts.forEach { lineTo(it.x, it.y) }
                                lineTo(segPts.last().x, chartBottom)
                                close()
                            },
                            color = ForestGreen.copy(alpha = 0.15f)
                        )
                        drawPath(
                            Path().apply {
                                moveTo(segPts.first().x, segPts.first().y)
                                segPts.drop(1).forEach { lineTo(it.x, it.y) }
                            },
                            color = ForestGreen,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Dots for all strip indices (clipRect clips off-screen ones)
                    for (i in 0..4) {
                        stripOffsets[i]?.let { pt ->
                            drawCircle(color = ForestGreen, radius = 4.dp.toPx(), center = pt)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                        }
                    }

                    // Selection line (only at rest — selection clears on drag)
                    selectedQuarterIdx?.let { si ->
                        val cx = lp + (si + 0.5f) * sectionW
                        drawLine(Color.Gray.copy(alpha = 0.35f), Offset(cx, 0f), Offset(cx, chartBottom), 1.5.dp.toPx())
                    }

                    // Month labels for all strip indices (clipRect clips off-screen ones)
                    for (i in 0..4) {
                        val ym  = stripMonths[i]
                        val cx  = dragXValue + lp + (i - 0.5f) * sectionW
                        val lbl = textMeasurer.measure("Th${ym.monthValue}", axisLabelStyle)
                        drawText(lbl, topLeft = Offset(cx - lbl.size.width / 2f, chartBottom + 4.dp.toPx()))
                    }
                }
            }
        }
    }
}

private fun BodyMetricDto.metricLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
