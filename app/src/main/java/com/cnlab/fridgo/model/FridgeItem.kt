package com.cnlab.fridgo.model

import java.util.concurrent.TimeUnit

/**
 * A single item in the user's fridge.
 *
 * Freshness is derived from the purchase date and a per-item shelf life.
 * The UI uses [freshnessPercent] (0..100) and [state] to render the colored bar.
 */
data class FridgeItem(
    val name: String,
    val purchaseEpochMillis: Long,
    val shelfLifeDays: Int,
    val id: Long = 0L,
    val quantity: Int = 1,
    /** Optional weight in grams, shown alongside the count when present. */
    val grams: Double? = null
) {
    /** Whole days elapsed since purchase. */
    private val daysElapsed: Long
        get() = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - purchaseEpochMillis)

    /** Days remaining before expiry (can be negative if already expired). */
    val daysRemaining: Long
        get() = shelfLifeDays - daysElapsed

    /** 0..100 freshness, clamped. 100 = just bought, 0 = expired. */
    val freshnessPercent: Int
        get() {
            if (shelfLifeDays <= 0) return 0
            val pct = (daysRemaining.toFloat() / shelfLifeDays.toFloat()) * 100f
            return pct.coerceIn(0f, 100f).toInt()
        }

    val state: Freshness
        get() = when {
            daysRemaining <= 1 -> Freshness.URGENT   // red
            freshnessPercent <= 40 -> Freshness.SOON  // amber
            else -> Freshness.FRESH                   // green
        }

    /** Absolute expiry instant (purchase + shelf life). */
    val expiryEpochMillis: Long
        get() = purchaseEpochMillis + TimeUnit.DAYS.toMillis(shelfLifeDays.toLong())

    /** Normalized name used for matching against recipe ingredients. */
    val key: String
        get() = name.trim().lowercase()

    /** Short human label for the amount, e.g. "×2 · 500 g", "300 g", or "×2". */
    val amountLabel: String
        get() {
            val g = grams?.let { gr ->
                if (gr >= 1000) "${(gr / 1000).trimZeros()} kg" else "${gr.trimZeros()} g"
            }
            val q = if (quantity > 1) "×$quantity" else null
            return listOfNotNull(q, g).joinToString("  ·  ")
        }
}

private fun Double.trimZeros(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)

enum class Freshness { FRESH, SOON, URGENT }
