package com.nextos.launcher.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import kotlin.math.max

object BlurHelper {

    fun applyBackdrop(view: View, radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        } else {
            view.alpha = 0.92f
        }
    }

    fun clear(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }

    /** Cheap box blur for wallpaper crops (lock/dock glass). */
    fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val r = max(1, radius)
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = pixels.copyOf()
        horizontal(pixels, out, w, h, r)
        vertical(out, pixels, w, h, r)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    fun saturate(src: Bitmap, amount: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val cm = ColorMatrix()
        cm.setSaturation(amount)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.colorFilter = ColorMatrixColorFilter(cm)
        c.drawBitmap(src, 0f, 0f, p)
        return out
    }

    private fun horizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = r * 2 + 1
        for (y in 0 until h) {
            var rs = 0
            var gs = 0
            var bs = 0
            var as = 0
            val row = y * w
            for (i in -r until w) {
                val add = src[row + i.coerceIn(0, w - 1)]
                rs += add shr 16 and 0xFF
                gs += add shr 8 and 0xFF
                bs += add and 0xFF
                as += add ushr 24
                if (i - r >= 0) {
                    val rem = src[row + (i - div).coerceIn(0, w - 1)]
                    rs -= rem shr 16 and 0xFF
                    gs -= rem shr 8 and 0xFF
                    bs -= rem and 0xFF
                    as -= rem ushr 24
                }
                if (i >= 0) {
                    dst[row + i] = (as / div shl 24) or (rs / div shl 16) or (gs / div shl 8) or (bs / div)
                }
            }
        }
    }

    private fun vertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = r * 2 + 1
        for (x in 0 until w) {
            var rs = 0
            var gs = 0
            var bs = 0
            var as = 0
            for (i in -r until h) {
                val add = src[i.coerceIn(0, h - 1) * w + x]
                rs += add shr 16 and 0xFF
                gs += add shr 8 and 0xFF
                bs += add and 0xFF
                as += add ushr 24
                if (i - r >= 0) {
                    val rem = src[(i - div).coerceIn(0, h - 1) * w + x]
                    rs -= rem shr 16 and 0xFF
                    gs -= rem shr 8 and 0xFF
                    bs -= rem and 0xFF
                    as -= rem ushr 24
                }
                if (i >= 0) {
                    dst[i * w + x] = (as / div shl 24) or (rs / div shl 16) or (gs / div shl 8) or (bs / div)
                }
            }
        }
    }
}
