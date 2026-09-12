package com.cnlab.fridgo.model

import java.io.Serializable

/** A clean domain model decoupled from the API DTOs. */
data class Recipe(
    val id: String,
    val name: String,
    val thumbUrl: String,
    val category: String = "",
    val area: String = "",
    val youtubeUrl: String = "",
    val instructions: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    /** Ingredients the user does NOT currently have. Filled in by the repository. */
    val missing: List<String> = emptyList()
) : Serializable {
    val haveCount: Int get() = ingredients.size - missing.size
    val totalCount: Int get() = ingredients.size
}

data class Ingredient(
    val name: String,
    val measure: String
) : Serializable {
    val key: String get() = name.trim().lowercase()
}
