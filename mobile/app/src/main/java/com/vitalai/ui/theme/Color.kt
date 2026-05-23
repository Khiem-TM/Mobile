package com.vitalai.ui.theme

import androidx.compose.ui.graphics.Color

// Primary mint scale
val Mint50 = Color(0xFFECFDF5)
val Mint100 = Color(0xFFD1FAE5)
val Mint200 = Color(0xFFA7F3D0)
val Mint300 = Color(0xFF6EE7B7)
val Mint400 = Color(0xFF34D399)
val Mint500 = Color(0xFF10B981)
val Mint600 = Color(0xFF059669)
val Mint700 = Color(0xFF047857)
val Mint800 = Color(0xFF065F46)
val Mint900 = Color(0xFF064E3B)

// Primary action teal (actual primary used throughout the app)
val Teal500 = Mint500

// Neutrals
val Ink900 = Color(0xFF0B1F17)
val Ink800 = Color(0xFF1A2A23)
val Ink700 = Color(0xFF2F3D36)
val Ink500 = Color(0xFF5A6A62)
val Ink400 = Color(0xFF8E9A93)
val Ink300 = Color(0xFFBFC8C3)
val Ink200 = Color(0xFFE6ECE9)
val Ink100 = Color(0xFFF2F6F4)
val Ink50 = Color(0xFFF8FAF9)
val AppBackground = Color(0xFFFFFFFF)
val AppMutedBackground = Color(0xFFF5F8F6)
val AppSurface = Color(0xFFFFFFFF)
val AppSurface2 = Color(0xFFF2F6F4)
val AppLine = Color(0xFFE6ECE9)
val AppLineSoft = Color(0xFFF0F4F2)

// Slate scale for dark headers / selected states
val Slate950 = Color(0xFF0F172A)
val Slate900 = Color(0xFF1E293B)

// Macro / nutrition colors
val MacroCarbs = Color(0xFFF59E0B)
val MacroProtein = Color(0xFFEF4444)
val MacroFat = Color(0xFF8B5CF6)
val MacroWater = Color(0xFF38BDF8)

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
