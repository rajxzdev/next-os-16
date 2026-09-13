package com.nextos.launcher.ui.lock

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.nextos.launcher.R
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.databinding.ActivityLockBinding
import com.nextos.launcher.ui.island.NextIslandBus
import com.nextos.launcher.util.WallpaperStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val store by lazy { AppLockStore(this) }
    private val prefs by lazy { LauncherPreferences(this) }
    private val buffer = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            bindClock()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WallpaperStore.applyTo(binding.lockWallpaper, prefs)
        bindClock()
        buildPad()
        if (prefs.lockIntegrated() && store.hasPin()) {
            binding.pinPad.visibility = android.view.View.VISIBLE
            binding.slideUnlock.text = getString(R.string.enter_pin)
        }
        var startX = 0f
        binding.slideUnlock.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> startX = e.rawX
                MotionEvent.ACTION_UP -> {
                    if (e.rawX - startX > 180) tryUnlock("")
                }
            }
            true
        }
        NextIslandBus.last?.let { binding.lockIsland.applyEvent(it) }
        handler.post(tick)
    }

    private fun bindClock() {
        binding.lockTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        binding.lockDate.text = SimpleDateFormat("EEEE, d MMMM", Locale("id")).format(Date())
    }

    private fun buildPad() {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        val size = (72 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()
        keys.forEach { k ->
            val tv = TextView(this)
            val lp = GridLayout.LayoutParams()
            lp.width = size
            lp.height = size
            lp.setMargins(margin, margin, margin, margin)
            tv.layoutParams = lp
            tv.gravity = Gravity.CENTER
            tv.text = k
            tv.textSize = 22f
            tv.setTextColor(Color.WHITE)
            if (k.isNotEmpty()) tv.setBackgroundResource(R.drawable.bg_pin_key)
            tv.setOnClickListener { onKey(k) }
            binding.pinGrid.addView(tv)
        }
    }

    private fun onKey(k: String) {
        when (k) {
            "" -> return
            "⌫" -> if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.length - 1)
            else -> if (buffer.length < 6) buffer.append(k)
        }
        binding.pinDots.text = (1..4).joinToString(" ") { i ->
            if (i <= buffer.length) "●" else "○"
        }
        if (buffer.length >= 4) tryUnlock(buffer.toString())
    }

    private fun tryUnlock(pin: String) {
        if (!store.hasPin()) {
            finish()
            return
        }
        if (pin.isEmpty()) {
            binding.pinPad.visibility = android.view.View.VISIBLE
            return
        }
        if (store.verifyPin(pin)) {
            finish()
        } else {
            buffer.clear()
            binding.pinDots.text = "○ ○ ○ ○"
            binding.pinHint.setText(R.string.wrong_pin)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // swallow — lock screen
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}
