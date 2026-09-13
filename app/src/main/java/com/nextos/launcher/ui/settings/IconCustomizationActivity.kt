package com.nextos.launcher.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.databinding.ActivityIconCustomBinding

class IconCustomizationActivity : AppCompatActivity() {

    private val plates = intArrayOf(
        0,
        R.drawable.plate_blue,
        R.drawable.plate_lavender,
        R.drawable.plate_mint,
        R.drawable.plate_blush,
        R.drawable.plate_peach,
        R.drawable.plate_graphite
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityIconCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)
        val prefs = LauncherPreferences(this)
        val catalog = AppCatalog(this)
        fun reload() {
            val apps = catalog.allLaunchable(includeHidden = true)
            binding.customList.layoutManager = LinearLayoutManager(this)
            binding.customList.adapter = AppToggleAdapter(
                apps,
                checked = { false },
                onToggle = { _, _ -> },
                editableName = true,
                onRename = { app, name -> prefs.setCustomName(app.packageName, name) },
                onIcon = { app ->
                    val current = prefs.customPlates()[app.packageName] ?: 0
                    val idx = plates.indexOf(current).let { if (it < 0) 0 else it }
                    val next = plates[(idx + 1) % plates.size]
                    prefs.setCustomPlate(app.packageName, next)
                    IconCache.get(this).evictPackage(app.packageName)
                    reload()
                }
            )
        }
        reload()
    }
}
