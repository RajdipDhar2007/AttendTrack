package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppPalette
import com.example.data.model.ThemeMode

// App-wide theme state holder
object AttendThemeState {
    var palette by mutableStateOf(AppPalette.AURORA)
    var mode by mutableStateOf(ThemeMode.DARK)
    var animationsEnabled by mutableStateOf(true)
}

val LocalAppPalette = compositionLocalOf { AppPalette.AURORA }
val LocalIsDarkTheme = compositionLocalOf { true }
val LocalIsAmoled = compositionLocalOf { false }

@Composable
fun AttendTrackTheme(
    palette: AppPalette = AttendThemeState.palette,
    mode: ThemeMode = AttendThemeState.mode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = mode == ThemeMode.AMOLED

    val colorScheme = when (palette) {
        AppPalette.AURORA -> if (isDark) {
            darkColorScheme(
                primary = AuroraPrimaryDark,
                secondary = AuroraSecondaryDark,
                background = if (isAmoled) AmoledBlack else AuroraBackgroundDark,
                surface = if (isAmoled) AmoledSurface else AuroraSurfaceDark,
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = Color(0xFFF1F5F9),
                onSurface = Color(0xFFF1F5F9)
            )
        } else {
            lightColorScheme(
                primary = AuroraPrimaryLight,
                secondary = AuroraSecondaryLight,
                background = AuroraBackgroundLight,
                surface = AuroraSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }
        AppPalette.SUNSET -> if (isDark) {
            darkColorScheme(
                primary = SunsetPrimaryDark,
                secondary = SunsetSecondaryDark,
                background = if (isAmoled) AmoledBlack else SunsetBackgroundDark,
                surface = if (isAmoled) AmoledSurface else SunsetSurfaceDark,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFFFF1F2),
                onSurface = Color(0xFFFFF1F2)
            )
        } else {
            lightColorScheme(
                primary = SunsetPrimaryLight,
                secondary = SunsetSecondaryLight,
                background = SunsetBackgroundLight,
                surface = SunsetSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1C1014),
                onSurface = Color(0xFF1C1014)
            )
        }
        AppPalette.OCEAN -> if (isDark) {
            darkColorScheme(
                primary = OceanPrimaryDark,
                secondary = OceanSecondaryDark,
                background = if (isAmoled) AmoledBlack else OceanBackgroundDark,
                surface = if (isAmoled) AmoledSurface else OceanSurfaceDark,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFF0F9FF),
                onSurface = Color(0xFFF0F9FF)
            )
        } else {
            lightColorScheme(
                primary = OceanPrimaryLight,
                secondary = OceanSecondaryLight,
                background = OceanBackgroundLight,
                surface = OceanSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF081A2D),
                onSurface = Color(0xFF081A2D)
            )
        }
        AppPalette.NEON -> if (isDark) {
            darkColorScheme(
                primary = NeonPrimaryDark,
                secondary = NeonSecondaryDark,
                background = if (isAmoled) AmoledBlack else NeonBackgroundDark,
                surface = if (isAmoled) AmoledSurface else NeonSurfaceDark,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFFAF5FF),
                onSurface = Color(0xFFFAF5FF)
            )
        } else {
            lightColorScheme(
                primary = NeonPrimaryLight,
                secondary = NeonSecondaryLight,
                background = NeonBackgroundLight,
                surface = NeonSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF13091F),
                onSurface = Color(0xFF13091F)
            )
        }
        AppPalette.FOREST -> if (isDark) {
            darkColorScheme(
                primary = ForestPrimaryDark,
                secondary = ForestSecondaryDark,
                background = if (isAmoled) AmoledBlack else ForestBackgroundDark,
                surface = if (isAmoled) AmoledSurface else ForestSurfaceDark,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFF0FDF4),
                onSurface = Color(0xFFF0FDF4)
            )
        } else {
            lightColorScheme(
                primary = ForestPrimaryLight,
                secondary = ForestSecondaryLight,
                background = ForestBackgroundLight,
                surface = ForestSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF051B11),
                onSurface = Color(0xFF051B11)
            )
        }
        AppPalette.MONO -> if (isDark) {
            darkColorScheme(
                primary = MonoPrimaryDark,
                secondary = MonoSecondaryDark,
                background = if (isAmoled) AmoledBlack else MonoBackgroundDark,
                surface = if (isAmoled) AmoledSurface else MonoSurfaceDark,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFF8FAFC)
            )
        } else {
            lightColorScheme(
                primary = MonoPrimaryLight,
                secondary = MonoSecondaryLight,
                background = MonoBackgroundLight,
                surface = MonoSurfaceLight,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF090D14),
                onSurface = Color(0xFF090D14)
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalIsDarkTheme provides isDark,
        LocalIsAmoled provides isAmoled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
