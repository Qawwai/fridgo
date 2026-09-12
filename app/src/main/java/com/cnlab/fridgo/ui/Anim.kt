package com.cnlab.fridgo.ui

import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Small view-animation helpers used across the app to keep interactions lively.
 */
object Anim {

    /** Press-bounce: quick scale down then springy scale back. */
    fun bounce(view: View, onEnd: (() -> Unit)? = null) {
        view.animate().cancel()
        view.scaleX = 1f; view.scaleY = 1f
        view.animate()
            .scaleX(0.92f).scaleY(0.92f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setInterpolator(OvershootInterpolator(3f))
                    .setDuration(180)
                    .withEndAction { onEnd?.invoke() }
                    .start()
            }
            .start()
    }

    /** Fade + slight slide-up entrance for a single view. */
    fun enter(view: View, delay: Long = 0L) {
        view.alpha = 0f
        view.translationY = 40f
        view.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(delay)
            .setDuration(320)
            .start()
    }

    /** Gentle infinite pulse to draw the eye to a primary CTA. */
    fun pulse(view: View) {
        view.animate()
            .scaleX(1.04f).scaleY(1.04f)
            .setDuration(700)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(700)
                    .withEndAction { pulse(view) }
                    .start()
            }
            .start()
    }
}
