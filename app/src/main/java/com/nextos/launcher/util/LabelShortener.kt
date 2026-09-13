package com.nextos.launcher.util

object LabelShortener {
    fun short(raw: String, max: Int = 10): String {
        val trimmed = raw.trim()
        if (trimmed.length <= max) return trimmed
        val cut = trimmed.substring(0, max).trimEnd()
        return if (cut.endsWith(".")) cut else "$cut…"
    }
}
