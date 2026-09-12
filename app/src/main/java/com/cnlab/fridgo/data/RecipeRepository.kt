package com.cnlab.fridgo.data

import com.cnlab.fridgo.model.Ingredient
import com.cnlab.fridgo.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Owns all recipe data + the core business logic:
 *   1. find candidate recipes for the expiring ingredients (filter.php)
 *   2. enrich each with its full ingredient list (lookup.php), concurrently
 *   3. diff against the fridge to compute the "missing" set
 *   4. rank by fewest missing ingredients
 */
class RecipeRepository(private val api: MealApi = RetrofitClient.api) {

    /** Max recipes we enrich+rank, to keep the demo snappy. */
    private val maxRecipes = 12

    suspend fun recipesForExpiring(
        expiring: List<String>,
        owned: Set<String>
    ): List<Recipe> = withContext(Dispatchers.IO) {
        // Step 1: gather candidate ids across all expiring ingredients (deduped).
        val candidateIds = LinkedHashSet<String>()
        for (ing in expiring) {
            val resp = runCatching { api.filterByIngredient(ing) }.getOrNull()
            resp?.meals?.forEach { m ->
                m.id?.let { candidateIds.add(it) }
            }
            if (candidateIds.size >= maxRecipes) break
        }
        enrichAndRank(candidateIds.take(maxRecipes), owned)
    }

    // ----- Discovery (browse / search / surprise) -----

    /** Full-text recipe search by name (search.php already returns full meals). */
    suspend fun searchByName(query: String, owned: Set<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
            val resp = runCatching { api.searchByName(query) }.getOrNull()
            resp?.meals.orEmpty()
                .mapNotNull { toRecipe(it, owned) }
                .sortedWith(rankByMissing)
        }

    /** Recipes in a category (filter.php returns summaries -> enrich top N). */
    suspend fun browseByCategory(category: String, owned: Set<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
            val ids = runCatching { api.filterByCategory(category) }.getOrNull()
                ?.meals.orEmpty().mapNotNull { it.id }
            enrichAndRank(ids.take(maxRecipes), owned)
        }

    /** Recipes from a cuisine / area. */
    suspend fun browseByArea(area: String, owned: Set<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
            val ids = runCatching { api.filterByArea(area) }.getOrNull()
                ?.meals.orEmpty().mapNotNull { it.id }
            enrichAndRank(ids.take(maxRecipes), owned)
        }

    /** One random recipe for "surprise me". */
    suspend fun surprise(owned: Set<String>): Recipe? = withContext(Dispatchers.IO) {
        runCatching { api.randomMeal() }.getOrNull()
            ?.meals?.firstOrNull()
            ?.let { toRecipe(it, owned) }
    }

    /** Category names for the filter chips. */
    suspend fun categoryNames(): List<String> = withContext(Dispatchers.IO) {
        runCatching { api.listCategories() }.getOrNull()
            ?.categories.orEmpty().mapNotNull { it.name }
    }

    /** Area / cuisine names for the filter chips. */
    suspend fun areaNames(): List<String> = withContext(Dispatchers.IO) {
        runCatching { api.listAreas() }.getOrNull()
            ?.areas.orEmpty().mapNotNull { it.name }
    }

    /** Look up a set of ids concurrently, map to domain recipes, rank by missing. */
    private suspend fun enrichAndRank(ids: List<String>, owned: Set<String>): List<Recipe> {
        if (ids.isEmpty()) return emptyList()
        val recipes = coroutineScope {
            ids.map { id ->
                async {
                    runCatching { api.lookupById(id) }
                        .getOrNull()
                        ?.meals
                        ?.firstOrNull()
                        ?.let { dto -> toRecipe(dto, owned) }
                }
            }.mapNotNull { it.await() }
        }
        return recipes.sortedWith(rankByMissing)
    }

    private val rankByMissing =
        compareBy<Recipe> { it.missing.size }.thenByDescending { it.haveCount }

    /** Map a DTO to the clean domain model and compute the missing list. */
    private fun toRecipe(dto: MealDetailDto, owned: Set<String>): Recipe? {
        val id = dto.id ?: return null
        val name = dto.name ?: return null
        val ingredients = dto.ingredientPairs().map { (n, m) -> Ingredient(n, m) }
        val missing = computeMissing(ingredients, owned)
        return Recipe(
            id = id,
            name = name,
            thumbUrl = dto.thumb.orEmpty(),
            category = dto.category.orEmpty(),
            area = dto.area.orEmpty(),
            youtubeUrl = dto.youtube.orEmpty(),
            instructions = dto.instructions.orEmpty(),
            ingredients = ingredients,
            missing = missing
        )
    }

    /**
     * Core diff. An ingredient counts as "owned" if any fridge key is contained
     * in the ingredient name or vice-versa (handles "chicken breast" vs "chicken").
     */
    fun computeMissing(ingredients: List<Ingredient>, owned: Set<String>): List<String> {
        return ingredients
            .filter { ing ->
                val k = ing.key
                owned.none { o -> k.contains(o) || o.contains(k) }
            }
            .map { it.name }
    }
}
