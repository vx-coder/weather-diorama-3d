package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.WeatherCondition
import kotlin.math.sin
import kotlin.random.Random

// Flat arrays to eliminate Object boxing overhead in hot loops
class ParticleArray(size: Int) {
    val x = FloatArray(size)
    val y = FloatArray(size)
    val factor = FloatArray(size)
    val count = size
}

@Composable
fun AtmosphericParticles(
    condition: WeatherCondition,
    windSpeed: Double,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "particles_engine")

    val rainProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_prog"
    )

    val snowProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_prog"
    )

    val sunPulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )

    val thunderFlash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thunder_flash"
    )

    // Pre-allocated flat float arrays
    val rainParticles = remember {
        ParticleArray(35).apply {
            for (i in 0 until count) {
                x[i] = Random.nextFloat()
                y[i] = Random.nextFloat()
                factor[i] = Random.nextFloat() * 0.5f + 0.5f
            }
        }
    }

    val snowParticles = remember {
        ParticleArray(30).apply {
            for (i in 0 until count) {
                x[i] = Random.nextFloat()
                y[i] = Random.nextFloat()
                factor[i] = Random.nextFloat() * 2f + 1.5f
            }
        }
    }

    val dustMotes = remember {
        ParticleArray(18).apply {
            for (i in 0 until count) {
                x[i] = Random.nextFloat()
                y[i] = Random.nextFloat()
                factor[i] = Random.nextFloat() * 2f + 1f
            }
        }
    }

    val fogColors = remember {
        listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.15f),
            Color(0xFFCBD5E1).copy(alpha = 0.45f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when {
            condition.isThunder -> {
                if (thunderFlash in 0.82f..0.88f || thunderFlash in 0.93f..0.96f) {
                    drawRect(Color.White.copy(alpha = 0.35f))
                }
                drawRain(rainParticles, rainProgress, w, h, windSpeed, isHeavy = true)
            }
            condition.isRainy -> {
                drawRain(rainParticles, rainProgress, w, h, windSpeed, isHeavy = false)
            }
            condition.isSnowy -> {
                drawSnow(snowParticles, snowProgress, w, h)
            }
            condition.isFoggy -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = fogColors,
                        startY = h * 0.4f,
                        endY = h
                    )
                )
            }
            condition.isClear -> {
                drawSunRaysAndMotes(dustMotes, sunPulse, w, h)
            }
            else -> {
                drawDustMotes(dustMotes, sunPulse, w, h)
            }
        }
    }
}

private fun DrawScope.drawRain(
    particles: ParticleArray,
    progress: Float,
    w: Float,
    h: Float,
    windSpeed: Double,
    isHeavy: Boolean
) {
    val slant = (windSpeed.toFloat() * 0.8f).coerceIn(-15f, 25f)
    val dropLength = if (isHeavy) 26f else 18f
    val strokeW = if (isHeavy) 1.8f else 1.2f
    val dropColor = Color(0xFF38BDF8).copy(alpha = 0.7f)

    for (i in 0 until particles.count) {
        val rx = particles.x[i]
        val ry = particles.y[i]
        val speedFactor = particles.factor[i]

        val curY = ((ry + progress * speedFactor * 1.5f) % 1f) * h
        val curX = (rx * w + (curY / h) * slant) % w

        drawLine(
            color = dropColor,
            start = Offset(curX, curY),
            end = Offset(curX + slant * 0.4f, curY + dropLength),
            strokeWidth = strokeW
        )

        if (curY > h * 0.78f) {
            val splashProgress = (curY - h * 0.78f) / (h * 0.22f)
            drawCircle(
                color = dropColor.copy(alpha = (1f - splashProgress) * 0.5f),
                radius = splashProgress * 8f,
                center = Offset(curX, h * 0.88f),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawSnow(
    particles: ParticleArray,
    progress: Float,
    w: Float,
    h: Float
) {
    val flakeColor = Color.White.copy(alpha = 0.85f)
    for (i in 0 until particles.count) {
        val rx = particles.x[i]
        val ry = particles.y[i]
        val radius = particles.factor[i]

        val curY = ((ry + progress * 0.6f * (radius / 2.5f)) % 1f) * h
        val sway = sin((progress * 6.28318f + i).toDouble()).toFloat() * 12f
        val curX = (rx * w + sway) % w

        drawCircle(
            color = flakeColor,
            radius = radius,
            center = Offset(curX, curY)
        )
    }
}

private fun DrawScope.drawSunRaysAndMotes(
    motes: ParticleArray,
    pulse: Float,
    w: Float,
    h: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.25f * pulse), Color.Transparent),
            center = Offset(w * 0.8f, h * 0.2f),
            radius = w * 0.6f
        ),
        radius = w * 0.6f,
        center = Offset(w * 0.8f, h * 0.2f)
    )
    drawDustMotes(motes, pulse, w, h)
}

private fun DrawScope.drawDustMotes(
    motes: ParticleArray,
    pulse: Float,
    w: Float,
    h: Float
) {
    for (i in 0 until motes.count) {
        val rx = motes.x[i]
        val ry = motes.y[i]
        val r = motes.factor[i]
        val alpha = (0.3f + 0.5f * sin((pulse * 3.14159f + i).toDouble()).toFloat()).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFFEF9C3).copy(alpha = alpha),
            radius = r,
            center = Offset(w * rx, h * ry)
        )
    }
}
