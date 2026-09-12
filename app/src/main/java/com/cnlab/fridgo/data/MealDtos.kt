package com.cnlab.fridgo.data

import com.google.gson.annotations.SerializedName

/** Response of filter.php?i=<ingredient> -> light recipe summaries. */
data class FilterResponse(
    @SerializedName("meals") val meals: List<MealSummaryDto>?
)

data class MealSummaryDto(
    @SerializedName("idMeal") val id: String?,
    @SerializedName("strMeal") val name: String?,
    @SerializedName("strMealThumb") val thumb: String?
)

/** Response of lookup.php?i=<id> -> full meal record. */
data class LookupResponse(
    @SerializedName("meals") val meals: List<MealDetailDto>?
)

/** Response of categories.php -> the list of recipe categories. */
data class CategoriesResponse(
    @SerializedName("categories") val categories: List<CategoryDto>?
)

data class CategoryDto(
    @SerializedName("idCategory") val id: String?,
    @SerializedName("strCategory") val name: String?,
    @SerializedName("strCategoryThumb") val thumb: String?,
    @SerializedName("strCategoryDescription") val description: String?
)

/** Response of list.php?a=list -> the available areas / cuisines. */
data class AreaResponse(
    @SerializedName("meals") val areas: List<AreaDto>?
)

data class AreaDto(
    @SerializedName("strArea") val name: String?
)

/** Response of list.php?i=list -> every known ingredient. */
data class IngredientListResponse(
    @SerializedName("meals") val ingredients: List<IngredientDto>?
)

data class IngredientDto(
    @SerializedName("idIngredient") val id: String?,
    @SerializedName("strIngredient") val name: String?,
    @SerializedName("strDescription") val description: String?,
    @SerializedName("strType") val type: String?
)

/**
 * TheMealDB returns ingredients as 20 flat fields strIngredient1..20 and
 * strMeasure1..20. Gson maps each explicitly; the repository flattens them.
 */
data class MealDetailDto(
    @SerializedName("idMeal") val id: String?,
    @SerializedName("strMeal") val name: String?,
    @SerializedName("strMealThumb") val thumb: String?,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?,
    @SerializedName("strInstructions") val instructions: String?,
    @SerializedName("strYoutube") val youtube: String?,

    @SerializedName("strIngredient1") val i1: String?, @SerializedName("strMeasure1") val m1: String?,
    @SerializedName("strIngredient2") val i2: String?, @SerializedName("strMeasure2") val m2: String?,
    @SerializedName("strIngredient3") val i3: String?, @SerializedName("strMeasure3") val m3: String?,
    @SerializedName("strIngredient4") val i4: String?, @SerializedName("strMeasure4") val m4: String?,
    @SerializedName("strIngredient5") val i5: String?, @SerializedName("strMeasure5") val m5: String?,
    @SerializedName("strIngredient6") val i6: String?, @SerializedName("strMeasure6") val m6: String?,
    @SerializedName("strIngredient7") val i7: String?, @SerializedName("strMeasure7") val m7: String?,
    @SerializedName("strIngredient8") val i8: String?, @SerializedName("strMeasure8") val m8: String?,
    @SerializedName("strIngredient9") val i9: String?, @SerializedName("strMeasure9") val m9: String?,
    @SerializedName("strIngredient10") val i10: String?, @SerializedName("strMeasure10") val m10: String?,
    @SerializedName("strIngredient11") val i11: String?, @SerializedName("strMeasure11") val m11: String?,
    @SerializedName("strIngredient12") val i12: String?, @SerializedName("strMeasure12") val m12: String?,
    @SerializedName("strIngredient13") val i13: String?, @SerializedName("strMeasure13") val m13: String?,
    @SerializedName("strIngredient14") val i14: String?, @SerializedName("strMeasure14") val m14: String?,
    @SerializedName("strIngredient15") val i15: String?, @SerializedName("strMeasure15") val m15: String?,
    @SerializedName("strIngredient16") val i16: String?, @SerializedName("strMeasure16") val m16: String?,
    @SerializedName("strIngredient17") val i17: String?, @SerializedName("strMeasure17") val m17: String?,
    @SerializedName("strIngredient18") val i18: String?, @SerializedName("strMeasure18") val m18: String?,
    @SerializedName("strIngredient19") val i19: String?, @SerializedName("strMeasure19") val m19: String?,
    @SerializedName("strIngredient20") val i20: String?, @SerializedName("strMeasure20") val m20: String?
) {
    /** Pair up ingredient/measure fields and drop blanks. */
    fun ingredientPairs(): List<Pair<String, String>> {
        val ings = listOf(i1,i2,i3,i4,i5,i6,i7,i8,i9,i10,i11,i12,i13,i14,i15,i16,i17,i18,i19,i20)
        val meas = listOf(m1,m2,m3,m4,m5,m6,m7,m8,m9,m10,m11,m12,m13,m14,m15,m16,m17,m18,m19,m20)
        val out = mutableListOf<Pair<String, String>>()
        for (idx in ings.indices) {
            val n = ings[idx]?.trim().orEmpty()
            if (n.isNotEmpty()) out.add(n to (meas[idx]?.trim().orEmpty()))
        }
        return out
    }
}
