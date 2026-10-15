package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    alpha: Float = 0.58f,
    borderAlpha: Float = 0.28f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val baseColor = if (isDark) surfaceColor.copy(alpha = alpha) else Color.White.copy(alpha = 0.78f)

    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) borderAlpha + 0.15f else 0.5f),
            Color.White.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f)
        )
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f)
            )
            .clip(shape)
            .background(baseColor)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}
