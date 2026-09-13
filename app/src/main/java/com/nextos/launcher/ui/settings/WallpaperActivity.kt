package com.nextos.launcher.ui.settings

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nextos.launcher.R
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.databinding.ActivityWallpaperBinding
import com.nextos.launcher.util.WallpaperStore

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperBinding
    private lateinit var prefs: LauncherPreferences

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val bmp = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.setTargetSampleSize(2)
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
        val path = WallpaperStore.saveCustom(this, bmp)
        prefs.setWallpaperPath(path)
        prefs.setWallpaperId(LauncherPreferences.WALL_CUSTOM)
        binding.preview.setImageBitmap(bmp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = LauncherPreferences(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)
        WallpaperStore.applyTo(binding.preview, prefs)

        binding.wallAurora.setOnClickListener { pickBuiltIn(LauncherPreferences.WALL_AURORA) }
        binding.wallDusk.setOnClickListener { pickBuiltIn(LauncherPreferences.WALL_DUSK) }
        binding.wallSand.setOnClickListener { pickBuiltIn(LauncherPreferences.WALL_SAND) }
        binding.btnPick.setOnClickListener { picker.launch("image/*") }
        binding.btnApply.setOnClickListener {
            val bmp = WallpaperStore.currentBitmap(this, prefs)
            if (bmp != null) WallpaperStore.pushSystem(this, bmp)
            Toast.makeText(this, R.string.apply_wallpaper, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun pickBuiltIn(id: Int) {
        prefs.setWallpaperId(id)
        WallpaperStore.applyTo(binding.preview, prefs)
    }
}
