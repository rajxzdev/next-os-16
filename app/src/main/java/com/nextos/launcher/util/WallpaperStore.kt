package com.nextos.launcher.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.nextos.launcher.R
import com.nextos.launcher.data.LauncherPreferences
import java.io.File
import java.io.FileOutputStream

object WallpaperStore {

    fun file(context: Context): File = File(context.filesDir, "next_wallpaper.jpg")

    fun drawableRes(id: Int): Int = when (id) {
        LauncherPreferences.WALL_DUSK -> R.drawable.wallpaper_dusk
        LauncherPreferences.WALL_SAND -> R.drawable.wallpaper_sand
        else -> R.drawable.wallpaper_aurora
    }

    fun applyTo(view: ImageView, prefs: LauncherPreferences) {
        val custom = prefs.wallpaperPath()?.let { BitmapFactory.decodeFile(it) }
        if (prefs.wallpaperId() == LauncherPreferences.WALL_CUSTOM && custom != null) {
            view.setImageBitmap(custom)
        } else {
            view.setImageResource(drawableRes(prefs.wallpaperId()))
        }
    }

    fun currentBitmap(context: Context, prefs: LauncherPreferences): Bitmap? {
        if (prefs.wallpaperId() == LauncherPreferences.WALL_CUSTOM) {
            val p = prefs.wallpaperPath()
            if (p != null) return BitmapFactory.decodeFile(p)
        }
        val d = ContextCompat.getDrawable(context, drawableRes(prefs.wallpaperId())) ?: return null
        return drawableBitmap(d, 720, 1280)
    }

    fun saveCustom(context: Context, bitmap: Bitmap): String {
        val f = file(context)
        FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return f.absolutePath
    }

    fun pushSystem(context: Context, bitmap: Bitmap) {
        try {
            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bitmap)
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            }
        } catch (_: Exception) {
        }
    }

    fun drawableBitmap(d: Drawable, w: Int, h: Int): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) {
            return Bitmap.createScaledBitmap(d.bitmap, w, h, true)
        }
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        return b
    }
}
