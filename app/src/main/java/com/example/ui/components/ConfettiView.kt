package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val color: Color,
    val size: Float,
    val rotation: Float
)

@Composable
fun ConfettiView(
    trigger: Boolean,
    onFinished: () -> Unit = {}
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFF6366F1),
            Color(0xFF06B6D4),
            Color(0xFFEC4899),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFF8B5CF6)
        )
        List(60) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.3f,
                speedY = 1.0f + Random.nextFloat() * 0.8f,
                speedX = (Random.nextFloat() - 0.5f) * 0.4f,
                color = colors[Random.nextInt(colors.size)],
                size = 12f + Random.nextFloat() * 14f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val currY = (particle.initialY + (particle.speedY * p)) * canvasHeight
            val currX = (particle.initialX + (particle.speedX * p)) * canvasWidth

            if (currY in 0f..canvasHeight) {
                drawRect(
                    color = particle.color.copy(alpha = 1f - (p * 0.8f)),
                    topLeft = Offset(currX, currY),
                    size = Size(particle.size, particle.size * 0.6f)
                )
            }
        }
    }
}
