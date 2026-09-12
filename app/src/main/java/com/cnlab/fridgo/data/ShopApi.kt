package com.cnlab.fridgo.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.Serializable

/**
 * DummyJSON (https://dummyjson.com) — a free, key-less, open REST API that serves
 * a real catalog of products (including a "groceries" category of food/ingredient
 * items with images, prices and stock). Fridgo uses it to power an in-app store:
 * browse groceries, view an item, add to cart, simulated checkout, and (for the
 * seller flow) post a new product.
 *
 * Note: DummyJSON *simulates* writes — POST/PUT/DELETE return a correct-looking
 * response (new id, updated fields) but do not persist server-side. That is the
 * intended behaviour for a prototype/demo and is plenty for a working buyer +
 * seller UI without any API key or account.
 */
interface ShopApi {

    /** All grocery/food products. */
    @GET("products/category/groceries")
    suspend fun groceries(
        @Query("limit") limit: Int = 0 // 0 = no limit on DummyJSON
    ): ProductListResponse

    /** Free-text product search across the whole catalog. */
    @GET("products/search")
    suspend fun search(@Query("q") query: String): ProductListResponse

    /** A single product by id. */
    @GET("products/{id}")
    suspend fun product(@Path("id") id: Int): ShopProductDto

    /** Seller flow: add a new product (simulated; echoes back with a new id). */
    @retrofit2.http.POST("products/add")
    suspend fun addProduct(@Body body: AddProductRequest): ShopProductDto

    /** Authenticate against DummyJSON's user directory (demo: emilys/emilyspass). */
    @retrofit2.http.POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthUser
}

data class ProductListResponse(
    @SerializedName("products") val products: List<ShopProductDto> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

/**
 * One catalog product. Implements [Serializable] so a product can ride along in
 * an Intent extra (matching how the app already passes its Recipe model around).
 */
data class ShopProductDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("discountPercentage") val discountPercentage: Double = 0.0,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("images") val images: List<String> = emptyList()
) : Serializable {

    /** Best image to show (full image if present, else thumbnail). */
    val heroImage: String get() = images.firstOrNull() ?: thumbnail.orEmpty()

    /** Price formatted for display, e.g. "$1.99". */
    val priceLabel: String get() = "$%.2f".format(price)
}

/** Body for the seller "add product" call. */
data class AddProductRequest(
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String = "groceries"
)

/** Credentials for the login call. */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

/** The authenticated user returned by `auth/login`. */
data class AuthUser(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("firstName") val firstName: String = "",
    @SerializedName("lastName") val lastName: String = "",
    @SerializedName("image") val image: String? = null,
    @SerializedName("accessToken") val accessToken: String? = null
) : Serializable {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { username }
}
