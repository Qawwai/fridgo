package com.cnlab.fridgo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Owns all in-app store data. The catalog is built from two open sources, merged
 * so the shelves are actually full (DummyJSON alone only has ~27 groceries):
 *
 *   1. DummyJSON `products/category/groceries` — real priced items with stock.
 *   2. TheMealDB ingredient list (~500 items) — covers all the fruits, veg, meat
 *      and seafood DummyJSON is missing, each with a product photo. These have no
 *      price/stock of their own, so we synthesize a stable price from the item's
 *      category (produce is cheap, meat/seafood dearer) and mark them in stock.
 *
 * Everything is normalised into [ShopProductDto] and deduped by name. Search and
 * category filtering then happen client-side over this merged list (see
 * ShopViewModel), so they're instant and span both sources.
 */
class ShopRepository(
    private val api: ShopApi = RetrofitClient.shopApi,
    private val mealApi: MealApi = RetrofitClient.api
) {

    /** The full merged grocery catalog (DummyJSON + TheMealDB ingredients). */
    suspend fun catalog(): List<ShopProductDto> = withContext(Dispatchers.IO) {
        val (dummy, ingredients) = coroutineScope {
            val d = async { runCatching { api.groceries().products }.getOrDefault(emptyList()) }
            val i = async { runCatching { mealApi.listIngredients().ingredients }.getOrNull().orEmpty() }
            d.await() to i.await()
        }

        val seen = HashSet<String>()
        val merged = ArrayList<ShopProductDto>(dummy.size + ingredients.size)

        // DummyJSON first (it has genuine prices/stock/descriptions).
        for (p in dummy) {
            if (seen.add(p.title.trim().lowercase())) merged.add(p)
        }
        // Then every TheMealDB ingredient we don't already have.
        for (ing in ingredients) {
            val name = ing.name?.trim().orEmpty()
            if (name.isEmpty()) continue
            if (!seen.add(name.lowercase())) continue
            merged.add(ing.toProduct())
        }
        merged
    }

    /** A single product by id (null on failure). */
    suspend fun product(id: Int): ShopProductDto? = withContext(Dispatchers.IO) {
        runCatching { api.product(id) }.getOrNull()
    }

    /**
     * Seller flow: post a new product. Returns the created product (with its new
     * id) on success, or null on failure. DummyJSON simulates this — the product
     * is echoed back but not persisted server-side.
     */
    suspend fun addProduct(title: String, price: Double, description: String): ShopProductDto? =
        withContext(Dispatchers.IO) {
            runCatching {
                api.addProduct(AddProductRequest(title, price, description))
            }.getOrNull()
        }

    /** Authenticate; returns the user on success or null on bad credentials/network. */
    suspend fun login(username: String, password: String): AuthUser? =
        withContext(Dispatchers.IO) {
            runCatching { api.login(LoginRequest(username, password)) }.getOrNull()
        }

    // ----- TheMealDB ingredient -> sellable product -----

    private fun IngredientDto.toProduct(): ShopProductDto {
        val cleanName = name!!.trim()
        val encoded = URLEncoder.encode(cleanName, "UTF-8").replace("+", "%20")
        val cat = FoodCategory.of(cleanName)
        val desc = description?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Fresh ${cleanName.lowercase()} — ${cat.label.lowercase()}."
        return ShopProductDto(
            // Offset well clear of DummyJSON's small numeric ids.
            id = 900_000 + (id?.toIntOrNull() ?: abs(cleanName.hashCode()) % 100_000),
            title = cleanName,
            description = desc,
            price = synthesizePrice(cleanName, cat),
            rating = 4.0 + (abs(cleanName.hashCode()) % 10) / 10.0, // 4.0–4.9, stable
            stock = 50,
            category = cat.label,
            thumbnail = "https://www.themealdb.com/images/ingredients/$encoded-Small.png",
            images = listOf("https://www.themealdb.com/images/ingredients/$encoded-Medium.png")
        )
    }

    /** A stable, plausible price derived from the name + category. */
    private fun synthesizePrice(name: String, category: FoodCategory): Double {
        val base = when (category) {
            FoodCategory.FRUIT, FoodCategory.VEGETABLE -> 1.0
            FoodCategory.BAKERY, FoodCategory.PANTRY, FoodCategory.BEVERAGE -> 2.0
            FoodCategory.DAIRY -> 2.5
            FoodCategory.FROZEN -> 3.5
            FoodCategory.SEAFOOD -> 7.0
            FoodCategory.MEAT -> 6.0
            FoodCategory.OTHER -> 2.0
        }
        val jitter = (abs(name.hashCode()) % 400) / 100.0 // 0.00–3.99, stable per name
        return "%.2f".format(base + jitter).toDouble()
    }
}
