package com.cnlab.fridgo.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts (https://world.openfoodfacts.org) — a free, key-less, open
 * database of grocery products keyed by barcode. Used to turn a scanned barcode
 * into a product name (and, when available, a net weight) so the user barely has
 * to type anything when adding an item.
 */
interface ProductApi {

    /** Look a product up by its barcode (EAN/UPC). */
    @GET("api/v2/product/{barcode}.json")
    suspend fun lookup(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,brands,quantity,product_quantity,categories"
    ): ProductResponse
}

data class ProductResponse(
    @SerializedName("status") val status: Int?,
    @SerializedName("product") val product: ProductDto?
)

data class ProductDto(
    @SerializedName("product_name") val name: String?,
    @SerializedName("brands") val brands: String?,
    /** Free-text label, e.g. "500 g" or "1 L". */
    @SerializedName("quantity") val quantity: String?,
    /** Net weight in grams as a numeric string, when OFF has parsed it. */
    @SerializedName("product_quantity") val productQuantity: String?,
    @SerializedName("categories") val categories: String?
)
