package com.example.budgettracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.budgettracker.data.repository.Accent
import com.example.budgettracker.data.repository.ThemeMode

private fun accentColor(accent: Accent): Color = when (accent) {
    Accent.EMERALD -> EmeraldGreen
    Accent.OCEAN -> OceanBlue
    Accent.SUNSET -> SunsetOrange
}

private fun scheme(dark: Boolean, accent: Accent) = run {
    val primary = accentColor(accent)
    // Emerald, ocean, and sunset all sit under the 0.55 cutoff, which would pick white.
    // Dark text is the readable choice already used on emerald buttons.
    val onPrimary = TextPrimaryLight
    if (dark) {
        darkColorScheme(
            primary = primary,
            secondary = CatSoftBlue,
            tertiary = CatTeal,
            background = BackgroundDark,
            surface = SurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onPrimary = onPrimary,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = CatSoftBlue,
            tertiary = CatTeal,
            background = BackgroundLight,
            surface = SurfaceLight,
            surfaceVariant = SurfaceVariantLight,
            onPrimary = onPrimary,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            error = ErrorRed
        )
    }
}

@Composable
fun BudgetTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.EMERALD,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> scheme(darkTheme, accent)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}