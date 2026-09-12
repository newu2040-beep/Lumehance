package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Apple Pro Palette - Dark
val AppleProDarkBg = Color(0xFF090A0C)
val AppleProDarkSurface = Color(0xFF141519)
val AppleProDarkSurfaceElevated = Color(0xFF1F2128)
val AppleProDarkSurfaceCard = Color(0xFF262832)
val AppleProDarkBorder = Color(0xFF2C2F3A)
val AppleProDarkTextPrimary = Color(0xFFF5F6F8)
val AppleProDarkTextSecondary = Color(0xFF9699A3)
val AppleProDarkTextTertiary = Color(0xFF646775)

// Apple Pro Palette - Light
val AppleProLightBg = Color(0xFFF6F7F9)
val AppleProLightSurface = Color(0xFFFFFFFF)
val AppleProLightSurfaceElevated = Color(0xFFFFFFFF)
val AppleProLightSurfaceCard = Color(0xFFF0F2F6)
val AppleProLightBorder = Color(0xFFE2E4E9)
val AppleProLightTextPrimary = Color(0xFF111318)
val AppleProLightTextSecondary = Color(0xFF686C78)
val AppleProLightTextTertiary = Color(0xFF9AA0AC)

// Curated Accent Colors
enum class AccentTheme(val id: String, val title: String, val color: Color, val darkColor: Color) {
    CYAN("cyan", "Electric Cyan", Color(0xFF0071E3), Color(0xFF0A84FF)),
    MINT("mint", "Neon Mint", Color(0xFF28A745), Color(0xFF30D158)),
    PURPLE("purple", "Ultra Violet", Color(0xFF8944AB), Color(0xFFBF5AF2)),
    ORANGE("orange", "Solar Orange", Color(0xFFFF9500), Color(0xFFFF9F0A)),
    ROSE("rose", "Rose Luster", Color(0xFFFF2D55), Color(0xFFFF375F)),
    ICE("ice", "Ice Blue", Color(0xFF00A2E8), Color(0xFF64D2FF));

    companion object {
        fun fromId(id: String): AccentTheme = entries.find { it.id == id } ?: CYAN
    }
}
