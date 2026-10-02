package com.inferra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==============================================================================
// Executive Light Palette (Warm Paper White & Charcoal)
// ==============================================================================
val PaperWhiteBg = Color(0xFFF9FAFB)
val PorcelainSurface = Color(0xFFFFFFFF)
val ElevatedSurfaceLight = Color(0xFFF3F4F6)
val BorderLight = Color(0xFFE5E7EB)

val CharcoalTextPrimary = Color(0xFF111827)
val SlateTextSecondary = Color(0xFF4B5563)
val MutedTextLight = Color(0xFF9CA3AF)

// ==============================================================================
// Executive Dark Palette (Deep Obsidian & Slate)
// ==============================================================================
val ObsidianBg = Color(0xFF0D0F12)
val SlateSurface = Color(0xFF16191E)
val ElevatedSurfaceDark = Color(0xFF21262D)
val BorderDark = Color(0xFF2B323B)

val CrispTextPrimary = Color(0xFFF9FAFB)
val CrispTextSecondary = Color(0xFF9CA3AF)
val MutedTextDark = Color(0xFF6B7280)

// ==============================================================================
// Accent & Domain Categorical Highlights
// ==============================================================================
val KleinBluePrimary = Color(0xFF2563EB)
val KleinBlueSecondary = Color(0xFF3B82F6)
val KleinBlueSubtle = Color(0x1A2563EB)

// Categorical Domain Highlights
val SageGreen = Color(0xFF059669)
val SageGreenDark = Color(0xFF10B981)
val SageGreenSubtle = Color(0x1A10B981)

val SlateBlue = Color(0xFF2563EB)
val SlateBlueDark = Color(0xFF60A5FA)
val SlateBlueSubtle = Color(0x1A60A5FA)

val WarmAmber = Color(0xFFD97706)
val WarmAmberDark = Color(0xFFFBBF24)
val WarmAmberSubtle = Color(0x1AFBBF24)

// Legacy alias mapping for Theme compatibility
val InkBg = ObsidianBg
val InkSurface = SlateSurface
val InkCard = ElevatedSurfaceDark

val GlassMaterial = Color(0x0CFFFFFF)
val GlassHover = Color(0x18FFFFFF)
val GlassBorder = Color(0x26FFFFFF)
val GlassBorderFocused = Color(0x50FFFFFF)

val TextPrimary = CrispTextPrimary
val TextSecondary = CrispTextSecondary
val TextMuted = MutedTextDark

val AccentAzure = KleinBlueSecondary
val AccentMuted = Color(0xFF818CF8)

val FitExcellent = SageGreenDark
val FitBorderline = WarmAmberDark
val FitInsufficient = Color(0xFFF87171)

// ==============================================================================
// Material 3 ColorScheme Semantic Extensions for Glass & Domain UI
// ==============================================================================
val ColorScheme.glassCardBackground: Color
    @Composable get() = if (surface == ObsidianBg || surface == SlateSurface) SlateSurface else PorcelainSurface

val ColorScheme.glassCardBorder: Color
    @Composable get() = if (surface == ObsidianBg || surface == SlateSurface) BorderDark else BorderLight

val ColorScheme.textPrimaryColor: Color
    @Composable get() = onSurface

val ColorScheme.textSecondaryColor: Color
    @Composable get() = onSurfaceVariant

val ColorScheme.textMutedColor: Color
    @Composable get() = outline
