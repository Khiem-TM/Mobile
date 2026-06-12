package com.vitalai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.*

@Composable
fun VitalScreen(
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    tintMint: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = when {
        tintMint -> Brush.radialGradient(
            colors = listOf(Mint50, Color.Transparent),
            center = Offset.Zero,
            radius = 900f
        )
        else -> null
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (bg != null) Modifier.background(bg)
                else Modifier.background(if (muted) AppMutedBackground else AppBackground)
            ),
        content = content
    )
}

@Composable
fun VitalScroll(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = VitalSpacing.ScreenBottom
    ),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun VitalCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    borderColor: Color = Color.Transparent,
    radius: Dp = VitalRadius.Md,
    contentPadding: PaddingValues = PaddingValues(VitalSpacing.Card),
    elevation: Dp = VitalElevation.Level2,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
        tonalElevation = 0.dp,
        shadowElevation = elevation
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun VitalBorderCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    borderColor: Color = AppLine,
    radius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(radius),
        color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun VitalFlatCard(
    modifier: Modifier = Modifier,
    color: Color = AppSurface2,
    radius: Dp = VitalRadius.Md,
    contentPadding: PaddingValues = PaddingValues(VitalSpacing.Card),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(color)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun VitalPill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    mint: Boolean = false,
    color: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val bg = when {
        selected -> Ink900
        mint -> Mint100
        color != null -> color.copy(alpha = 0.14f)
        else -> AppSurface2
    }
    val fg = when {
        selected -> Color.White
        mint -> Mint700
        color != null -> color
        else -> Ink700
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VitalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mint: Boolean = false,
    primary: Boolean = false,
    enabled: Boolean = true,
    textColor: Color? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(VitalRadius.Md),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (mint) Mint500 else if (primary) Ink900 else AppSurface2,
            contentColor = textColor ?: (if (mint || primary) Color.White else Ink900),
            disabledContainerColor = Ink200,
            disabledContentColor = Ink500
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LocalContentColor.current)
    }
}

@Composable
fun VitalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppSurface2,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Ink900,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = color)
        if (action != null && onAction != null) {
            Text(
                action,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Mint600,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun SegmentedPills(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Ink900
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .clickable { onSelected(key) }
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
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Mint700,
    subtitle: String? = null
) {
    VitalFlatCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Ink500, modifier = Modifier.padding(top = 2.dp))
        if (subtitle != null) {
            Text(subtitle, fontSize = 10.sp, color = Ink400, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun MacroBar(
    name: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String = "g"
) {
    val progress = if (target <= 0f) 0f else (current / target).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink700)
            Text("${current.toInt()}/${target.toInt()} $unit", fontSize = 12.sp, color = Ink500)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(VitalRadius.Pill))
                .background(Ink100)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(color)
            )
        }
    }
}

@Composable
fun ArcGauge(
    value: Float,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 204.dp,
    stroke: Dp = 18.dp,
    color: Color = Mint500,
    track: Color = Ink100
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * value.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink900)
            Text(sublabel, fontSize = 13.sp, color = Ink500, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun VitalScreenHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink900)
                if (subtitle != null) {
                    Text(subtitle, color = Ink700, fontSize = 14.sp)
                }
            }

            actions()
        }
        Divider(color = Ink100, thickness = 1.dp)
    }
}
