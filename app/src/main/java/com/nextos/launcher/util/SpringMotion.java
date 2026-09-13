package com.nextos.launcher.util;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Shared spring physics for elastic open / close and parallel property motion.
 */
public final class SpringMotion {

    private SpringMotion() {
    }

    public static SpringForce force(float stiffness, float damping) {
        SpringForce f = new SpringForce();
        f.setStiffness(stiffness);
        f.setDampingRatio(damping);
        return f;
    }

    public static SpringAnimation spring(View view, DynamicAnimation.ViewProperty property,
                                         float finalValue) {
        SpringAnimation anim = new SpringAnimation(view, property, finalValue);
        anim.setSpring(force(SpringForce.STIFFNESS_MEDIUM, 0.58f));
        return anim;
    }

    public static void popIn(View view) {
        view.setScaleX(0.82f);
        view.setScaleY(0.82f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        spring(view, SpringAnimation.SCALE_X, 1f).start();
        spring(view, SpringAnimation.SCALE_Y, 1f).start();
        spring(view, SpringAnimation.ALPHA, 1f)
                .setSpring(force(SpringForce.STIFFNESS_LOW, 0.9f))
                .start();
    }

    public static void popOut(final View view, final Runnable end) {
        SpringAnimation sx = spring(view, SpringAnimation.SCALE_X, 0.88f);
        SpringAnimation sy = spring(view, SpringAnimation.SCALE_Y, 0.88f);
        SpringAnimation a = spring(view, SpringAnimation.ALPHA, 0f);
        a.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled,
                                       float value, float velocity) {
                view.setVisibility(View.GONE);
                view.setScaleX(1f);
                view.setScaleY(1f);
                if (end != null) {
                    end.run();
                }
            }
        });
        sx.start();
        sy.start();
        a.start();
    }

    public static void press(View view) {
        spring(view, SpringAnimation.SCALE_X, 0.88f)
                .setSpring(force(SpringForce.STIFFNESS_HIGH, 0.45f)).start();
        spring(view, SpringAnimation.SCALE_Y, 0.88f)
                .setSpring(force(SpringForce.STIFFNESS_HIGH, 0.45f)).start();
    }

    public static void release(View view) {
        spring(view, SpringAnimation.SCALE_X, 1f)
                .setSpring(force(SpringForce.STIFFNESS_LOW, 0.38f)).start();
        spring(view, SpringAnimation.SCALE_Y, 1f)
                .setSpring(force(SpringForce.STIFFNESS_LOW, 0.38f)).start();
    }

    public static void slideY(View view, float from, float to) {
        view.setTranslationY(from);
        view.setVisibility(View.VISIBLE);
        spring(view, SpringAnimation.TRANSLATION_Y, to)
                .setSpring(force(380f, 0.62f))
                .start();
        spring(view, SpringAnimation.ALPHA, 1f).start();
    }

    public static void overshootY(View view, float from, float to) {
        view.setTranslationY(from);
        view.animate()
                .translationY(to)
                .setDuration(520)
                .setInterpolator(new OvershootInterpolator(1.18f))
                .start();
    }

    public static void staggerChildren(ViewGroup group) {
        int n = group.getChildCount();
        for (int i = 0; i < n; i++) {
            View c = group.getChildAt(i);
            c.setAlpha(0f);
            c.setTranslationY(24f);
            c.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 28L)
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(0.9f))
                    .start();
        }
    }
}
