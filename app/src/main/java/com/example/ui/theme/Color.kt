package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base Brand Colors
val AttendPrimary = Color(0xFF6366F1)
val AttendSecondary = Color(0xFF06B6D4)
val AttendAccent = Color(0xFFEC4899)
val AttendSuccess = Color(0xFF10B981)
val AttendWarning = Color(0xFFF59E0B)
val AttendDanger = Color(0xFFEF4444)

// Palettes
// 1. AURORA
val AuroraPrimaryLight = Color(0xFF4F46E5)
val AuroraSecondaryLight = Color(0xFF0D9488)
val AuroraBackgroundLight = Color(0xFFF8FAFC)
val AuroraSurfaceLight = Color(0xFFFFFFFF)

val AuroraPrimaryDark = Color(0xFF818CF8)
val AuroraSecondaryDark = Color(0xFF2DD4BF)
val AuroraBackgroundDark = Color(0xFF0F172A)
val AuroraSurfaceDark = Color(0xFF1E293B)

// 2. SUNSET
val SunsetPrimaryLight = Color(0xFFE11D48)
val SunsetSecondaryLight = Color(0xFFEA580C)
val SunsetBackgroundLight = Color(0xFFFFF1F2)
val SunsetSurfaceLight = Color(0xFFFFFFFF)

val SunsetPrimaryDark = Color(0xFFFB7185)
val SunsetSecondaryDark = Color(0xFFFB923C)
val SunsetBackgroundDark = Color(0xFF1C1014)
val SunsetSurfaceDark = Color(0xFF2D1822)

// 3. OCEAN
val OceanPrimaryLight = Color(0xFF0284C7)
val OceanSecondaryLight = Color(0xFF0891B2)
val OceanBackgroundLight = Color(0xFFF0F9FF)
val OceanSurfaceLight = Color(0xFFFFFFFF)

val OceanPrimaryDark = Color(0xFF38BDF8)
val OceanSecondaryDark = Color(0xFF22D3EE)
val OceanBackgroundDark = Color(0xFF081A2D)
val OceanSurfaceDark = Color(0xFF0F2D4A)

// 4. NEON
val NeonPrimaryLight = Color(0xFF9333EA)
val NeonSecondaryLight = Color(0xFF059669)
val NeonBackgroundLight = Color(0xFFFAF5FF)
val NeonSurfaceLight = Color(0xFFFFFFFF)

val NeonPrimaryDark = Color(0xFFC084FC)
val NeonSecondaryDark = Color(0xFF34D399)
val NeonBackgroundDark = Color(0xFF0D0B14)
val NeonSurfaceDark = Color(0xFF1A1329)

// 5. FOREST
val ForestPrimaryLight = Color(0xFF059669)
val ForestSecondaryLight = Color(0xFF16A34A)
val ForestBackgroundLight = Color(0xFFF0FDF4)
val ForestSurfaceLight = Color(0xFFFFFFFF)

val ForestPrimaryDark = Color(0xFF34D399)
val ForestSecondaryDark = Color(0xFF4ADE80)
val ForestBackgroundDark = Color(0xFF051B11)
val ForestSurfaceDark = Color(0xFF0C2E20)

// 6. MONO
val MonoPrimaryLight = Color(0xFF334155)
val MonoSecondaryLight = Color(0xFF64748B)
val MonoBackgroundLight = Color(0xFFF8FAFC)
val MonoSurfaceLight = Color(0xFFFFFFFF)

val MonoPrimaryDark = Color(0xFF94A3B8)
val MonoSecondaryDark = Color(0xFFCBD5E1)
val MonoBackgroundDark = Color(0xFF090D14)
val MonoSurfaceDark = Color(0xFF141A24)

// AMOLED Backgrounds
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0B0E14)

// Glassmorphism Gradients
fun getAuroraGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0B1329)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFFF8FAFC)))
}

fun getSunsetGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF2A0815), Color(0xFF3E1124), Color(0xFF19060E)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFFF7ED)))
}

fun getOceanGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF031926), Color(0xFF072B4A), Color(0xFF02121E)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFF0F9FF)))
}

fun getNeonGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF13091F), Color(0xFF220E38), Color(0xFF0D0616)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFFDF4FF), Color(0xFFFAE8FF), Color(0xFFF5F3FF)))
}

fun getForestGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF03170E), Color(0xFF07291B), Color(0xFF02110A)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFF0FDF4)))
}

fun getMonoGradient(isDark: Boolean) = if (isDark) {
    Brush.verticalGradient(listOf(Color(0xFF0B0F17), Color(0xFF151C28), Color(0xFF070A0F)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFF8FAFC)))
}
