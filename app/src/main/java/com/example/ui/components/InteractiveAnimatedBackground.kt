package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.AppPalette
import com.example.ui.theme.AttendThemeState
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.getAuroraGradient
import com.example.ui.theme.getForestGradient
import com.example.ui.theme.getMonoGradient
import com.example.ui.theme.getNeonGradient
import com.example.ui.theme.getOceanGradient
import com.example.ui.theme.getSunsetGradient
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Ambient glowing orb definition
private data class LiquidOrb(
    val xBaseRatio: Float,
    val yBaseRatio: Float,
    val radiusRatio: Float,
    val xPeriod: Float,
    val yPeriod: Float,
    val phase: Float,
    val colorIndex: Int // 0: primary, 1: secondary, 2: tertiary, 3: accent
)

// Floating particle definition (twinkling star/dust/bokeh)
private data class StardustParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val size: Float,
    val speedY: Float,
    val driftSpeedX: Float,
    val phase: Float,
    val alphaMultiplier: Float,
    val colorType: Int
)

// Dynamic interactive ripple spawned by tapping or dragging
private data class RippleWave(
    val id: Long,
    val x: Float,
    val y: Float,
    val birthTime: Long,
    val maxRadius: Float,
    val color: Color
)

// Burst sparkle particle emitted from touch location
private data class TouchSpark(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val birthTime: Long,
    val color: Color,
    val size: Float
)

/**
 * Super dynamic, hypnotic, addictive animated background:
 * - Fluid multi-layered chromatic nebula mesh with drifting liquid gradient orbs.
 * - Shimmering floating stardust particles moving with organic sine physics.
 * - Dynamic interactive touch ripples with glowing concentric neon rings.
 * - Reactive touch sparkles bursting on tap and drag gestures.
 * - High-efficiency GPU-accelerated Compose Canvas implementation ensuring 60/120 FPS.
 */
@Composable
fun InteractiveAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalAppPalette.current
    val animationsEnabled = AttendThemeState.animationsEnabled

    val baseGradient = when (palette) {
        AppPalette.AURORA -> getAuroraGradient(isDark)
        AppPalette.SUNSET -> getSunsetGradient(isDark)
        AppPalette.OCEAN -> getOceanGradient(isDark)
        AppPalette.NEON -> getNeonGradient(isDark)
        AppPalette.FOREST -> getForestGradient(isDark)
        AppPalette.MONO -> getMonoGradient(isDark)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Palette-informed accent highlights
    val highlightColor = when (palette) {
        AppPalette.AURORA -> Color(0xFFC084FC) // Electric Violet
        AppPalette.SUNSET -> Color(0xFFF43F5E) // Radiant Rose
        AppPalette.OCEAN -> Color(0xFF38BDF8) // Aqua Cyan
        AppPalette.NEON -> Color(0xFF22C55E) // Neon Emerald
        AppPalette.FOREST -> Color(0xFF86EFAC) // Mint Glow
        AppPalette.MONO -> Color(0xFFE2E8F0) // Platinum
    }

    val colorPalette = remember(primaryColor, secondaryColor, tertiaryColor, highlightColor) {
        listOf(primaryColor, secondaryColor, tertiaryColor, highlightColor)
    }

    // Seamless continuous infinite animation clocks (low CPU overhead)
    val infiniteTransition = rememberInfiniteTransition(label = "super_ambient_motion")

    val tClock by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831853f * 4f, // 0 to 8*pi
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time_orbit"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val waveClock by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831853f * 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_clock"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    // Pre-allocated fluid ambient orbs with organic motion parameters
    val liquidOrbs = remember {
        listOf(
            LiquidOrb(xBaseRatio = 0.20f, yBaseRatio = 0.18f, radiusRatio = 0.65f, xPeriod = 1.0f, yPeriod = 0.8f, phase = 0.0f, colorIndex = 0),
            LiquidOrb(xBaseRatio = 0.85f, yBaseRatio = 0.35f, radiusRatio = 0.70f, xPeriod = 0.8f, yPeriod = 1.1f, phase = 1.7f, colorIndex = 1),
            LiquidOrb(xBaseRatio = 0.30f, yBaseRatio = 0.68f, radiusRatio = 0.58f, xPeriod = 1.2f, yPeriod = 0.9f, phase = 3.4f, colorIndex = 2),
            LiquidOrb(xBaseRatio = 0.80f, yBaseRatio = 0.88f, radiusRatio = 0.62f, xPeriod = 0.9f, yPeriod = 1.3f, phase = 4.9f, colorIndex = 3),
            LiquidOrb(xBaseRatio = 0.50f, yBaseRatio = 0.48f, radiusRatio = 0.45f, xPeriod = 1.4f, yPeriod = 1.4f, phase = 2.5f, colorIndex = 0)
        )
    }

    // Pre-generated stardust constellation particles (28 particles for visual sparkle without frame drops)
    val stardustParticles = remember {
        val rand = Random(42)
        List(28) { i ->
            StardustParticle(
                initialXRatio = rand.nextFloat(),
                initialYRatio = rand.nextFloat(),
                size = 1.8f + rand.nextFloat() * 2.8f,
                speedY = 0.018f + rand.nextFloat() * 0.024f,
                driftSpeedX = 0.4f + rand.nextFloat() * 0.8f,
                phase = rand.nextFloat() * 6.28f,
                alphaMultiplier = 0.5f + rand.nextFloat() * 0.5f,
                colorType = i % 4
            )
        }
    }

    // Interactive ripples and touch sparks
    val rippleList = remember { mutableStateListOf<RippleWave>() }
    val sparkList = remember { mutableStateListOf<TouchSpark>() }

    var nextId by remember { mutableStateOf(0L) }

    fun addTouchBurst(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val burstColor = colorPalette[((x.toInt() xor y.toInt()) and 3).coerceIn(0, 3)]

        // 1. Expanding ripple wave
        rippleList.add(
            RippleWave(
                id = nextId++,
                x = x,
                y = y,
                birthTime = now,
                maxRadius = 240f,
                color = burstColor
            )
        )
        if (rippleList.size > 8) {
            rippleList.removeAt(0)
        }

        // 2. Add constellation sparkles shooting outward
        val rand = Random(now)
        for (i in 0 until 5) {
            val angle = rand.nextFloat() * 6.2831853f
            val speed = 25f + rand.nextFloat() * 55f
            sparkList.add(
                TouchSpark(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    birthTime = now,
                    color = if (i % 2 == 0) highlightColor else burstColor,
                    size = 2.5f + rand.nextFloat() * 2.5f
                )
            )
        }
        while (sparkList.size > 25) {
            sparkList.removeAt(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        addTouchBurst(offset.x, offset.y)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        addTouchBurst(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        // Throttle drag sparkles
                        if (Random.nextFloat() < 0.35f) {
                            addTouchBurst(change.position.x, change.position.y)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw base high-vibrancy chromatic gradient
            drawRect(brush = baseGradient)

            // 2. Draw Multi-Layer Liquid Glowing Nebula Orbs
            val orbAlphaBase = if (isDark) 0.28f else 0.18f
            liquidOrbs.forEachIndexed { idx, orb ->
                val chosenColor = colorPalette[orb.colorIndex]

                val orbX = width * orb.xBaseRatio +
                        cos(tClock * orb.xPeriod + orb.phase) * (width * 0.10f) +
                        sin(tClock * 0.5f + orb.phase) * 20f

                val orbY = height * orb.yBaseRatio +
                        sin(tClock * orb.yPeriod + orb.phase) * (height * 0.08f) +
                        cos(tClock * 0.6f + orb.phase) * 25f

                val pulse = 1f + 0.14f * sin(waveClock + orb.phase)
                val orbRadius = width * orb.radiusRatio * pulse

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            chosenColor.copy(alpha = orbAlphaBase * (if (idx == 0 || idx == 3) 1.25f else 1.0f)),
                            chosenColor.copy(alpha = orbAlphaBase * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(orbX, orbY),
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = Offset(orbX, orbY)
                )
            }

            // 3. Subtle chromatic horizon wave glow
            val waveY = height * (0.55f + 0.06f * sin(waveClock * 0.7f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = if (isDark) 0.15f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, waveY),
                    radius = width * 0.85f
                ),
                radius = width * 0.85f,
                center = Offset(width * 0.5f, waveY)
            )

            // 4. Draw Floating Constellation & Twinkling Stardust Particles
            val particleAlphaBase = if (isDark) 0.45f else 0.28f
            stardustParticles.forEachIndexed { i, p ->
                val particleColor = colorPalette[p.colorType]

                // Upward floating motion with modulo wrap
                val currentYRatio = (p.initialYRatio - (tClock * p.speedY)) % 1f
                val py = (if (currentYRatio < 0f) currentYRatio + 1f else currentYRatio) * height

                // Gentle horizontal harmonic sway
                val sway = sin(tClock * p.driftSpeedX + p.phase) * 35f
                val px = ((p.initialXRatio * width) + sway + width) % width

                // Twinkle pulse effect
                val twinkle = 0.4f + 0.6f * sin(tClock * 2.5f + p.phase)
                val alpha = (particleAlphaBase * p.alphaMultiplier * twinkle).coerceIn(0f, 1f)
                val curRadius = p.size * (0.8f + 0.4f * twinkle)

                // Particle outer glow halo
                drawCircle(
                    color = particleColor.copy(alpha = alpha * 0.35f),
                    radius = curRadius * 2.2f,
                    center = Offset(px, py)
                )

                // Particle solid bright core
                drawCircle(
                    color = if (isDark) Color.White.copy(alpha = alpha * 0.9f) else particleColor.copy(alpha = alpha),
                    radius = curRadius,
                    center = Offset(px, py)
                )

                // Connect nearby particles with subtle ethereal constellation lines
                if (i % 3 == 0 && i + 1 < stardustParticles.size) {
                    val nextP = stardustParticles[i + 1]
                    val nextYRatio = (nextP.initialYRatio - (tClock * nextP.speedY)) % 1f
                    val nextPy = (if (nextYRatio < 0f) nextYRatio + 1f else nextYRatio) * height
                    val nextSway = sin(tClock * nextP.driftSpeedX + nextP.phase) * 35f
                    val nextPx = ((nextP.initialXRatio * width) + nextSway + width) % width

                    val dx = px - nextPx
                    val dy = py - nextPy
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 120f) {
                        val lineAlpha = ((1f - (dist / 120f)) * (if (isDark) 0.20f else 0.12f) * twinkle).coerceIn(0f, 1f)
                        drawLine(
                            color = primaryColor.copy(alpha = lineAlpha),
                            start = Offset(px, py),
                            end = Offset(nextPx, nextPy),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }

            // 5. Draw Interactive Responsive Ripple Waves
            val nowTime = System.currentTimeMillis()
            val iterator = rippleList.iterator()
            while (iterator.hasNext()) {
                val wave = iterator.next()
                val age = (nowTime - wave.birthTime).toFloat()
                val duration = 850f
                if (age > duration) {
                    // Naturally expires
                } else {
                    val progress = age / duration
                    val currentRadius = 15f + (wave.maxRadius - 15f) * progress
                    val waveAlpha = (1f - progress) * (if (isDark) 0.42f else 0.28f)

                    // Outer expanding glow ring
                    drawCircle(
                        color = wave.color.copy(alpha = waveAlpha),
                        radius = currentRadius,
                        center = Offset(wave.x, wave.y),
                        style = Stroke(width = 3.5f * (1f - progress * 0.6f))
                    )

                    // Secondary inner echoing harmonic ring
                    if (progress > 0.15f) {
                        val innerProgress = (progress - 0.15f) / 0.85f
                        val innerRadius = 10f + (wave.maxRadius * 0.65f) * innerProgress
                        val innerAlpha = (1f - innerProgress) * (if (isDark) 0.30f else 0.20f)
                        drawCircle(
                            color = highlightColor.copy(alpha = innerAlpha),
                            radius = innerRadius,
                            center = Offset(wave.x, wave.y),
                            style = Stroke(width = 2.0f * (1f - innerProgress))
                        )
                    }
                }
            }

            // 6. Draw Burst Sparkles
            val sparkIter = sparkList.iterator()
            while (sparkIter.hasNext()) {
                val spark = sparkIter.next()
                val age = (nowTime - spark.birthTime).toFloat()
                val sparkLife = 700f
                if (age < sparkLife) {
                    val progress = age / sparkLife
                    val sx = spark.x + spark.vx * progress
                    val sy = spark.y + spark.vy * progress + (40f * progress * progress) // gentle gravity
                    val alpha = (1f - progress) * (if (isDark) 0.85f else 0.60f)

                    drawCircle(
                        color = spark.color.copy(alpha = alpha),
                        radius = spark.size * (1f - progress * 0.5f),
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Host Screen content cleanly layered on top with smooth glass aesthetics
        content()
    }
}
