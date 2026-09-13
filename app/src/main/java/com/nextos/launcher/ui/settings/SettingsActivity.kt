package com.nextos.launcher.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextos.launcher.NextOSApplication
import com.nextos.launcher.R
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.databinding.ActivitySettingsBinding
import com.nextos.launcher.util.PermissionHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: LauncherPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = LauncherPreferences(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)

        when (prefs.darkMode()) {
            LauncherPreferences.DARK_LIGHT -> binding.darkModeGroup.check(R.id.modeLight)
            LauncherPreferences.DARK_DARK -> binding.darkModeGroup.check(R.id.modeDark)
            else -> binding.darkModeGroup.check(R.id.modeSystem)
        }
        binding.darkModeGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val mode = when (id) {
                R.id.modeLight -> LauncherPreferences.DARK_LIGHT
                R.id.modeDark -> LauncherPreferences.DARK_DARK
                else -> LauncherPreferences.DARK_SYSTEM
            }
            prefs.setDarkMode(mode)
            NextOSApplication.applyDarkMode(mode)
        }

        binding.swGlass.isChecked = prefs.liquidGlass()
        binding.swGlass.setOnCheckedChangeListener { _, v -> prefs.setLiquidGlass(v) }
        binding.swSpring.isChecked = prefs.spring()
        binding.swSpring.setOnCheckedChangeListener { _, v -> prefs.setSpring(v) }
        binding.swParallax.isChecked = prefs.parallax()
        binding.swParallax.setOnCheckedChangeListener { _, v -> prefs.setParallax(v) }
        binding.swIsland.isChecked = prefs.island()
        binding.swIsland.setOnCheckedChangeListener { _, v -> prefs.setIsland(v) }
        binding.swLabels.isChecked = prefs.labels()
        binding.swLabels.setOnCheckedChangeListener { _, v -> prefs.setLabels(v) }
        binding.swLockIntegrated.isChecked = prefs.lockIntegrated()
        binding.swLockIntegrated.setOnCheckedChangeListener { _, v -> prefs.setLockIntegrated(v) }

        val colIndex = when (prefs.columns()) {
            5 -> 1
            6 -> 2
            else -> 0
        }
        binding.gridSeek.progress = colIndex
        renderGrid(colIndex)
        binding.gridSeek.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                renderGrid(progress)
                if (fromUser) {
                    val cols = 4 + progress
                    prefs.setGrid(cols, if (cols >= 6) 5 else 6)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        binding.btnWallpaper.setOnClickListener {
            startActivity(Intent(this, WallpaperActivity::class.java))
        }
        binding.btnIcons.setOnClickListener {
            startActivity(Intent(this, IconCustomizationActivity::class.java))
        }
        binding.btnHidden.setOnClickListener {
            startActivity(Intent(this, HiddenAppsActivity::class.java))
        }
        binding.btnAppLock.setOnClickListener {
            startActivity(Intent(this, AppLockSetupActivity::class.java))
        }
        binding.btnPermissions.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
        binding.btnDefaultHome.setOnClickListener { PermissionHelper.requestDefaultHome(this) }
    }

    private fun renderGrid(progress: Int) {
        val cols = 4 + progress
        val rows = if (cols >= 6) 5 else 6
        binding.gridLabel.text = "$cols x $rows"
    }
}
