package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SlotWithDetails
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.viewmodel.ActiveClassInfo
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalTime

private fun title(code: String, name: String, type: Any): String =
    (if (code.isNotBlank()) "$code - $name" else name) + " [$type]"

private fun fmt(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%02d:%02d", m, sec)
}

/**
 * Animated card showing the class that is happening right now with a live ring countdown,
 * or (when idle) the next class with a "starts in" countdown.
 */
@Composable
fun LiveClassCountdownCard(
    activeClass: ActiveClassInfo?,
    nextClass: SlotWithDetails?,
    modifier: Modifier = Modifier
) {
    // Own 1s ticker so the "starts in" countdown is live even when no class is active
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }

    // 0 = live, 1 = upcoming, 2 = done for today
    val mode = when {
        activeClass != null -> 0
        nextClass != null -> 1
        else -> 2
    }

    val accent = when (mode) {
        0 -> AttendSuccess
        1 -> MaterialTheme.colorScheme.primary
        else -> AttendWarning
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.7f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot"
    )
    val glow by pulse.animateFloat(
        initialValue = 0.25f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    GlassCard(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 4 }) togetherWith
                    (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 4 })
            },
            label = "countdown-mode"
        ) { state ->
            when (state) {
                0 -> {
                    val c = activeClass ?: return@AnimatedContent
                    val animProgress by animateFloatAsState(
                        c.progress, tween(900, easing = LinearEasing), label = "progress"
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
                                Box(
                                    Modifier.size(10.dp).scale(pulseScale).alpha(0.35f)
                                        .clip(CircleShape).background(accent)
                                )
                                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "HAPPENING NOW", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black, color = accent, letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
                                Canvas(Modifier.size(112.dp)) {
                                    val stroke = 11.dp.toPx()
                                    val inset = stroke / 2 + 4.dp.toPx()
                                    val arc = Size(size.width - inset * 2, size.height - inset * 2)
                                    drawCircle(accent.copy(alpha = glow * 0.18f), radius = size.minDimension / 2)
                                    drawArc(
                                        accent.copy(alpha = 0.18f), 0f, 360f, false,
                                        Offset(inset, inset), arc, style = Stroke(stroke)
                                    )
                                    drawArc(
                                        Brush.sweepGradient(listOf(accent.copy(alpha = 0.6f), accent)),
                                        -90f, 360f * animProgress, false,
                                        Offset(inset, inset), arc,
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AnimatedContent(
                                        targetState = fmt(c.remainingSeconds),
                                        transitionSpec = {
                                            (slideInVertically(tween(250)) { -it } + fadeIn(tween(250))) togetherWith
                                                (slideOutVertically(tween(250)) { it } + fadeOut(tween(150)))
                                        },
                                        label = "digits"
                                    ) { t ->
                                        Text(
                                            t, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        "left", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    title(c.subject.code, c.subject.name, c.slot.classType),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                )
                                val room = if (c.slot.room.isNotBlank()) "Room ${c.slot.room}" else ""
                                val teacher = c.teacher?.name ?: ""
                                val sub = listOf(room, teacher).filter { it.isNotBlank() }.joinToString(" • ")
                                if (sub.isNotBlank()) Text(
                                    sub, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    c.slot.getDisplayTimeSpan(), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${(animProgress * 100).toInt()}% complete",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = accent
                                )
                            }
                        }
                    }
                }

                1 -> {
                    val n = nextClass ?: return@AnimatedContent
                    val secs = Duration.between(now, n.slot.getStartLocalTime()).seconds
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp).scale(pulseScale.coerceAtMost(1.2f))
                                    .clip(CircleShape).background(accent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "UP NEXT AT ${n.slot.getDisplayStartTime()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black, color = accent, letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    title(n.subject.code, n.subject.name, n.slot.classType),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (n.slot.room.isNotBlank()) "Room ${n.slot.room}" else "Scheduled slot",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                AnimatedContent(
                                    targetState = fmt(secs),
                                    transitionSpec = {
                                        (slideInVertically(tween(250)) { -it } + fadeIn(tween(250))) togetherWith
                                            (slideOutVertically(tween(250)) { it } + fadeOut(tween(150)))
                                    },
                                    label = "next-digits"
                                ) { t ->
                                    Text(
                                        t, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = accent
                                    )
                                }
                                Text(
                                    "until start", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Fills up as the class approaches (last 60 minutes)
                        val approach by animateFloatAsState(
                            (1f - secs / 3600f).coerceIn(0f, 1f), tween(900), label = "approach"
                        )
                        Box(
                            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                .background(accent.copy(alpha = 0.18f))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(approach).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)).background(accent)
                            )
                        }
                    }
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎉", fontSize = 30.sp, modifier = Modifier.scale(pulseScale.coerceAtMost(1.2f)))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "NO MORE CLASSES", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black, color = accent, letterSpacing = 1.sp
                            )
                            Text(
                                "You're free for the rest of the day",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
