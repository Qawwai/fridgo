package com.cnlab.fridgo.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.cnlab.fridgo.R

/**
 * Central, lightweight sound player. Loads five short clips into a SoundPool
 * once and plays them by semantic type, so different actions feel different:
 *   TAP     - navigation / opening a recipe
 *   ADD     - adding an item to the fridge
 *   DELETE  - removing an item
 *   SUCCESS - recipe results loaded
 *   CART    - buy / shop action
 *
 * Implemented as a singleton initialised from the Application context so the
 * pool survives across activities and never leaks an Activity.
 */
object SoundManager {

    enum class Fx { TAP, ADD, DELETE, SUCCESS, CART }

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Fx, Int>()
    private var loaded = false

    fun init(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        p.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        val app = context.applicationContext
        ids[Fx.TAP] = p.load(app, R.raw.tap, 1)
        ids[Fx.ADD] = p.load(app, R.raw.add, 1)
        ids[Fx.DELETE] = p.load(app, R.raw.delete, 1)
        ids[Fx.SUCCESS] = p.load(app, R.raw.success, 1)
        ids[Fx.CART] = p.load(app, R.raw.cart, 1)
        pool = p
    }

    fun play(fx: Fx) {
        val p = pool ?: return
        val id = ids[fx] ?: return
        // volume tuned per-clip, kept gentle so nothing is jarring
        val vol = when (fx) {
            Fx.TAP -> 0.45f
            Fx.ADD -> 0.6f
            Fx.DELETE -> 0.55f
            Fx.SUCCESS -> 0.65f
            Fx.CART -> 0.6f
        }
        p.play(id, vol, vol, 1, 0, 1f)
    }
}
