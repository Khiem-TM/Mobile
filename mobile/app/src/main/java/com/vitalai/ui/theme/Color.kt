package com.vitalai.ui.theme

import androidx.compose.ui.graphics.Color

// Primary mint scale
val Mint50 = Color(0xFFECFDF5)
val Mint100 = Color(0xFFD1FAE5)
val Mint200 = Color(0xFFA7F3D0)
val Mint500 = Color(0xFF10B981)
val Mint600 = Color(0xFF059669)
val Mint700 = Color(0xFF047857)
val Mint900 = Color(0xFF064E3B)

// Primary action teal (actual primary used throughout the app)
val Teal500 = Color(0xFF38C182)

// Neutrals
val Ink900 = Color(0xFF111827)
val Ink700 = Color(0xFF374151)
val Ink500 = Color(0xFF6B7280)
val Ink300 = Color(0xFFD1D5DB)
val Ink200 = Color(0xFFE5E7EB)
val Ink100 = Color(0xFFF3F4F6)
val AppBackground = Color(0xFFF9FAFB)
val AppSurface = Color(0xFFFFFFFF)

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
val TextBody = Ink700
val TextMuted = Ink500
val BorderColor = Ink200
val GreenCTA = Mint900
