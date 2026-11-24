package com.wordcraft.game.ui.components

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
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Particle(
    val initialX: Float,
    val initialY: Float,
    val size: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiCelebration(
    triggerTimestamp: Long,
    modifier: Modifier = Modifier
) {
    if (triggerTimestamp == 0L) return

    val progress = remember(triggerTimestamp) { Animatable(0f) }

    LaunchedEffect(triggerTimestamp) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
    }

    val colors = listOf(
        Color(0xFFFFD166),
        Color(0xFF06D6A0),
        Color(0xFF118AB2),
        Color(0xFFEF476F),
        Color(0xFFFF9F1C),
        Color(0xFF8338EC)
    )

    val particles = remember(triggerTimestamp) {
        val rand = Random(triggerTimestamp)
        List(60) {
            Particle(
                initialX = rand.nextFloat(),
                initialY = -0.1f - rand.nextFloat() * 0.2f,
                size = 12f + rand.nextFloat() * 14f,
                color = colors[rand.nextInt(colors.size)],
                speedY = 1.0f + rand.nextFloat() * 0.5f,
                speedX = (rand.nextFloat() - 0.5f) * 0.4f,
                rotationSpeed = (rand.nextFloat() - 0.5f) * 720f
            )
        }
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val t = progress.value

            for (p in particles) {
                val currentX = (p.initialX + p.speedX * t) * canvasW
                val currentY = (p.initialY + p.speedY * t) * canvasH
                val currentRotation = p.rotationSpeed * t

                rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - t * 0.8f).coerceIn(0f, 1f)),
                        topLeft = Offset(currentX - p.size / 2, currentY - p.size / 2),
                        size = Size(p.size, p.size * 0.7f)
                    )
                }
            }
        }
    }
}
