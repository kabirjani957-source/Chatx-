package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class DarkModePreference(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String): DarkModePreference = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

val ChatXShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ChatXTheme(
    darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    themePreset: ThemePreset = ThemePreset.EMERALD,
    content: @Composable () -> Unit
) {
    val isDark = when (darkModePreference) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themePreset.darkPrimary,
            onPrimary = Color(0xFF003828),
            primaryContainer = Color(0xFF00513B),
            onPrimaryContainer = Color(0xFFA7F3D0),
            secondary = EmeraldPrimary,
            onSecondary = Color(0xFF002117),
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = DarkOnSurface,
            tertiary = ReadTickBlue,
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A)
        )
    } else {
        lightColorScheme(
            primary = themePreset.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = themePreset.container,
            onPrimaryContainer = EmeraldOnMintContainer,
            secondary = EmeraldDark,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightOnSurface,
            tertiary = ReadTickBlue,
            background = LightBackground,
            onBackground = LightOnSurface,
            surface = LightBackground,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline,
            error = CallDeclineRed,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChatXTypography,
        shapes = ChatXShapes,
        content = content
    )
}
