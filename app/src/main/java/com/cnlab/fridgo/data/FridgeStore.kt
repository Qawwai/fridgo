package com.cnlab.fridgo.data

import android.content.Context
import com.cnlab.fridgo.model.FridgeItem
import com.cnlab.fridgo.model.Freshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for the user's fridge, now backed by Room.
 *
 * Design: Room is the durable store, but the UI (adapter, recipe matching) reads
 * synchronously, so we keep an in-memory cache mirror that is loaded once via
 * [load] and kept in sync on every mutation. Reads stay synchronous and fast;
 * writes are suspend functions that persist to Room and update the cache.
 */
object FridgeStore {

    private var dao: FridgeDao? = null
    private var statsDao: StatsDao? = null
    private val cache = mutableListOf<FridgeItem>()

    var saved: Int = 0; private set
    var wasted: Int = 0; private set

    /** Initialise from the database. Call once at startup before reading. */
    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        dao = db.fridgeDao()
        statsDao = db.statsDao()

        // Seed sample data the first time the DB is empty.
        if (db.fridgeDao().count() == 0) {
            val now = System.currentTimeMillis()
            fun daysAgo(d: Int) = now - TimeUnit.DAYS.toMillis(d.toLong())
            val seed = listOf(
                FridgeEntity(name = "Eggs", purchaseEpochMillis = daysAgo(13), shelfLifeDays = 14, quantity = 6),
                FridgeEntity(name = "Spinach", purchaseEpochMillis = daysAgo(4), shelfLifeDays = 5, quantity = 1),
                FridgeEntity(name = "Chicken", purchaseEpochMillis = daysAgo(2), shelfLifeDays = 4, quantity = 2),
                FridgeEntity(name = "Milk", purchaseEpochMillis = daysAgo(3), shelfLifeDays = 10, quantity = 1),
                FridgeEntity(name = "Rice", purchaseEpochMillis = daysAgo(20), shelfLifeDays = 365, quantity = 1),
                FridgeEntity(name = "Onion", purchaseEpochMillis = daysAgo(5), shelfLifeDays = 30, quantity = 3),
            )
            seed.forEach { db.fridgeDao().insert(it) }
        }

        cache.clear()
        cache.addAll(db.fridgeDao().getAll().map { it.toItem() })

        val s = db.statsDao().get() ?: StatsEntity().also { db.statsDao().put(it) }
        saved = s.saved
        wasted = s.wasted
    }

    // ----- Synchronous reads (unchanged API used by UI + recipe matching) -----

    fun sortedByUrgency(): List<FridgeItem> = cache.sortedBy { it.daysRemaining }

    fun ownedKeys(): Set<String> = cache.map { it.key }.toSet()

    fun expiringIngredients(count: Int = 3): List<String> =
        sortedByUrgency().take(count).map { it.name }

    fun all(): List<FridgeItem> = cache.toList()

    fun urgentCount(): Int = cache.count { it.state == Freshness.URGENT }
    fun soonCount(): Int = cache.count { it.state == Freshness.SOON }

    // ----- Suspend mutations (persist + update cache) -----

    suspend fun add(item: FridgeItem) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        val newId = d.insert(item.toEntity().copy(id = 0))
        cache.add(item.copy(id = newId))
    }

    suspend fun update(item: FridgeItem) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.update(item.toEntity())
        val idx = cache.indexOfFirst { it.id == item.id }
        if (idx >= 0) cache[idx] = item
    }

    /**
     * Remove an item, recording the user's explicit choice of whether they used
     * it in time ([asWasted] = false -> "saved") or let it go to waste
     * ([asWasted] = true -> "wasted").
     */
    suspend fun remove(item: FridgeItem, asWasted: Boolean) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.delete(item.toEntity())
        cache.removeAll { it.id == item.id }
        if (asWasted) wasted++ else saved++
        statsDao?.put(StatsEntity(0, saved, wasted))
    }

    /**
     * Consume the fridge ingredients used by a cooked meal. For each recipe
     * ingredient that matches a fridge item (same fuzzy contains-match the recipe
     * diff uses), drop the count by one — removing the item and crediting "saved"
     * when it hits zero. Returns the names of items that were used up.
     */
    suspend fun deductForMeal(ingredientKeys: List<String>): List<String> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext emptyList()
        val usedUp = mutableListOf<String>()
        for (key in ingredientKeys) {
            val idx = cache.indexOfFirst { it.key.contains(key) || key.contains(it.key) }
            if (idx < 0) continue
            val item = cache[idx]
            if (item.quantity > 1) {
                val updated = item.copy(quantity = item.quantity - 1)
                d.update(updated.toEntity())
                cache[idx] = updated
            } else {
                d.delete(item.toEntity())
                cache.removeAt(idx)
                saved++
                usedUp.add(item.name)
            }
        }
        statsDao?.put(StatsEntity(0, saved, wasted))
        usedUp
    }
}
