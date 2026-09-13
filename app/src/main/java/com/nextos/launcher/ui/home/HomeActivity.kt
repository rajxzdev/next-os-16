package com.nextos.launcher.ui.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.AppEntry
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.data.CellType
import com.nextos.launcher.data.HomeCell
import com.nextos.launcher.data.HomeLayoutManager
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.data.SearchHit
import com.nextos.launcher.databinding.ActivityHomeBinding
import com.nextos.launcher.receiver.PackageChangeReceiver
import com.nextos.launcher.ui.island.IslandEvent
import com.nextos.launcher.ui.island.NextIslandBus
import com.nextos.launcher.ui.library.LibraryAdapter
import com.nextos.launcher.ui.library.LibraryBuilder
import com.nextos.launcher.ui.lock.LockScreenActivity
import com.nextos.launcher.ui.search.SearchAdapter
import com.nextos.launcher.ui.settings.SettingsActivity
import com.nextos.launcher.ui.widgets.WidgetPickerAdapter
import com.nextos.launcher.util.AppLauncher
import com.nextos.launcher.util.PermissionHelper
import com.nextos.launcher.util.SpringMotion
import com.nextos.launcher.util.WallpaperStore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var prefs: LauncherPreferences
    private lateinit var catalog: AppCatalog
    private lateinit var layout: HomeLayoutManager
    private lateinit var lockStore: AppLockStore
    private lateinit var pagerAdapter: HomePagerAdapter
    private lateinit var dockAdapter: DockAdapter
    private lateinit var libraryAdapter: LibraryAdapter
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var gesture: GestureDetector

    private var jiggle = false
    private var contextPage = 0
    private var contextIndex = 0
    private var contextPkg: String? = null
    private var openFolderId: String? = null
    private var torchOn = false
    private val clock = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (::pagerAdapter.isInitialized) pagerAdapter.notifyDataSetChanged()
            clock.postDelayed(this, 30_000)
        }
    }

    private val appChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = reloadApps()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.statusBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = LauncherPreferences(this)
        catalog = AppCatalog(this)
        lockStore = AppLockStore(this)
        layout = HomeLayoutManager(this, prefs, catalog)
        layout.load()

        WallpaperStore.applyTo(binding.wallpaperView, prefs)
        binding.nextIsland.visibility = if (prefs.island()) View.VISIBLE else View.GONE
        binding.dockGlass.setCornerRadiusDp(38f)

        setupPager()
        setupDock()
        setupLibrary()
        setupSearch()
        setupControl()
        setupWidgetMenu()
        setupFolder()
        setupEditMenu()
        setupContext()
        setupGestures()
        refreshDots(0)

        NextIslandBus.idle()
        clock.post(tick)

        ContextCompat.registerReceiver(
            this,
            appChange,
            IntentFilter(PackageChangeReceiver.ACTION_APPS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun setupPager() {
        pagerAdapter = HomePagerAdapter(
            layout, catalog, prefs,
            onClick = { _, cell, view -> handleCellClick(cell, view) },
            onLong = { page, cell, index, view -> handleCellLong(page, cell, index, view) },
            onRemove = { page, index ->
                layout.removeAt(page, index)
                pagerAdapter.notifyItemChanged(page)
                refreshDots(binding.homePager.currentItem)
            }
        )
        binding.homePager.adapter = pagerAdapter
        binding.homePager.offscreenPageLimit = 1
        binding.homePager.setPageTransformer { page, position ->
            val abs = kotlin.math.abs(position)
            page.alpha = 1f - abs * 0.25f
            page.scaleX = 1f - abs * 0.04f
            page.scaleY = 1f - abs * 0.04f
            page.translationX = -position * 28f
        }
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = refreshDots(position)
        })
    }

    private fun setupDock() {
        dockAdapter = DockAdapter(
            catalog,
            onClick = { pkg, view ->
                val app = catalog.byPackage(pkg) ?: return@DockAdapter
                AppLauncher.launch(this, app.launchIntent, pkg, view)
            },
            onLong = { _, _ -> enterJiggle() }
        )
        dockAdapter.packages = layout.dock
        binding.dockList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dockList.adapter = dockAdapter
        binding.dockList.itemAnimator = null
    }

    private fun setupLibrary() {
        libraryAdapter = LibraryAdapter(prefs, { app, view ->
            closeLibrary()
            AppLauncher.launch(this, app.launchIntent, app.packageName, view)
        }, { app -> showAppContext(app) })
        binding.libraryOverlay.libraryList.layoutManager = LinearLayoutManager(this)
        binding.libraryOverlay.libraryList.adapter = libraryAdapter
        binding.libraryOverlay.librarySearch.addTextChangedListener(simpleWatcher {
            fillLibrary(it)
        })
        refreshLibrary()
    }

    private fun setupSearch() {
        searchAdapter = SearchAdapter { hit, view ->
            closeSearch()
            AppLauncher.launch(this, hit.entry.launchIntent, hit.entry.packageName, view)
        }
        binding.searchOverlay.searchResults.layoutManager = LinearLayoutManager(this)
        binding.searchOverlay.searchResults.adapter = searchAdapter
        binding.searchOverlay.smartSearchInput.addTextChangedListener(simpleWatcher { q ->
            val cache = IconCache.get(this)
            val hits = catalog.allLaunchable().filter {
                it.label.contains(q, true) || it.packageName.contains(q, true)
            }.take(24).map { SearchHit(it, cache.load(it.packageName, it.customPlate)) }
            searchAdapter.hits = hits
        })
    }

    private fun setupControl() {
        val o = binding.controlOverlay
        bindTile(o.tileWifi.root, R.drawable.ic_wifi, R.string.wifi, wifiOn()) {
            openPanel("android.settings.panel.action.WIFI")
            refreshControl()
        }
        bindTile(o.tileBt.root, R.drawable.ic_bluetooth, R.string.bluetooth, false) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        bindTile(o.tileAir.root, R.drawable.ic_airplane, R.string.airplane, false) {
            startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        }
        bindTile(o.tileCell.root, R.drawable.ic_cell, R.string.cellular, true) {
            startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        }
        bindTile(o.tileFlash.root, R.drawable.ic_flashlight, R.string.flashlight, torchOn) {
            toggleTorch()
            refreshControl()
        }
        bindTile(o.tileDnd.root, R.drawable.ic_dnd, R.string.dnd, false) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        bindTile(o.tileRotate.root, R.drawable.ic_rotation, R.string.rotation, true) {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        bindTile(o.tileDark.root, R.drawable.ic_dark_mode, R.string.dark_mode, prefs.darkMode() == 2) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        bindTile(o.tileLock.root, R.drawable.ic_lock, R.string.lock, false) {
            closeControl()
            startActivity(Intent(this, LockScreenActivity::class.java))
        }
        bindTile(o.tileHome.root, R.drawable.ic_settings, R.string.settings, false) {
            closeControl()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        o.controlRoot.setOnClickListener { closeControl() }
        o.controlSheet.setOnClickListener { /* swallow */ }
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        o.volumeBar.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        o.volumeBar.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        o.volumeBar.setOnSeekBarChangeListener(seek { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) })
        try {
            val cur = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            o.brightnessBar.progress = cur
        } catch (_: Exception) {
        }
        o.brightnessBar.setOnSeekBarChangeListener(seek { value ->
            val lp = window.attributes
            lp.screenBrightness = (value / 255f).coerceIn(0.05f, 1f)
            window.attributes = lp
        })
    }

    private fun refreshControl() {
        val o = binding.controlOverlay
        bindTile(o.tileWifi.root, R.drawable.ic_wifi, R.string.wifi, wifiOn()) {
            openPanel("android.settings.panel.action.WIFI")
            refreshControl()
        }
        bindTile(o.tileFlash.root, R.drawable.ic_flashlight, R.string.flashlight, torchOn) {
            toggleTorch()
            refreshControl()
        }
    }

    private fun setupWidgetMenu() {
        val rv = binding.widgetMenuOverlay.widgetPicker
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = WidgetPickerAdapter { kind ->
            layout.addWidget(binding.homePager.currentItem, kind)
            pagerAdapter.notifyItemChanged(binding.homePager.currentItem)
            closeWidgetMenu()
            NextIslandBus.emit(IslandEvent("Widget", kind.name, packageName, true))
        }
        binding.widgetMenuOverlay.widgetMenuRoot.setOnClickListener { closeWidgetMenu() }
    }

    private fun setupFolder() {
        binding.folderOverlay.folderRoot.setOnClickListener { closeFolder() }
        binding.folderOverlay.folderName.setOnFocusChangeListener { _, has ->
            if (!has) {
                val id = openFolderId ?: return@setOnFocusChangeListener
                layout.folders[id]?.name = binding.folderOverlay.folderName.text.toString()
                layout.persist()
                pagerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupEditMenu() {
        binding.editMenuOverlay.btnAddWidget.setOnClickListener { openWidgetMenu() }
        binding.editMenuOverlay.btnWallpaper.setOnClickListener {
            startActivity(Intent(this, com.nextos.launcher.ui.settings.WallpaperActivity::class.java))
        }
        binding.editMenuOverlay.btnDoneEdit.setOnClickListener { exitJiggle() }
    }

    private fun setupContext() {
        val c = binding.iconContextOverlay
        c.contextRoot.setOnClickListener { c.root.visibility = View.GONE }
        c.ctxRename.setOnClickListener {
            c.root.visibility = View.GONE
            val pkg = contextPkg ?: return@setOnClickListener
            val current = catalog.byPackage(pkg)?.label ?: ""
            val input = EditText(this).apply { setText(current) }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.save) { _, _ ->
                    prefs.setCustomName(pkg, input.text.toString())
                    reloadApps()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        c.ctxHide.setOnClickListener {
            contextPkg?.let { pkg ->
                prefs.hide(pkg, true)
                val cell = layout.pages.getOrNull(contextPage)?.getOrNull(contextIndex)
                if (cell?.packageName == pkg) layout.removeAt(contextPage, contextIndex)
                catalog.invalidate()
                Toast.makeText(this, R.string.app_hidden, Toast.LENGTH_SHORT).show()
                reloadApps()
            }
            c.root.visibility = View.GONE
        }
        c.ctxLock.setOnClickListener {
            val pkg = contextPkg
            c.root.visibility = View.GONE
            if (pkg == null) return@setOnClickListener
            if (!lockStore.hasPin()) {
                startActivity(Intent(this, com.nextos.launcher.ui.settings.AppLockSetupActivity::class.java))
                return@setOnClickListener
            }
            lockStore.setLocked(pkg, !lockStore.isLocked(pkg))
            Toast.makeText(this, R.string.app_locked_toast, Toast.LENGTH_SHORT).show()
            reloadApps()
        }
        c.ctxFolder.setOnClickListener {
            c.root.visibility = View.GONE
            val id = layout.createFolderFrom(contextPage, contextIndex)
            if (id != null) {
                pagerAdapter.notifyItemChanged(contextPage)
                openFolder(id)
            } else {
                enterJiggle()
            }
        }
        c.ctxRemove.setOnClickListener {
            layout.removeAt(contextPage, contextIndex)
            pagerAdapter.notifyItemChanged(contextPage)
            c.root.visibility = View.GONE
        }
    }

    private fun setupGestures() {
        gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val h = binding.root.height
                if (kotlin.math.abs(velocityX) > kotlin.math.abs(velocityY)) return false
                if (e1.y < h * 0.18f && velocityY > 900) {
                    openControl()
                    return true
                }
                if (e1.y > h * 0.72f && velocityY < -900) {
                    openLibrary()
                    return true
                }
                if (velocityY > 1400 && e1.y in h * 0.2f..h * 0.6f) {
                    openSearch()
                    return true
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                openSearch()
                return true
            }
        })
        binding.root.setOnTouchListener { _, ev ->
            gesture.onTouchEvent(ev)
            false
        }
    }

    private fun handleCellClick(cell: HomeCell, view: View) {
        if (jiggle) return
        when (cell.type) {
            CellType.APP -> {
                val app = catalog.byPackage(cell.packageName ?: return) ?: return
                AppLauncher.launch(this, app.launchIntent, app.packageName, view)
            }
            CellType.FOLDER -> openFolder(cell.folderId)
            CellType.EMPTY -> Unit
        }
    }

    private fun handleCellLong(page: Int, cell: HomeCell, index: Int, view: View) {
        contextPage = page
        contextIndex = index
        contextPkg = cell.packageName
        if (cell.type == CellType.EMPTY) {
            enterJiggle()
            return
        }
        if (cell.type == CellType.FOLDER) {
            openFolder(cell.folderId)
            return
        }
        binding.iconContextOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.iconContextOverlay.contextSheet)
        enterJiggle()
    }

    private fun showAppContext(app: AppEntry) {
        contextPkg = app.packageName
        binding.iconContextOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.iconContextOverlay.contextSheet)
    }

    private fun enterJiggle() {
        if (jiggle) return
        jiggle = true
        pagerAdapter.jiggle = true
        dockAdapter.jiggle = true
        dockAdapter.notifyDataSetChanged()
        binding.editMenuOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.editMenuOverlay.root)
        NextIslandBus.emit(IslandEvent("Edit", getString(R.string.edit_home), packageName, true))
    }

    private fun exitJiggle() {
        jiggle = false
        JiggleController.stopAll()
        pagerAdapter.jiggle = false
        dockAdapter.jiggle = false
        dockAdapter.notifyDataSetChanged()
        binding.editMenuOverlay.root.visibility = View.GONE
        NextIslandBus.idle()
    }

    private fun openLibrary() {
        refreshLibrary()
        val v = binding.libraryOverlay.root
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationY = 80f
        SpringMotion.slideY(v, 80f, 0f)
        NextIslandBus.emit(IslandEvent(getString(R.string.app_library), "", packageName, true))
    }

    private fun closeLibrary() {
        SpringMotion.popOut(binding.libraryOverlay.root, null)
        hideKeyboard()
        NextIslandBus.idle()
    }

    private fun openSearch() {
        binding.searchOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.searchOverlay.root)
        binding.searchOverlay.smartSearchInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchOverlay.smartSearchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        SpringMotion.popOut(binding.searchOverlay.root, null)
        hideKeyboard()
    }

    private fun openControl() {
        binding.controlOverlay.root.visibility = View.VISIBLE
        refreshControl()
        SpringMotion.overshootY(binding.controlOverlay.controlSheet, -240f, 0f)
        binding.controlOverlay.root.alpha = 0f
        binding.controlOverlay.root.animate().alpha(1f).setDuration(220).start()
    }

    private fun closeControl() {
        SpringMotion.popOut(binding.controlOverlay.root, null)
    }

    private fun openWidgetMenu() {
        binding.widgetMenuOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.widgetMenuOverlay.root)
    }

    private fun closeWidgetMenu() {
        binding.widgetMenuOverlay.root.visibility = View.GONE
    }

    private fun openFolder(id: String?) {
        val folder = layout.folders[id] ?: return
        openFolderId = id
        binding.folderOverlay.folderName.setText(folder.name)
        val rv = binding.folderOverlay.folderGrid
        rv.layoutManager = GridLayoutManager(this, 4)
        val apps = folder.packages.mapNotNull { catalog.byPackage(it) }
        rv.adapter = com.nextos.launcher.ui.library.MiniAppAdapter(
            apps, prefs,
            { app, view ->
                closeFolder()
                AppLauncher.launch(this, app.launchIntent, app.packageName, view)
            },
            { }
        )
        binding.folderOverlay.root.visibility = View.VISIBLE
        SpringMotion.popIn(binding.folderOverlay.folderSheet)
    }

    private fun closeFolder() {
        val id = openFolderId
        if (id != null) {
            layout.folders[id]?.name = binding.folderOverlay.folderName.text.toString()
            layout.persist()
        }
        binding.folderOverlay.root.visibility = View.GONE
        openFolderId = null
    }

    private fun refreshLibrary() = fillLibrary(binding.libraryOverlay.librarySearch.text?.toString().orEmpty())

    private fun fillLibrary(query: String) {
        val all = catalog.allLaunchable()
        val filtered = if (query.isBlank()) all else all.filter {
            it.label.contains(query, true) || it.packageName.contains(query, true)
        }
        libraryAdapter.groups = LibraryBuilder.build(this, filtered, catalog.recent())
    }

    private fun reloadApps() {
        layout.load()
        pagerAdapter.notifyDataSetChanged()
        dockAdapter.packages = layout.dock
        refreshLibrary()
        refreshDots(binding.homePager.currentItem)
    }

    private fun refreshDots(selected: Int) {
        val bar = binding.pageDots
        bar.removeAllViews()
        val size = resources.getDimensionPixelSize(R.dimen.page_dot)
        val gap = (6 * resources.displayMetrics.density).toInt()
        repeat(layout.pages.size) { i ->
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginStart = gap
            lp.marginEnd = gap
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.bg_page_dot)
            dot.isSelected = i == selected
            bar.addView(dot)
        }
    }

    private fun bindTile(root: View, icon: Int, label: Int, on: Boolean, click: () -> Unit) {
        root.findViewById<ImageView>(R.id.tileIcon).setImageResource(icon)
        root.findViewById<TextView>(R.id.tileLabel).setText(label)
        root.findViewById<View>(R.id.tileRoot)
            .setBackgroundResource(if (on) R.drawable.bg_tile_on else R.drawable.bg_tile)
        root.setOnClickListener { click() }
    }

    private fun wifiOn(): Boolean {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return wm.isWifiEnabled
    }

    private fun toggleTorch() {
        try {
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cid ->
                cm.getCameraCharacteristics(cid)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        } catch (_: Exception) {
        }
    }

    private fun openPanel(action: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(Intent(action))
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun simpleWatcher(block: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            block(s?.toString().orEmpty())
        }
    }

    private fun seek(block: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) block(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.iconContextOverlay.root.visibility == View.VISIBLE ->
                binding.iconContextOverlay.root.visibility = View.GONE
            binding.folderOverlay.root.visibility == View.VISIBLE -> closeFolder()
            binding.widgetMenuOverlay.root.visibility == View.VISIBLE -> closeWidgetMenu()
            binding.controlOverlay.root.visibility == View.VISIBLE -> closeControl()
            binding.searchOverlay.root.visibility == View.VISIBLE -> closeSearch()
            binding.libraryOverlay.root.visibility == View.VISIBLE -> closeLibrary()
            jiggle -> exitJiggle()
            else -> { /* stay on home */ }
        }
    }

    override fun onResume() {
        super.onResume()
        WallpaperStore.applyTo(binding.wallpaperView, prefs)
        binding.nextIsland.visibility = if (prefs.island()) View.VISIBLE else View.GONE
        pagerAdapter.notifyDataSetChanged()
        if (!PermissionHelper.isDefaultHome(this)) {
            NextIslandBus.emit(
                IslandEvent(getString(R.string.set_as_home), "", packageName, true)
            )
        }
    }

    override fun onDestroy() {
        clock.removeCallbacks(tick)
        runCatching { unregisterReceiver(appChange) }
        super.onDestroy()
    }
}
