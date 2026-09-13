package com.nextos.launcher.ui.island

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.util.SpringMotion

class NextIslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val inner: LinearLayout
    private val icon: ImageView
    private val text: TextView
    private var expanded = false
    private val listener: (IslandEvent) -> Unit = { applyEvent(it) }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_next_island, this, true)
        inner = findViewById(R.id.islandInner)
        icon = findViewById(R.id.islandIcon)
        text = findViewById(R.id.islandText)
        inner.setOnClickListener { toggle() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        NextIslandBus.add(listener)
    }

    override fun onDetachedFromWindow() {
        NextIslandBus.remove(listener)
        super.onDetachedFromWindow()
    }

    fun applyEvent(event: IslandEvent) {
        val label = if (event.body.isBlank()) event.title else "${event.title}  ${event.body}"
        text.text = label
        try {
            icon.setImageDrawable(IconCache.get(context).load(event.packageName, null))
        } catch (_: Exception) {
            icon.setImageResource(R.drawable.ic_notification)
        }
        if (event.expanded) expand() else collapse()
    }

    fun expand() {
        if (expanded) return
        expanded = true
        inner.animate().cancel()
        SpringMotion.spring(inner, androidx.dynamicanimation.animation.SpringAnimation.SCALE_X, 1.08f).start()
        val extra = (220 * resources.displayMetrics.density).toInt()
        inner.layoutParams = inner.layoutParams.apply { width = extra }
        inner.requestLayout()
        text.maxWidth = extra
    }

    fun collapse() {
        expanded = false
        val idle = resources.getDimensionPixelSize(R.dimen.island_width_idle)
        inner.layoutParams = inner.layoutParams.apply { width = idle }
        inner.requestLayout()
        SpringMotion.spring(inner, androidx.dynamicanimation.animation.SpringAnimation.SCALE_X, 1f).start()
    }

    private fun toggle() {
        if (expanded) collapse() else expand()
    }
}
