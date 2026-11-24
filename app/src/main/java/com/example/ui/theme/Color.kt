package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ChatX Primary Brand Palette — Fresh Emerald & Crisp White
val EmeraldPrimary = Color(0xFF00A884)
val EmeraldDark = Color(0xFF008069)
val EmeraldDeep = Color(0xFF064E3B)
val EmeraldMintContainer = Color(0xFFD7F7EC)
val EmeraldOnMintContainer = Color(0xFF002117)

// Custom Theme Presets
val TealPrimary = Color(0xFF0D9488)
val TealDark = Color(0xFF0F766E)
val TealContainer = Color(0xFFCCFBF1)

val ForestPrimary = Color(0xFF15803D)
val ForestDark = Color(0xFF166534)
val ForestContainer = Color(0xFFDCFCE7)

val JadePrimary = Color(0xFF059669)
val JadeDark = Color(0xFF047857)
val JadeContainer = Color(0xFFD1FAE5)

// Surfaces & Neutrals
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF7FAF8)
val LightSurfaceVariant = Color(0xFFEAF3EE)
val LightOutline = Color(0xFFD0DDD6)
val LightOnSurface = Color(0xFF111B21)
val LightOnSurfaceVariant = Color(0xFF54656F)

val DarkBackground = Color(0xFF0B141A)
val DarkSurface = Color(0xFF111B21)
val DarkSurfaceVariant = Color(0xFF1F2C34)
val DarkOutline = Color(0xFF2A3942)
val DarkOnSurface = Color(0xFFE9EDEF)
val DarkOnSurfaceVariant = Color(0xFF8696A0)

// Message Bubbles & Receipts
val OutgoingBubbleLight = Color(0xFFD9FDD3)
val OutgoingBubbleDark = Color(0xFF005C4B)
val IncomingBubbleLight = Color(0xFFFFFFFF)
val IncomingBubbleDark = Color(0xFF202C33)
val SecretBadgeColor = Color(0xFF0F766E)
val ReadTickBlue = Color(0xFF53BDEB)
val UnreadBadgeGreen = Color(0xFF25D366)
val CallAcceptGreen = Color(0xFF16A34A)
val CallDeclineRed = Color(0xFFDC2626)
val WarningAmber = Color(0xFFD97706)
val WarningContainerLight = Color(0xFFFEF3C7)
val WarningContainerDark = Color(0xFF451A03)

enum class ThemePreset(val id: String, val displayName: String, val lightPrimary: Color, val darkPrimary: Color, val container: Color) {
    EMERALD("emerald", "Emerald Classic", EmeraldPrimary, Color(0xFF25D366), EmeraldMintContainer),
    TEAL("teal", "Ocean Teal", TealPrimary, Color(0xFF2DD4BF), TealContainer),
    FOREST("forest", "Forest Mint", ForestPrimary, Color(0xFF4ADE80), ForestContainer),
    JADE("jade", "Royal Jade", JadePrimary, Color(0xFF34D399), JadeContainer);

    companion object {
        fun fromId(id: String): ThemePreset = entries.firstOrNull { it.id == id } ?: EMERALD
    }
}
