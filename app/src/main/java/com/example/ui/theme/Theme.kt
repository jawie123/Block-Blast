package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GameDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF001F24),
    primaryContainer = Color(0xFF1E2640),
    onPrimaryContainer = Color(0xFFB4E5FF),
    secondary = Color(0xFFFFD700),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF33290A),
    onSecondaryContainer = Color(0xFFFFE082),
    background = DarkBackground,
    onBackground = Color(0xFFECEFF4),
    surface = BoardBackground,
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = CellEmpty,
    onSurfaceVariant = Color(0xFFB0B7C6)
)

@Composable
fun BlockBlastTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GameDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
