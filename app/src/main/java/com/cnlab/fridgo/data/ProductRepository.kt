package com.cnlab.fridgo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A resolved product from a scanned barcode. */
data class ProductInfo(
    val name: String,
    /** Net weight in grams, if the product database knew it. */
    val grams: Double?
)

/**
 * Looks a scanned barcode up against Open Food Facts and returns a clean
 * [ProductInfo] (name + optional weight) for the add-item flow. Network errors
 * and "not found" both collapse to null so the UI can fall back to typing.
 */
class ProductRepository(private val api: ProductApi = RetrofitClient.productApi) {

    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        val resp = runCatching { api.lookup(barcode) }.getOrNull() ?: return@withContext null
        if (resp.status != 1) return@withContext null
        val p = resp.product ?: return@withContext null

        val name = p.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: p.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@withContext null

        ProductInfo(name = name, grams = parseGrams(p))
    }

    /**
     * Prefer OFF's parsed numeric grams; otherwise pull a leading number + unit
     * out of the free-text quantity label ("500 g", "1 L" -> 1000 ml treated as g
     * is wrong, so only grams/kg are trusted).
     */
    private fun parseGrams(p: ProductDto): Double? {
        p.productQuantity?.toDoubleOrNull()?.let { if (it > 0) return it }

        val q = p.quantity?.lowercase()?.trim() ?: return null
        val match = Regex("([0-9]+([.,][0-9]+)?)\\s*(kg|g)").find(q) ?: return null
        val value = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
        return if (match.groupValues[3] == "kg") value * 1000 else value
    }
}
