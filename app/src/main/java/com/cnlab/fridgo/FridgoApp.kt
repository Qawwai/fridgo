package com.cnlab.fridgo

import android.app.Application
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.data.MealPlanStore
import com.cnlab.fridgo.data.SessionStore
import com.cnlab.fridgo.ui.SoundManager
import kotlinx.coroutines.runBlocking

/** Initialises shared singletons (sound pool, fridge store) once at process start. */
class FridgoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SoundManager.init(this)
        SessionStore.load(this)
        // Load the fridge from Room once at startup. The DB is tiny, so this
        // one-time blocking load keeps the synchronous read API intact without
        // any perceptible delay.
        runBlocking {
            FridgeStore.load(this@FridgoApp)
            MealPlanStore.load(this@FridgoApp)
        }
    }
}
