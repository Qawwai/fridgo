package com.cnlab.fridgo.model

import java.io.Serializable

/**
 * A meal the user has added to their cooking plan. Carries enough of the recipe
 * to display it and to deduct its ingredients from the fridge once cooked,
 * without needing another network round-trip.
 */
data class PlannedMeal(
    val recipeId: String,
    val name: String,
    val thumbUrl: String,
    val area: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val cooked: Boolean = false,
    val id: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
) : Serializable {

    /** The normalized ingredient keys this meal needs. */
    val ingredientKeys: List<String>
        get() = ingredients.map { it.key }
}
