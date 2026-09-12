package com.cnlab.fridgo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cnlab.fridgo.model.Ingredient
import com.cnlab.fridgo.model.PlannedMeal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room row backing a [PlannedMeal]. The ingredient list is stored as a small
 * JSON blob so the whole recipe survives a restart and can be deducted from the
 * fridge later without re-fetching it.
 */
@Entity(tableName = "planned_meals")
data class PlannedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val name: String,
    val thumbUrl: String,
    val area: String,
    val ingredientsJson: String,
    val cooked: Boolean,
    val addedAt: Long
)

private val gson = Gson()
private val ingredientListType = object : TypeToken<List<Ingredient>>() {}.type

fun PlannedMealEntity.toMeal(): PlannedMeal = PlannedMeal(
    recipeId = recipeId,
    name = name,
    thumbUrl = thumbUrl,
    area = area,
    ingredients = runCatching {
        gson.fromJson<List<Ingredient>>(ingredientsJson, ingredientListType)
    }.getOrNull().orEmpty(),
    cooked = cooked,
    id = id,
    addedAt = addedAt
)

fun PlannedMeal.toEntity(): PlannedMealEntity = PlannedMealEntity(
    id = id,
    recipeId = recipeId,
    name = name,
    thumbUrl = thumbUrl,
    area = area,
    ingredientsJson = gson.toJson(ingredients),
    cooked = cooked,
    addedAt = addedAt
)
