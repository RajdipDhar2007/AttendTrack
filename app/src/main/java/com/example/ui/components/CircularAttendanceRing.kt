package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularAttendanceRing(
    percentage: Float,
    targetPercentage: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    strokeWidth: Dp = 16.dp,
    showSubtext: Boolean = true
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    val progressColor = when {
        animatedPercent >= targetPercentage -> AttendSuccess
        animatedPercent >= targetPercentage - 10f -> AttendWarning
        else -> AttendDanger
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)

            // Background Track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Gradient Sweep Arc
            val sweepAngle = (animatedPercent / 100f) * 360f
            if (sweepAngle > 0f) {
                val gradientBrush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor,
                        progressColor,
                        progressColor.copy(alpha = 0.9f)
                    ),
                    center = center
                )

                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // Target Indicator Notch (Draw small circle/line at target % angle)
            val targetAngle = (-90f + (targetPercentage / 100f) * 360f)
            val targetRad = Math.toRadians(targetAngle.toDouble())
            val targetX = center.x + radius * cos(targetRad).toFloat()
            val targetY = center.y + radius * sin(targetRad).toFloat()

            drawCircle(
                color = Color.White,
                radius = strokePx / 3.5f,
                center = Offset(targetX, targetY)
            )
        }

        // Inner Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f%%", animatedPercent),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showSubtext) {
                Spacer(modifier = Modifier.height(2.dp))
                val statusText = when {
                    animatedPercent >= targetPercentage -> "Goal Met"
                    animatedPercent >= targetPercentage - 5f -> "Close to Goal"
                    else -> "Attendance Low"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = progressColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: ${targetPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
