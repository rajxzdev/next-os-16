package com.nextos.launcher.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextos.launcher.R
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.databinding.ActivityHiddenAppsBinding

class HiddenAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHiddenAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)
        val prefs = LauncherPreferences(this)
        val apps = AppCatalog(this).allLaunchable(includeHidden = true)
        binding.hiddenList.layoutManager = LinearLayoutManager(this)
        binding.hiddenList.adapter = AppToggleAdapter(
            apps,
            checked = { it.hidden },
            onToggle = { app, on -> prefs.hide(app.packageName, on) }
        )
    }
}
