package com.wordcraft.game.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import com.wordcraft.game.data.model.CellData
import kotlin.math.min

class CrosswordGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gridMatrix: List<List<CellData?>> = emptyList()
    private var rows = 0
    private var cols = 0

    private var cellSize = 0f
    private var cellSpacing = dpToPx(5f)
    private var cornerRadius = dpToPx(8f)
    private var startX = 0f
    private var startY = 0f

    // Scale animation factor for newly revealed cells
    private var revealAnimScale = 1f

    // Paints
    private val emptyCellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }

    private val emptyCellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
    }

    private val revealedCellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val revealedCellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22000000")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val hintedCellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF3B0")
        style = Paint.Style.FILL
    }

    private val hintedCellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB703")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }

    private val hintDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB703")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D1B2A")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val cellRect = RectF()

    fun setGrid(matrix: List<List<CellData?>>) {
        gridMatrix = matrix
        rows = matrix.size
        cols = matrix.maxOfOrNull { it.size } ?: 0
        recalculateLayout()
        invalidate()
    }

    fun animateWordReveal() {
        ValueAnimator.ofFloat(0.7f, 1f).apply {
            duration = 350
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener {
                revealAnimScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateLayout()
    }

    private fun recalculateLayout() {
        if (rows == 0 || cols == 0 || width == 0 || height == 0) return

        val paddingH = dpToPx(16f)
        val paddingV = dpToPx(8f)
        val availableW = width - (paddingH * 2)
        val availableH = height - (paddingV * 2)

        val sizeByW = (availableW - (cellSpacing * (cols - 1))) / cols
        val sizeByH = (availableH - (cellSpacing * (rows - 1))) / rows
        val rawSize = min(sizeByW, sizeByH)

        cellSize = rawSize.coerceIn(dpToPx(32f), dpToPx(54f))
        textPaint.textSize = cellSize * 0.55f

        val totalGridW = (cellSize * cols) + (cellSpacing * (cols - 1))
        val totalGridH = (cellSize * rows) + (cellSpacing * (rows - 1))

        startX = (width - totalGridW) / 2f
        startY = (height - totalGridH) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rows == 0 || cols == 0) return

        val fontMetrics = textPaint.fontMetrics
        val textOffsetY = (fontMetrics.descent + fontMetrics.ascent) / 2f

        for (r in 0 until rows) {
            val rowCells = gridMatrix.getOrNull(r) ?: continue
            for (c in 0 until cols) {
                val cell = rowCells.getOrNull(c) ?: continue

                val left = startX + c * (cellSize + cellSpacing)
                val top = startY + r * (cellSize + cellSpacing)
                val right = left + cellSize
                val bottom = top + cellSize

                cellRect.set(left, top, right, bottom)

                if (cell.isRevealed) {
                    val bgPaint = if (cell.isHinted) hintedCellBgPaint else revealedCellBgPaint
                    val borderPaint = if (cell.isHinted) hintedCellBorderPaint else revealedCellBorderPaint

                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, bgPaint)
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, borderPaint)

                    // Draw letter
                    val cx = left + cellSize / 2f
                    val cy = top + cellSize / 2f
                    canvas.drawText(cell.letter.toString(), cx, cy - textOffsetY, textPaint)

                    // Draw hint dot
                    if (cell.isHinted) {
                        canvas.drawCircle(right - dpToPx(5f), top + dpToPx(5f), dpToPx(3f), hintDotPaint)
                    }
                } else {
                    // Empty cell placeholder
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, emptyCellBgPaint)
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, emptyCellBorderPaint)
                }
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
