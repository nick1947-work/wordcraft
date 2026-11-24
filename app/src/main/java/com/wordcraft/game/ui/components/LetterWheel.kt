package com.wordcraft.game.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordcraft.game.ui.theme.GoldenYellow
import com.wordcraft.game.ui.theme.LetterBubbleDefaultBg
import com.wordcraft.game.ui.theme.LetterBubbleDefaultText
import com.wordcraft.game.ui.theme.LetterBubbleSelectedBg
import com.wordcraft.game.ui.theme.LetterBubbleSelectedText
import com.wordcraft.game.ui.theme.LineColor
import com.wordcraft.game.ui.theme.LineGlowColor
import com.wordcraft.game.ui.theme.WheelPlateBg
import com.wordcraft.game.ui.theme.WheelPlateBorder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LetterWheel(
    letters: List<String>,
    selectedIndices: List<Int>,
    touchPosition: Offset?,
    shuffleAnimKey: Int,
    onLetterTouchDown: (index: Int, pos: Offset) -> Unit,
    onLetterTouchMove: (pos: Offset, hoveredIndex: Int?) -> Unit,
    onLetterTouchUp: () -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 270.dp
) {
    val density = LocalDensity.current
    val wheelSizePx = with(density) { wheelSize.toPx() }
    val center = Offset(wheelSizePx / 2f, wheelSizePx / 2f)
    val wheelRadius = wheelSizePx * 0.38f
    val letterBubbleRadius = with(density) { 30.dp.toPx() }
    val hitRadius = with(density) { 42.dp.toPx() }

    // Shuffle rotation animation
    var targetRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(shuffleAnimKey) {
        if (shuffleAnimKey > 0) {
            targetRotation += 360f
        }
    }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "wheelRotation"
    )

    // Calculate letter node center positions
    val letterPositions = remember(letters.size, wheelRadius, center) {
        val count = letters.size.coerceAtLeast(1)
        List(count) { i ->
            val angle = -PI / 2.0 + (i * 2.0 * PI / count)
            Offset(
                x = center.x + (wheelRadius * cos(angle)).toFloat(),
                y = center.y + (wheelRadius * sin(angle)).toFloat()
            )
        }
    }

    Box(
        modifier = modifier
            .size(wheelSize)
            .pointerInput(letters, letterPositions) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val hitIndex = findClosestLetter(startOffset, letterPositions, hitRadius)
                        if (hitIndex != null) {
                            onLetterTouchDown(hitIndex, startOffset)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentPos = change.position
                        val hoveredIndex = findClosestLetter(currentPos, letterPositions, hitRadius)
                        onLetterTouchMove(currentPos, hoveredIndex)
                    },
                    onDragEnd = {
                        onLetterTouchUp()
                    },
                    onDragCancel = {
                        onLetterTouchUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedRotation)
        ) {
            // 1. Draw outer wheel background plate
            drawCircle(
                color = WheelPlateBg,
                radius = wheelRadius + letterBubbleRadius + 14.dp.toPx(),
                center = center
            )
            drawCircle(
                color = WheelPlateBorder,
                radius = wheelRadius + letterBubbleRadius + 14.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0x15FFFFFF),
                radius = wheelRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Draw connecting lines between selected letters
            if (selectedIndices.isNotEmpty()) {
                val linePath = Path()
                val firstCenter = letterPositions[selectedIndices.first()]
                linePath.moveTo(firstCenter.x, firstCenter.y)

                for (idx in 1 until selectedIndices.size) {
                    val nextCenter = letterPositions[selectedIndices[idx]]
                    linePath.lineTo(nextCenter.x, nextCenter.y)
                }

                // If currently dragging, connect to finger touch position
                touchPosition?.let { fingerPos ->
                    linePath.lineTo(fingerPos.x, fingerPos.y)
                }

                // Glow line (outer)
                drawPath(
                    path = linePath,
                    color = LineGlowColor,
                    style = Stroke(
                        width = 16.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Core line (inner vibrant)
                drawPath(
                    path = linePath,
                    brush = Brush.linearGradient(
                        colors = listOf(GoldenYellow, Color(0xFFFFA000))
                    ),
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw Letter Bubbles
            val textPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 24.dp.toPx()
            }

            for (i in letters.indices) {
                val pos = letterPositions.getOrNull(i) ?: continue
                val letter = letters[i]
                val isSelected = selectedIndices.contains(i)

                val radius = if (isSelected) letterBubbleRadius * 1.15f else letterBubbleRadius

                // Outer shadow
                drawCircle(
                    color = Color(0x33000000),
                    radius = radius + 3.dp.toPx(),
                    center = Offset(pos.x, pos.y + 3.dp.toPx())
                )

                // Bubble fill
                if (isSelected) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F), LetterBubbleSelectedBg),
                            center = pos,
                            radius = radius
                        ),
                        radius = radius,
                        center = pos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = pos,
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = LetterBubbleDefaultBg,
                        radius = radius,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0x33000000),
                        radius = radius,
                        center = pos,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw letter text
                textPaint.color = if (isSelected) {
                    LetterBubbleSelectedText.toArgb()
                } else {
                    LetterBubbleDefaultText.toArgb()
                }

                // Vertical centering font metric
                val fontMetrics = textPaint.fontMetrics
                val textCenterY = pos.y - (fontMetrics.descent + fontMetrics.ascent) / 2f

                drawContext.canvas.nativeCanvas.drawText(
                    letter,
                    pos.x,
                    textCenterY,
                    textPaint
                )
            }
        }
    }
}

private fun findClosestLetter(
    touchPos: Offset,
    positions: List<Offset>,
    hitRadius: Float
): Int? {
    var closestIdx: Int? = null
    var minDistance = Float.MAX_VALUE

    for (i in positions.indices) {
        val pos = positions[i]
        val distance = hypot(touchPos.x - pos.x, touchPos.y - pos.y)
        if (distance <= hitRadius && distance < minDistance) {
            minDistance = distance
            closestIdx = i
        }
    }
    return closestIdx
}
