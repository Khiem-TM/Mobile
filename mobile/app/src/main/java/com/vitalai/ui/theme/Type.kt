package com.vitalai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.vitalai.R

val VitalFontFamily = FontFamily(Font(R.font.inter_variable))
val VitalDisplayFontFamily = FontFamily(Font(R.font.inter_tight_variable))
val VitalNumberFontFamily = VitalFontFamily

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = VitalDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = Ink900,
        lineHeightStyle = TightLineHeight
    ),
    headlineLarge = TextStyle(
        fontFamily = VitalDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
        color = Ink900,
        lineHeightStyle = TightLineHeight
    ),
    headlineMedium = TextStyle(
        fontFamily = VitalDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        color = Ink900,
        lineHeightStyle = TightLineHeight
    ),
    titleLarge = TextStyle(
        fontFamily = VitalDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = Ink900
    ),
    titleMedium = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        color = Ink900
    ),
    bodyLarge = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = TextBody
    ),
    bodyMedium = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = Ink700
    ),
    labelLarge = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = Ink900
    ),
    labelMedium = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        color = Ink700
    ),
    labelSmall = TextStyle(
        fontFamily = VitalFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        color = TextMuted
    )
)
