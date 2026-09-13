package com.nextos.launcher.ui.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.nextos.launcher.R
import com.nextos.launcher.data.WidgetKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OsWidgetFactory {

    fun inflate(context: Context, kind: WidgetKind): View {
        val inf = LayoutInflater.from(context)
        return when (kind) {
            WidgetKind.CLOCK -> bindClock(inf.inflate(R.layout.widget_clock, null, false))
            WidgetKind.WEATHER -> bindWeather(inf.inflate(R.layout.widget_weather, null, false))
            WidgetKind.CALENDAR -> bindCalendar(inf.inflate(R.layout.widget_calendar, null, false))
            WidgetKind.MUSIC -> bindMusic(inf.inflate(R.layout.widget_music, null, false))
            WidgetKind.BATTERY -> bindBattery(context, inf.inflate(R.layout.widget_battery, null, false))
        }
    }

    private fun bindClock(v: View): View {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("EEEE, d MMMM", Locale("id")).format(Date())
        v.findViewById<TextView>(R.id.clockTime).text = time
        v.findViewById<TextView>(R.id.clockDate).text = date
        return v
    }

    private fun bindWeather(v: View): View {
        v.findViewById<TextView>(R.id.weatherCity).text = "Jakarta"
        v.findViewById<TextView>(R.id.weatherTemp).text = v.context.getString(R.string.weather_placeholder)
        return v
    }

    private fun bindCalendar(v: View): View {
        v.findViewById<TextView>(R.id.calWeekday).text =
            SimpleDateFormat("EEEE", Locale("id")).format(Date())
        v.findViewById<TextView>(R.id.calDay).text =
            SimpleDateFormat("d", Locale.getDefault()).format(Date())
        return v
    }

    private fun bindMusic(v: View): View {
        v.findViewById<TextView>(R.id.musicTitle).text = v.context.getString(R.string.now_playing)
        v.findViewById<ImageView>(R.id.btnPlay).setOnClickListener {
            val am = v.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            )
            am.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            )
        }
        return v
    }

    private fun bindBattery(context: Context, v: View): View {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0) level * 100 / scale else 0
        v.findViewById<TextView>(R.id.batteryText).text = "$pct%"
        return v
    }
}
