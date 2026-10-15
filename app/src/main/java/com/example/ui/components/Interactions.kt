package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Springy press-down scale plus a haptic tick. Use instead of plain clickable. */
fun Modifier.bounceClick(
    pressedScale: Float = 0.92f,
    haptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val feedback = LocalHapticFeedback.current
    val s by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce_click"
    )
    this
        .scale(s)
        .clickable(interactionSource = source, indication = null) {
            if (haptic) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Fade + slide-up entrance, delayed by [index] so lists cascade in. Plays once. */
fun Modifier.staggeredEntrance(index: Int, stepMillis: Int = 70): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = 450, delayMillis = (index * stepMillis).coerceAtMost(600), easing = FastOutSlowInEasing)
        )
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 48f
    }
}

/** Number that counts smoothly to its new value whenever it changes. */
@Composable
fun AnimatedCountText(
    value: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified
) {
    val shown by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "count_text"
    )
    Text(
        text = String.format("%.${decimals}f", shown) + suffix,
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color
    )
}

/** Progress bar whose fill grows from empty on first show and eases on every change. */
@Composable
fun AnimatedProgressBar(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
) {
    val start = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        start.animateTo(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    val animatedColor by animateColorAsState(fillColor, tween(400), label = "progress_color")
    val shape = RoundedCornerShape(height / 2)
    Box(modifier = modifier.fillMaxWidth().height(height).clip(shape).background(trackColor)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(start.value.coerceAtLeast(0.001f))
                .height(height)
                .clip(shape)
                .background(animatedColor)
        )
    }
}
