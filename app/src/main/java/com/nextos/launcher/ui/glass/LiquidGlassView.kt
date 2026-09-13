package com.nextos.launcher.ui.glass

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.nextos.launcher.R

/**
 * Liquid glass: rounded clip, gaussian-ish blur of children, specular highlight.
 */
class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.glass_highlight)
    }
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radiusPx = 32f * resources.displayMetrics.density

    init {
        setWillNotDraw(false)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), radiusPx)
            }
        }
        // Do not RenderEffect-blur this view: that would blur icons inside.
        // Glass is the translucent fill + specular stroke painted in dispatchDraw.
    }

    fun setCornerRadiusDp(dp: Float) {
        radiusPx = dp * resources.displayMetrics.density
        invalidateOutline()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sheen.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x55FFFFFF, 0x00FFFFFF, 0x22FFFFFF),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val inset = stroke.strokeWidth / 2f
        canvas.drawRoundRect(
            inset, inset, width - inset, height - inset, radiusPx, radiusPx, sheen
        )
        canvas.drawRoundRect(
            inset, inset, width - inset, height - inset, radiusPx, radiusPx, stroke
        )
    }
}
