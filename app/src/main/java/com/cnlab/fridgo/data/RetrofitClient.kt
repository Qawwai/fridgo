package com.cnlab.fridgo.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Shared Retrofit instances for the app's two open APIs. */
object RetrofitClient {

    private const val MEAL_BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
    private const val OFF_BASE_URL = "https://world.openfoodfacts.org/"
    private const val SHOP_BASE_URL = "https://dummyjson.com/"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            // Open Food Facts asks API users to identify themselves via User-Agent.
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Fridgo/1.0 (Android; SKKU CNLAB student project)")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** TheMealDB — recipes. */
    val api: MealApi by lazy {
        Retrofit.Builder()
            .baseUrl(MEAL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealApi::class.java)
    }

    /** Open Food Facts — products by barcode. */
    val productApi: ProductApi by lazy {
        Retrofit.Builder()
            .baseUrl(OFF_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductApi::class.java)
    }

    /** DummyJSON — in-app store catalog (groceries), cart and seller flow. */
    val shopApi: ShopApi by lazy {
        Retrofit.Builder()
            .baseUrl(SHOP_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShopApi::class.java)
    }
}
