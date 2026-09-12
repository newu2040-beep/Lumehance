package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class ExtendedColors(
    val surfaceGlass: Color,
    val surfaceGlassBorder: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val subtleBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val isDark: Boolean
)

val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(
        surfaceGlass = Color.White.copy(alpha = 0.85f),
        surfaceGlassBorder = Color.White.copy(alpha = 0.2f),
        cardSurface = AppleProLightSurfaceCard,
        elevatedSurface = AppleProLightSurfaceElevated,
        subtleBorder = AppleProLightBorder,
        textPrimary = AppleProLightTextPrimary,
        textSecondary = AppleProLightTextSecondary,
        textTertiary = AppleProLightTextTertiary,
        accent = AccentTheme.CYAN.color,
        isDark = false
    )
}

object LumenhanceTheme {
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@Composable
fun LumenhanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: AccentTheme = AccentTheme.CYAN,
    content: @Composable () -> Unit
) {
    val targetAccent = if (darkTheme) accentTheme.darkColor else accentTheme.color
    val animatedAccent = animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 250),
        label = "accent"
    ).value

    val animatedBg = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkBg else AppleProLightBg,
        animationSpec = tween(durationMillis = 250),
        label = "bg"
    ).value

    val animatedSurface = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkSurface else AppleProLightSurface,
        animationSpec = tween(durationMillis = 250),
        label = "surface"
    ).value

    val animatedCard = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkSurfaceCard else AppleProLightSurfaceCard,
        animationSpec = tween(durationMillis = 250),
        label = "card"
    ).value

    val animatedElevated = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkSurfaceElevated else AppleProLightSurfaceElevated,
        animationSpec = tween(durationMillis = 250),
        label = "elevated"
    ).value

    val animatedBorder = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkBorder else AppleProLightBorder,
        animationSpec = tween(durationMillis = 250),
        label = "border"
    ).value

    val animatedTextPrimary = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkTextPrimary else AppleProLightTextPrimary,
        animationSpec = tween(durationMillis = 250),
        label = "textPrimary"
    ).value

    val animatedTextSecondary = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkTextSecondary else AppleProLightTextSecondary,
        animationSpec = tween(durationMillis = 250),
        label = "textSecondary"
    ).value

    val animatedTextTertiary = animateColorAsState(
        targetValue = if (darkTheme) AppleProDarkTextTertiary else AppleProLightTextTertiary,
        animationSpec = tween(durationMillis = 250),
        label = "textTertiary"
    ).value

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = animatedAccent,
            onPrimary = Color.Black,
            primaryContainer = animatedAccent.copy(alpha = 0.2f),
            onPrimaryContainer = animatedAccent,
            background = animatedBg,
            onBackground = animatedTextPrimary,
            surface = animatedSurface,
            onSurface = animatedTextPrimary,
            surfaceVariant = animatedCard,
            onSurfaceVariant = animatedTextSecondary,
            outline = animatedBorder,
            outlineVariant = animatedBorder.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = animatedAccent,
            onPrimary = Color.White,
            primaryContainer = animatedAccent.copy(alpha = 0.12f),
            onPrimaryContainer = animatedAccent,
            background = animatedBg,
            onBackground = animatedTextPrimary,
            surface = animatedSurface,
            onSurface = animatedTextPrimary,
            surfaceVariant = animatedCard,
            onSurfaceVariant = animatedTextSecondary,
            outline = animatedBorder,
            outlineVariant = animatedBorder.copy(alpha = 0.5f)
        )
    }

    val extendedColors = ExtendedColors(
        surfaceGlass = if (darkTheme) Color(0xFF181A20).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f),
        surfaceGlassBorder = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
        cardSurface = animatedCard,
        elevatedSurface = animatedElevated,
        subtleBorder = animatedBorder,
        textPrimary = animatedTextPrimary,
        textSecondary = animatedTextSecondary,
        textTertiary = animatedTextTertiary,
        accent = animatedAccent,
        isDark = darkTheme
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
