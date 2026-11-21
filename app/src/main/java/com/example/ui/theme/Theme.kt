package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SovereignGoldBright,
    onPrimary = SovereignNavyDeep,
    primaryContainer = SovereignGoldMuted,
    onPrimaryContainer = Color.White,
    secondary = ClearanceEmerald,
    onSecondary = SovereignNavyDeep,
    secondaryContainer = ClearanceEmeraldContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = TacticalCyan,
    onTertiary = SovereignNavyDeep,
    background = SovereignNavyDeep,
    onBackground = Color(0xFFF1F5F9),
    surface = SovereignNavy,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SovereignSlate,
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainer = SovereignSlate,
    surfaceContainerHigh = SovereignSlateElevated,
    outline = SovereignBorder,
    error = CriticalCrimson,
    onError = Color.White,
    errorContainer = CriticalCrimsonContainer,
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = SovereignNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE7F5),
    onPrimaryContainer = SovereignNavyDeep,
    secondary = DaylightEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = DaylightGoldPrimary,
    onTertiary = Color.White,
    background = DaylightBackground,
    onBackground = DaylightInkPrimary,
    surface = DaylightSurface,
    onSurface = DaylightInkPrimary,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = DaylightInkSecondary,
    surfaceContainer = DaylightSurface,
    surfaceContainerHigh = DaylightSurfaceVariant,
    outline = DaylightOutline,
    error = DaylightCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BorderPortsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
