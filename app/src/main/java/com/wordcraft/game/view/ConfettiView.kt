package com.wordcraft.game.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        val initialX: Float,
        val initialY: Float,
        val size: Float,
        val color: Int,
        val speedY: Float,
        val speedX: Float,
        val rotationSpeed: Float
    )

    private val colors = intArrayOf(
        Color.parseColor("#FFD166"),
        Color.parseColor("#06D6A0"),
        Color.parseColor("#118AB2"),
        Color.parseColor("#EF476F"),
        Color.parseColor("#FF9F1C"),
        Color.parseColor("#8338EC")
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 1f
    private var animator: ValueAnimator? = null

    fun startCelebration() {
        val rand = Random(System.currentTimeMillis())
        particles.clear()
        for (i in 0 until 50) {
            particles.add(
                Particle(
                    initialX = rand.nextFloat(),
                    initialY = -0.1f - rand.nextFloat() * 0.2f,
                    size = dpToPx(8f + rand.nextFloat() * 8f),
                    color = colors[rand.nextInt(colors.size)],
                    speedY = 1.0f + rand.nextFloat() * 0.5f,
                    speedX = (rand.nextFloat() - 0.5f) * 0.35f,
                    rotationSpeed = (rand.nextFloat() - 0.5f) * 720f
                )
            )
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress >= 1f || particles.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val alpha = ((1f - progress * 0.8f).coerceIn(0f, 1f) * 255).toInt()

        for (p in particles) {
            val cx = (p.initialX + p.speedX * progress) * w
            val cy = (p.initialY + p.speedY * progress) * h
            val rot = p.rotationSpeed * progress

            paint.color = p.color
            paint.alpha = alpha

            canvas.save()
            canvas.rotate(rot, cx, cy)
            canvas.drawRect(
                cx - p.size / 2f,
                cy - p.size / 2f,
                cx + p.size / 2f,
                cy + p.size / 2f,
                paint
            )
            canvas.restore()
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
