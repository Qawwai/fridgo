package com.cnlab.fridgo.data

import java.net.URLEncoder

/**
 * The online stores Fridgo can hand a missing ingredient off to via a search
 * deep link. Each store opens its own app (if installed) or the browser, pre-
 * searched for the item.
 *
 * Why several stores: Naver Shopping is great for packaged goods but thin on
 * fresh fruit/vegetables, so Fridgo also offers Coupang (Rocket Fresh), Market
 * Kurly (a fresh-grocery specialist) and SSG (E-Mart). For produce we surface
 * the grocery specialists first; for everything else, Naver leads.
 *
 * Search-URL formats are the public storefront search endpoints (verified to
 * reflect the query): they are deliberately opened as normal web/app links
 * rather than scraped, respecting each platform's API boundary.
 */
enum class Store(
    val displayName: String,
    val shortName: String,
    val emoji: String,
    private val searchBase: String,
    /** True for stores that are strong at fresh fruit/vegetables. */
    val goodForProduce: Boolean
) {
    NAVER(
        "Naver Shopping", "Naver", "🟢",
        "https://search.shopping.naver.com/search/all?query=",
        goodForProduce = false
    ),
    COUPANG(
        "Coupang", "Coupang", "🚀",
        "https://www.coupang.com/np/search?q=",
        goodForProduce = true
    ),
    KURLY(
        "Market Kurly", "Kurly", "💜",
        "https://www.kurly.com/search?sword=",
        goodForProduce = true
    ),
    SSG(
        "SSG (E-Mart)", "SSG", "🛒",
        "https://www.ssg.com/search.ssg?target=all&query=",
        goodForProduce = true
    );

    /** Full search URL for [query], percent-encoded. */
    fun searchUrl(query: String): String =
        searchBase + URLEncoder.encode(query, "UTF-8").replace("+", "%20")

    companion object {
        /**
         * Stores ordered best-first for an item of the given [category].
         * Fresh produce -> grocery specialists first; otherwise Naver first.
         */
        fun rankedFor(category: FoodCategory): List<Store> {
            val all = values().toList()
            return if (category.isProduce) {
                // Kurly first (fresh specialist), then Coupang, SSG, Naver last.
                listOf(KURLY, COUPANG, SSG, NAVER)
            } else {
                all.sortedByDescending { it == NAVER } // Naver first, rest in declared order
            }
        }

        /** The single recommended store for an item name. */
        fun recommendedFor(itemName: String): Store =
            rankedFor(FoodCategory.of(itemName)).first()
    }
}
