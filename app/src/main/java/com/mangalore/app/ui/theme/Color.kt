package com.mangalore.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ZTheme {
    val bg = Color(0xFF10141C)
    val surface = Color(0xFF161B26)
    val card = Color(0xFF1B2130)
    val cardHover = Color(0xFF232B3D)
    val border = Color(0xFF2A3245)
    val borderLight = Color(0xFF3A4459)
    val accent = Color(0xFFF5B301)
    val accentBright = Color(0xFFFFC933)
    val accentDim = Color(0xFFF5B301).copy(alpha = 0.15f)
    val textPrimary = Color(0xFFF2F4F8)
    val textSecondary = Color(0xFF9099AC)
    val textTertiary = Color(0xFF5B6478)
    val danger = Color(0xFFEF5D6F)
    val success = Color(0xFF4CD498)
    val warning = Color(0xFFF5B301)

    val goldGradient = Brush.linearGradient(listOf(accentBright, accent))
    val newBadgeGradient = Brush.linearGradient(listOf(accent, Color(0xFFE89A00)))
}
