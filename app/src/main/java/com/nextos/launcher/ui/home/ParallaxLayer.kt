package com.nextos.launcher.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.widget.FrameLayout
import com.nextos.launcher.NextOSApplication

/** Subtle parallel / parallax shift of home content vs wallpaper. */
class ParallaxLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var enabled = true
    private var tx = 0f
    private var ty = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enabled = try {
            NextOSApplication.instance.prefs.parallax()
        } catch (_: Exception) {
            true
        }
        if (enabled && gyro != null) {
            sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDetachedFromWindow() {
        sm.unregisterListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!enabled) return
        val nx = (event.values[0] * 3.2f).coerceIn(-12f, 12f)
        val ny = (event.values[1] * 2.4f).coerceIn(-10f, 10f)
        tx += (nx - tx) * 0.08f
        ty += (ny - ty) * 0.08f
        getChildAt(0)?.translationX = -tx
        getChildAt(0)?.translationY = ty * 0.35f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
