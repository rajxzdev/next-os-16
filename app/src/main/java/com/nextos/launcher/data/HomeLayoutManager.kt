package com.nextos.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HomeLayoutManager(
    context: Context,
    private val prefs: LauncherPreferences,
    private val catalog: AppCatalog
) {
    val pages: MutableList<MutableList<HomeCell>> = mutableListOf()
    val dock: MutableList<String> = mutableListOf()
    val folders: MutableMap<String, FolderModel> = linkedMapOf()
    val widgets: MutableList<PageWidgets> = mutableListOf()

    val columns: Int get() = prefs.columns()
    val rows: Int get() = prefs.rows()
    val pageSize: Int get() = columns * rows

    fun load() {
        pages.clear()
        dock.clear()
        folders.clear()
        widgets.clear()
        loadFolders()
        loadWidgets()
        val stored = prefs.homeLayout()
        if (stored.length() == 0 || prefs.isFirstRun) {
            seed()
            persist()
            prefs.setFirstRunDone()
            return
        }
        for (p in 0 until stored.length()) {
            val arr = stored.optJSONArray(p) ?: JSONArray()
            val cells = MutableList(pageSize) { HomeCell(CellType.EMPTY) }
            for (i in 0 until minOf(pageSize, arr.length())) {
                cells[i] = parseCell(arr.optString(i))
            }
            pages.add(cells)
        }
        if (pages.isEmpty()) pages.add(emptyPage())
        val d = prefs.dockPackages()
        if (d.isEmpty()) dock.addAll(catalog.resolveDockDefaults()) else dock.addAll(d.take(4))
        while (dock.size < 4) dock.add("")
    }

    fun persist() {
        val root = JSONArray()
        for (page in pages) {
            val arr = JSONArray()
            page.forEach { arr.put(serialize(it)) }
            root.put(arr)
        }
        prefs.setHomeLayout(root)
        prefs.setDockPackages(dock.filter { it.isNotEmpty() })
        val fo = JSONObject()
        folders.values.forEach { f ->
            fo.put(f.id, JSONObject().put("name", f.name).put("apps", JSONArray(f.packages)))
        }
        prefs.setFoldersJson(fo)
        val w = JSONArray()
        widgets.forEach { pw ->
            w.put(
                JSONObject()
                    .put("page", pw.page)
                    .put("kinds", JSONArray(pw.kinds.map { it.name }))
            )
        }
        prefs.setWidgetsJson(w)
    }

    fun widgetsOn(page: Int): List<WidgetKind> =
        widgets.firstOrNull { it.page == page }?.kinds ?: emptyList()

    fun addWidget(page: Int, kind: WidgetKind) {
        val existing = widgets.firstOrNull { it.page == page }
        if (existing == null) widgets.add(PageWidgets(page, mutableListOf(kind)))
        else if (!existing.kinds.contains(kind)) existing.kinds.add(kind)
        persist()
    }

    fun removeWidget(page: Int, kind: WidgetKind) {
        widgets.firstOrNull { it.page == page }?.kinds?.remove(kind)
        persist()
    }

    fun placeApp(pkg: String): Boolean {
        if (dock.contains(pkg)) return false
        if (pages.any { page -> page.any { it.packageName == pkg } }) return false
        if (folders.values.any { it.packages.contains(pkg) }) return false
        for (page in pages) {
            val idx = page.indexOfFirst { it.type == CellType.EMPTY }
            if (idx >= 0) {
                page[idx] = HomeCell(CellType.APP, pkg)
                persist()
                return true
            }
        }
        val next = emptyPage()
        next[0] = HomeCell(CellType.APP, pkg)
        pages.add(next)
        persist()
        return true
    }

    fun removeAt(page: Int, index: Int) {
        if (page !in pages.indices || index !in pages[page].indices) return
        pages[page][index] = HomeCell(CellType.EMPTY)
        compact(page)
        persist()
    }

    fun createFolderFrom(page: Int, index: Int): String? {
        val cell = pages.getOrNull(page)?.getOrNull(index) ?: return null
        if (cell.type != CellType.APP || cell.packageName == null) return null
        val id = UUID.randomUUID().toString().take(8)
        folders[id] = FolderModel(id, "Folder", mutableListOf(cell.packageName))
        pages[page][index] = HomeCell(CellType.FOLDER, folderId = id)
        persist()
        return id
    }

    fun createFolder(page: Int, a: Int, b: Int): String? {
        val ca = pages.getOrNull(page)?.getOrNull(a) ?: return null
        val cb = pages.getOrNull(page)?.getOrNull(b) ?: return null
        val pkgs = mutableListOf<String>()
        if (ca.type == CellType.APP && ca.packageName != null) pkgs.add(ca.packageName)
        if (cb.type == CellType.APP && cb.packageName != null) pkgs.add(cb.packageName)
        if (pkgs.size < 2) return null
        val id = UUID.randomUUID().toString().take(8)
        folders[id] = FolderModel(id, "Folder", pkgs)
        pages[page][a] = HomeCell(CellType.FOLDER, folderId = id)
        pages[page][b] = HomeCell(CellType.EMPTY)
        compact(page)
        persist()
        return id
    }

    fun addToFolder(folderId: String, pkg: String) {
        folders[folderId]?.packages?.add(pkg)
        persist()
    }

    private fun compact(page: Int) {
        val filled = pages[page].filter { it.type != CellType.EMPTY }.toMutableList()
        while (filled.size < pageSize) filled.add(HomeCell(CellType.EMPTY))
        pages[page] = filled
    }

    private fun seed() {
        pages.add(emptyPage())
        widgets.add(PageWidgets(0, mutableListOf(WidgetKind.CLOCK, WidgetKind.WEATHER)))
        dock.clear()
        dock.addAll(catalog.resolveDockDefaults())
        while (dock.size < 4) dock.add("")
        val skip = dock.toSet()
        var i = 0
        for (app in catalog.allLaunchable()) {
            if (app.packageName in skip) continue
            if (i >= pageSize) {
                pages.add(emptyPage())
                i = 0
            }
            pages.last()[i] = HomeCell(CellType.APP, app.packageName)
            i++
            if (pages.size >= 3 && i >= 8) break
        }
    }

    private fun emptyPage() = MutableList(pageSize) { HomeCell(CellType.EMPTY) }

    private fun loadFolders() {
        val o = prefs.foldersJson()
        val keys = o.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val fo = o.optJSONObject(id) ?: continue
            val apps = mutableListOf<String>()
            val arr = fo.optJSONArray("apps") ?: JSONArray()
            for (i in 0 until arr.length()) apps.add(arr.optString(i))
            folders[id] = FolderModel(id, fo.optString("name", "Folder"), apps)
        }
    }

    private fun loadWidgets() {
        val arr = prefs.widgetsJson()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val kinds = mutableListOf<WidgetKind>()
            val ka = o.optJSONArray("kinds") ?: JSONArray()
            for (k in 0 until ka.length()) {
                runCatching { WidgetKind.valueOf(ka.optString(k)) }.getOrNull()?.let { kinds.add(it) }
            }
            widgets.add(PageWidgets(o.optInt("page"), kinds))
        }
    }

    private fun parseCell(raw: String): HomeCell {
        return when {
            raw.startsWith("app:") -> HomeCell(CellType.APP, packageName = raw.removePrefix("app:"))
            raw.startsWith("folder:") -> HomeCell(CellType.FOLDER, folderId = raw.removePrefix("folder:"))
            else -> HomeCell(CellType.EMPTY)
        }
    }

    private fun serialize(c: HomeCell): String = when (c.type) {
        CellType.APP -> "app:${c.packageName}"
        CellType.FOLDER -> "folder:${c.folderId}"
        CellType.EMPTY -> "empty"
    }
}
