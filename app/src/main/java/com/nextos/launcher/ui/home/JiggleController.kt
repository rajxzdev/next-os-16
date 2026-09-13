package com.nextos.launcher.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.WeakHashMap
import kotlin.random.Random

object JiggleController {
    private val running = WeakHashMap<View, AnimatorSet>()

    fun start(view: View) {
        stop(view)
        val rot = ObjectAnimator.ofFloat(view, View.ROTATION, -2.8f, 2.8f).apply {
            duration = 130
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            startDelay = Random.nextLong(0, 90)
        }
        val scale = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.98f, 1.02f).apply {
            duration = 160
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }
        val sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.98f, 1.02f).apply {
            duration = 160
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }
        val set = AnimatorSet()
        set.playTogether(rot, scale, sy) // parallel animation
        set.start()
        running[view] = set
    }

    fun stop(view: View) {
        running.remove(view)?.cancel()
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    fun stopAll() {
        running.keys.toList().forEach { stop(it) }
        running.clear()
    }
}
