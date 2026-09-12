package com.cnlab.fridgo.data

import android.content.Context
import com.cnlab.fridgo.model.PlannedMeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the user's meal plan, backed by Room. Mirrors the
 * design of [FridgeStore]: a synchronous in-memory cache for fast UI reads, with
 * suspend mutations that persist to the database.
 */
object MealPlanStore {

    private var dao: PlannedMealDao? = null
    private val cache = mutableListOf<PlannedMeal>()

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        dao = AppDatabase.get(context).plannedMealDao()
        cache.clear()
        cache.addAll(dao!!.getAll().map { it.toMeal() })
    }

    // ----- Synchronous reads -----

    fun all(): List<PlannedMeal> = cache.toList()
    fun planned(): List<PlannedMeal> = cache.filter { !it.cooked }
    fun cooked(): List<PlannedMeal> = cache.filter { it.cooked }
    fun activeCount(): Int = cache.count { !it.cooked }
    fun contains(recipeId: String): Boolean = cache.any { it.recipeId == recipeId && !it.cooked }

    // ----- Suspend mutations -----

    suspend fun add(meal: PlannedMeal) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        val newId = d.insert(meal.toEntity().copy(id = 0))
        cache.add(0, meal.copy(id = newId))
    }

    suspend fun remove(meal: PlannedMeal) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.delete(meal.toEntity())
        cache.removeAll { it.id == meal.id }
    }

    /** Mark a meal cooked (kept in history, shown greyed). */
    suspend fun markCooked(meal: PlannedMeal) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        val updated = meal.copy(cooked = true)
        d.update(updated.toEntity())
        val idx = cache.indexOfFirst { it.id == meal.id }
        if (idx >= 0) cache[idx] = updated
    }
}
