package com.vitalai.ui.theme

import androidx.compose.ui.graphics.Color

// ── Design system palette (Design/DESIGN.md — "Ease Health") ──────────────
// Forest Green is the single brand/action color; surfaces are cream + green tints.
val ForestGreen = Color(0xFF228C66)
val CreamCanvas = Color(0xFFFFFEFC)
val MintGlaze = Color(0xFFB1DBB8)
val SlateMist = Color(0xFFB6CED5)
val KeylimeWash = Color(0xFFE1F4DF)
val MintKiss = Color(0xFFCFE7D3)
val BorderGrey = Color(0xFFE5E7EB)
val InkText = Color(0xFF222222)
val DarkCharcoal = Color(0xFF333333)

// Primary "mint" scale. Mint500 is THE primary action color referenced across the
// app, so it maps to Forest Green; lighter steps map to the green surface tints.
val Mint50 = KeylimeWash
val Mint100 = MintKiss
val Mint200 = Color(0xFFA7F3D0)
val Mint300 = Color(0xFF6EE7B7)
val Mint400 = Color(0xFF2E7D43)
val Mint500 = ForestGreen
val Mint600 = Color(0xFF1B5E27)
val Mint700 = Color(0xFF17501F)
val Mint800 = Color(0xFF124019)
val Mint900 = ForestGreen

// Primary action teal (actual primary used throughout the app)
val Teal500 = Mint500

// Neutrals
val Ink900 = InkText
val Ink800 = DarkCharcoal
val Ink700 = Color(0xFF3F4A44)
val Ink500 = Color(0xFF5A6A62)
val Ink400 = Color(0xFF8E9A93)
val Ink300 = Color(0xFFBFC8C3)
val Ink200 = BorderGrey
val Ink100 = Color(0xFFF2F6F4)
val Ink50 = Color(0xFFF8FAF9)
val AppBackground = CreamCanvas
val AppMutedBackground = Color(0xFFF4F8F4)
val AppSurface = CreamCanvas
val AppSurface2 = Color(0xFFF1F6F1)
val AppLine = BorderGrey
val AppLineSoft = Color(0xFFF0F4F2)

// Slate scale for dark headers / selected states
val Slate950 = Color(0xFF0F172A)
val Slate900 = Color(0xFF1E293B)

// Macro / nutrition colors
val MacroCarbs = Color(0xFFF59E0B)
val MacroProtein = Color(0xFFEF4444)
val MacroFat = Color(0xFF8B5CF6)
val MacroWater = Color(0xFF257EA5)

// Domain-specific colors (water, meal badges)
val WaterBlue = Color(0xFF3B82F6)
val WaterBlueTint = Color(0xFFEFF6FF)
val MealTimeBg = Color(0xFFFCE7F3)
val MealTimeText = Color(0xFFBE185D)
val AmberContainer = Color(0xFFFEF3C7)
val AmberOnContainer = Color(0xFFB45309)
val AmberTint = Color(0xFFFFFBEB)

// Semantic containers (light/dark mode aware via Theme)
val ErrorContainerLight = Color(0xFFFEF2F2)
val ErrorContainerDark = Color(0xFF93000A)

// Legacy aliases — kept for backward compatibility with existing code
val GreenDark = Mint900
val GreenMid = Mint700
val GreenAccent = Teal500
val GreenLight = Mint100
val Background = AppBackground
val CardBackground = AppSurface
val Orange = MacroCarbs
val RedAccent = MacroProtein
val TextBody = Ink800
val TextMuted = Ink500
val BorderColor = Ink200
val GreenCTA = Mint900
