package com.vitalai.ui.screens.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.ui.theme.BorderGrey
import com.vitalai.ui.theme.CreamCanvas
import com.vitalai.ui.theme.DarkCharcoal
import com.vitalai.ui.theme.ForestGreen
import com.vitalai.ui.theme.InkText
import com.vitalai.ui.theme.KeylimeWash
import com.vitalai.ui.theme.MintGlaze
import com.vitalai.ui.theme.MintKiss as ThemeMintKiss
import com.vitalai.ui.theme.SlateMist
import com.vitalai.ui.theme.VitalDisplayFontFamily
import com.vitalai.ui.theme.VitalFontFamily
import kotlin.math.roundToInt

internal object FoodModule {
    val Forest: Color = ForestGreen
    val Cream: Color = CreamCanvas
    val Mint: Color = MintGlaze
    val Slate: Color = SlateMist
    val Keylime: Color = KeylimeWash
    val MintKiss: Color = ThemeMintKiss
    val Border: Color = BorderGrey
    val Ink: Color = InkText
    val Charcoal: Color = DarkCharcoal
    val CardRadius = 14.dp
    val Gap = 21.dp
}

@Composable
internal fun FoodPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = FoodModule.Cream,
    color: Color = FoodModule.Forest,
    borderColor: Color? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(999.dp)) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontFamily = VitalFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun FoodPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(FoodModule.CardRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = FoodModule.Forest,
            contentColor = FoodModule.Cream,
            disabledContainerColor = FoodModule.Forest.copy(alpha = 0.35f),
            disabledContentColor = FoodModule.Cream.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = VitalFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun FoodThumb(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    circle: Boolean = true
) {
    val shape = if (circle) CircleShape else RoundedCornerShape(FoodModule.CardRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(FoodModule.Keylime),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FoodThumbFallback(name = name, size = size, circle = circle)
        }
    }
}

@Composable
internal fun FoodThumbFallback(
    name: String,
    size: Dp,
    circle: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = listOf(FoodModule.Keylime, FoodModule.Mint, FoodModule.Slate, FoodModule.MintKiss)
    val bg = colors[(name.sumOf { it.code }.takeIf { it >= 0 } ?: 0) % colors.size]
    val shape = if (circle) CircleShape else RoundedCornerShape(FoodModule.CardRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials(name),
            color = FoodModule.Forest,
            fontSize = (size.value * 0.34f).roundToInt().sp,
            fontFamily = VitalFontFamily,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
internal fun FoodProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(FoodModule.Forest.copy(alpha = 0.13f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(FoodModule.Forest)
        )
    }
}

@Composable
internal fun CalorieRing(consumed: Float, goal: Float, modifier: Modifier = Modifier, size: Dp = 132.dp) {
    val safeGoal = goal.takeIf { it > 0f } ?: 1f
    val progress = (consumed / safeGoal).coerceIn(0f, 1f)
    val left = (goal - consumed).roundToInt()
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val radius = (this.size.minDimension - stroke) / 2f
            drawCircle(
                color = FoodModule.Forest.copy(alpha = 0.13f),
                radius = radius,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = FoodModule.Forest,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (left < 0) "+${-left}" else "$left",
                color = FoodModule.Forest,
                fontSize = 38.sp,
                lineHeight = 38.sp,
                fontFamily = VitalDisplayFontFamily,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = if (left < 0) "kcal vượt" else "kcal còn",
                color = FoodModule.Charcoal,
                fontSize = 11.sp,
                fontFamily = VitalFontFamily
            )
        }
    }
}

internal fun initials(name: String): String {
    val words = name
        .replace(Regex("[^\\p{L}\\p{Nd} ]"), "")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return buildString {
        append(words.getOrNull(0)?.firstOrNull() ?: 'V')
        append(words.getOrNull(1)?.firstOrNull() ?: "")
    }.uppercase()
}

internal fun Float.cleanNumber(): String {
    return if (this == this.toInt().toFloat()) this.toInt().toString() else String.format("%.1f", this)
}
