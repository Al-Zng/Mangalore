package com.mangalore.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MangaloreColorScheme = darkColorScheme(
    primary = ZTheme.accent,
    onPrimary = ZTheme.bg,
    secondary = ZTheme.accentBright,
    background = ZTheme.bg,
    onBackground = ZTheme.textPrimary,
    surface = ZTheme.surface,
    onSurface = ZTheme.textPrimary,
    surfaceVariant = ZTheme.card,
    onSurfaceVariant = ZTheme.textSecondary,
    error = ZTheme.danger
)

@Composable
fun MangaloreTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            it.statusBarColor = ZTheme.bg.toArgb()
            it.navigationBarColor = ZTheme.bg.toArgb()
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = MangaloreColorScheme,
        typography = MangaloreTypography,
        content = content
    )
}
