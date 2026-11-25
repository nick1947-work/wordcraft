package com.wordcraft.game.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class LetterWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnWordGestureListener {
        fun onLetterSelected(index: Int, letter: String)
        fun onWordUpdated(currentWord: String)
        fun onWordCompleted(word: String)
    }

    var listener: OnWordGestureListener? = null

    private var letters: List<String> = emptyList()
    private val selectedIndices = mutableListOf<Int>()
    private var touchPoint: PointF? = null

    // Geometry
    private val letterPositions = mutableListOf<PointF>()
    private var wheelRadius = 0f
    private var letterBubbleRadius = 0f
    private var hitRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // Paints
    private val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    private val plateBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }

    private val lineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFD166")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(14f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val lineCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB703")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(7f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val bubbleDefaultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val bubbleDefaultBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val bubbleSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB703")
        style = Paint.Style.FILL
    }

    private val bubbleSelectedBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = dpToPx(22f)
    }

    private val path = Path()

    fun setLetters(newLetters: List<String>) {
        letters = newLetters
        selectedIndices.clear()
        touchPoint = null
        recalculateLetterPositions()
        invalidate()
    }

    fun animateShuffle() {
        animate()
            .rotationBy(360f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val size = min(w, h)
        wheelRadius = size * 0.36f
        letterBubbleRadius = dpToPx(26f)
        hitRadius = dpToPx(38f)
        recalculateLetterPositions()
    }

    private fun recalculateLetterPositions() {
        letterPositions.clear()
        if (letters.isEmpty()) return

        val count = letters.size
        for (i in 0 until count) {
            val angle = -PI / 2.0 + (i * 2.0 * PI / count)
            val x = centerX + (wheelRadius * cos(angle)).toFloat()
            val y = centerY + (wheelRadius * sin(angle)).toFloat()
            letterPositions.add(PointF(x, y))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw outer wheel background plate
        val plateRadius = wheelRadius + letterBubbleRadius + dpToPx(12f)
        canvas.drawCircle(centerX, centerY, plateRadius, platePaint)
        canvas.drawCircle(centerX, centerY, plateRadius, plateBorderPaint)

        // 2. Draw connecting lines between selected letters
        if (selectedIndices.isNotEmpty()) {
            path.reset()
            val first = letterPositions[selectedIndices[0]]
            path.moveTo(first.x, first.y)

            for (i in 1 until selectedIndices.size) {
                val next = letterPositions[selectedIndices[i]]
                path.lineTo(next.x, next.y)
            }

            // Draw line to current touch point if dragging
            touchPoint?.let { pt ->
                path.lineTo(pt.x, pt.y)
            }

            canvas.drawPath(path, lineGlowPaint)
            canvas.drawPath(path, lineCorePaint)
        }

        // 3. Draw letter bubbles
        val fontMetrics = textPaint.fontMetrics
        val textOffsetY = (fontMetrics.descent + fontMetrics.ascent) / 2f

        for (i in letters.indices) {
            val pos = letterPositions.getOrNull(i) ?: continue
            val letter = letters[i]
            val isSelected = selectedIndices.contains(i)

            val radius = if (isSelected) letterBubbleRadius * 1.15f else letterBubbleRadius

            // Soft shadow
            canvas.drawCircle(pos.x, pos.y + dpToPx(2f), radius + dpToPx(2f), platePaint)

            // Bubble background & border
            if (isSelected) {
                canvas.drawCircle(pos.x, pos.y, radius, bubbleSelectedPaint)
                canvas.drawCircle(pos.x, pos.y, radius, bubbleSelectedBorderPaint)
                textPaint.color = Color.parseColor("#0D1B2A")
            } else {
                canvas.drawCircle(pos.x, pos.y, radius, bubbleDefaultPaint)
                canvas.drawCircle(pos.x, pos.y, radius, bubbleDefaultBorderPaint)
                textPaint.color = Color.parseColor("#1B263B")
            }

            // Draw text
            canvas.drawText(letter, pos.x, pos.y - textOffsetY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hitIdx = findClosestLetter(event.x, event.y)
                if (hitIdx != null) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    selectedIndices.clear()
                    selectedIndices.add(hitIdx)
                    touchPoint = PointF(event.x, event.y)
                    listener?.onLetterSelected(hitIdx, letters[hitIdx])
                    listener?.onWordUpdated(letters[hitIdx])
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedIndices.isNotEmpty()) {
                    touchPoint = PointF(event.x, event.y)
                    val hitIdx = findClosestLetter(event.x, event.y)

                    if (hitIdx != null) {
                        // Backtracking: if dragged back to second-to-last letter, undo last
                        if (selectedIndices.size >= 2 && hitIdx == selectedIndices[selectedIndices.size - 2]) {
                            selectedIndices.removeAt(selectedIndices.size - 1)
                            val currentWord = selectedIndices.map { letters[it] }.joinToString("")
                            listener?.onWordUpdated(currentWord)
                        } else if (!selectedIndices.contains(hitIdx)) {
                            // Add new letter
                            selectedIndices.add(hitIdx)
                            listener?.onLetterSelected(hitIdx, letters[hitIdx])
                            val currentWord = selectedIndices.map { letters[it] }.joinToString("")
                            listener?.onWordUpdated(currentWord)
                        }
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (selectedIndices.isNotEmpty()) {
                    val finalWord = selectedIndices.map { letters[it] }.joinToString("")
                    listener?.onWordCompleted(finalWord)
                    selectedIndices.clear()
                    touchPoint = null
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findClosestLetter(x: Float, y: Float): Int? {
        var closestIdx: Int? = null
        var minDistance = Float.MAX_VALUE

        for (i in letterPositions.indices) {
            val pos = letterPositions[i]
            val distance = hypot(x - pos.x, y - pos.y)
            if (distance <= hitRadius && distance < minDistance) {
                minDistance = distance
                closestIdx = i
            }
        }
        return closestIdx
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
