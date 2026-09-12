package com.cnlab.fridgo.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB open API (https://www.themealdb.com/api.php).
 * Free, key-less. Uses the public test key "1".
 */
interface MealApi {

    /** Candidate recipes that use a given ingredient. */
    @GET("filter.php")
    suspend fun filterByIngredient(@Query("i") ingredient: String): FilterResponse

    /** Candidate recipes in a given category (e.g. "Seafood", "Vegetarian"). */
    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): FilterResponse

    /** Candidate recipes from a given area / cuisine (e.g. "Japanese"). */
    @GET("filter.php")
    suspend fun filterByArea(@Query("a") area: String): FilterResponse

    /** Full recipe (ingredients + instructions) by id. */
    @GET("lookup.php")
    suspend fun lookupById(@Query("i") id: String): LookupResponse

    /** Full recipes whose name contains the search term. */
    @GET("search.php")
    suspend fun searchByName(@Query("s") name: String): LookupResponse

    /** The list of all recipe categories. */
    @GET("categories.php")
    suspend fun listCategories(): CategoriesResponse

    /** The list of all areas / cuisines (list.php?a=list). */
    @GET("list.php")
    suspend fun listAreas(@Query("a") list: String = "list"): AreaResponse

    /** One random full recipe (used for "surprise me"). */
    @GET("random.php")
    suspend fun randomMeal(): LookupResponse

    /** The full list of known ingredients (list.php?i=list) — ~500 items. */
    @GET("list.php")
    suspend fun listIngredients(@Query("i") list: String = "list"): IngredientListResponse
}
