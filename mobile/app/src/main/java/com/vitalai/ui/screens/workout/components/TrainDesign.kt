package com.vitalai.ui.screens.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.ui.theme.VitalDisplayFontFamily
import kotlin.math.max

object TrainColors {
    val Forest = Color(0xFF0F3E17)
    val Cream = Color(0xFFFFFEFC)
    val MintGlaze = Color(0xFFB1DBB8)
    val SlateMist = Color(0xFFB6CED5)
    val KeylimeWash = Color(0xFFE1F4DF)
    val MintKiss = Color(0xFFCFE7D3)
    val Border = Color(0xFFE5E7EB)
    val Ink = Color(0xFF222222)
    val Charcoal = Color(0xFF333333)
    val Sport = Color(0xFFB8651F)
    val SportBg = Color(0xFFF4E3D2)
    val Cardio = Color(0xFFA83228)
    val CardioBg = Color(0xFFF3DDD8)
    val GymBg = Color(0xFFD7ECD9)
}

val TrainCardRadius = 14.dp
val TrainButtonRadius = 14.dp

@Composable
fun TrainScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrainColors.Cream),
        content = content
    )
}

@Composable
fun TrainHeader(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    right: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TrainColors.Cream.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, TrainColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBack != null) {
                TrainRoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    onClick = onBack,
                    background = TrainColors.KeylimeWash,
                    tint = TrainColors.Forest,
                    size = 38.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = if (large) 21.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TrainColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = TrainColors.Charcoal)
                }
            }
            right()
        }
    }
}

@Composable
fun TrainCard(
    modifier: Modifier = Modifier,
    tone: TrainTone = TrainTone.Cream,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = when (tone) {
        TrainTone.Cream -> TrainColors.Cream
        TrainTone.Keylime -> TrainColors.KeylimeWash
        TrainTone.Mint -> TrainColors.MintGlaze
        TrainTone.Slate -> TrainColors.SlateMist
        TrainTone.Kiss -> TrainColors.MintKiss
    }
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(TrainCardRadius),
        color = color,
        border = if (tone == TrainTone.Cream) BorderStroke(1.dp, TrainColors.Border) else null,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

enum class TrainTone { Cream, Keylime, Mint, Slate, Kiss }

@Composable
fun TrainPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(TrainButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrainColors.Forest,
            contentColor = TrainColors.Cream,
            disabledContainerColor = TrainColors.Forest.copy(alpha = 0.45f),
            disabledContentColor = TrainColors.Cream.copy(alpha = 0.72f)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        val contentColor = if (enabled) TrainColors.Cream else TrainColors.Cream.copy(alpha = 0.72f)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TrainGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(TrainButtonRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TrainColors.Forest),
        border = BorderStroke(1.5.dp, TrainColors.Forest)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TrainRoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    background: Color = TrainColors.Cream,
    tint: Color = if (active) TrainColors.Cream else TrainColors.Ink,
    activeBackground: Color = TrainColors.Forest,
    size: Dp = 40.dp,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) activeBackground else background)
            .border(1.dp, TrainColors.Border, CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun <T> TrainSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(TrainColors.KeylimeWash)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) TrainColors.Forest else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = if (small) 8.dp else 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) TrainColors.Cream else TrainColors.Charcoal,
                    fontSize = if (small) 12.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrainChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    grow: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .then(if (grow) Modifier.fillMaxWidth() else Modifier)
            .clip(CircleShape)
            .background(if (selected) TrainColors.Forest else TrainColors.Cream)
            .border(1.dp, if (selected) TrainColors.Forest else TrainColors.Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) TrainColors.Cream else TrainColors.Charcoal,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrainTypeBadge(type: String, modifier: Modifier = Modifier) {
    val (label, fg, bg) = when (type.uppercase()) {
        "CARDIO" -> Triple("CARDIO", TrainColors.Cardio, TrainColors.CardioBg)
        "SPORT" -> Triple("SPORT", TrainColors.Sport, TrainColors.SportBg)
        else -> Triple("GYM", TrainColors.Forest, TrainColors.GymBg)
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(label, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TrainExerciseThumb(
    exercise: ExerciseDto,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    radius: Dp = 12.dp
) {
    val bg = when (exercise.exerciseType.uppercase()) {
        "CARDIO" -> TrainColors.CardioBg
        "SPORT" -> TrainColors.SportBg
        else -> TrainColors.GymBg
    }
    val fg = when (exercise.exerciseType.uppercase()) {
        "CARDIO" -> TrainColors.Cardio
        "SPORT" -> TrainColors.Sport
        else -> TrainColors.Forest
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (exercise.displayImageUrl != null) {
            AsyncImage(
                model = exercise.displayImageUrl,
                contentDescription = exercise.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = when (exercise.exerciseType.uppercase()) {
                    "CARDIO" -> Icons.AutoMirrored.Filled.DirectionsRun
                    "SPORT" -> Icons.Default.Pool
                    else -> Icons.Default.FitnessCenter
                },
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

@Composable
fun TrainStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    tone: TrainTone = TrainTone.Keylime
) {
    TrainCard(modifier = modifier, tone = tone, padding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TrainColors.Cream.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(19.dp))
            }
            Column {
                Text(value, color = TrainColors.Forest, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                Text(label, color = TrainColors.Charcoal, fontSize = 12.sp)
                if (sub != null) Text(sub, color = TrainColors.Charcoal.copy(alpha = 0.7f), fontSize = 10.5.sp)
            }
        }
    }
}

@Composable
fun TrainSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = TrainColors.Charcoal,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TrainBarChart(
    values: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1
) {
    val maxValue = max(values.maxOfOrNull { it.second } ?: 0f, 1f)
    Row(
        modifier = modifier.height(136.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, item ->
            val active = item.second > 0f
            val heightFraction = if (active) (item.second / maxValue).coerceIn(0.08f, 1f) else 0.04f
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier.height(20.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Text(
                            compactKcalLabel(item.second),
                            color = TrainColors.Forest,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(82.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .heightIn(min = 4.dp)
                    ) {
                        val barWidth = size.width.coerceAtMost(24.dp.toPx())
                        drawRoundRect(
                            color = when {
                                !active -> TrainColors.Border
                                index == highlightIndex -> TrainColors.Forest
                                else -> TrainColors.MintGlaze
                            },
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = (size.width - barWidth) / 2f,
                                y = 0f
                            ),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    item.first,
                    color = TrainColors.Charcoal.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun compactKcalLabel(value: Float): String {
    val rounded = value.toInt()
    return if (rounded >= 1000) {
        val oneDecimal = rounded / 100f / 10f
        if (rounded % 1000 >= 100) "${oneDecimal}k" else "${rounded / 1000}k"
    } else {
        rounded.toString()
    }
}

@Composable
fun TrainDisplayTitle(text: String, modifier: Modifier = Modifier, color: Color = TrainColors.Forest) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = VitalDisplayFontFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp
    )
}
