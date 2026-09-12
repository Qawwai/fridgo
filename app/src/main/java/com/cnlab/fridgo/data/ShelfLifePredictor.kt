package com.cnlab.fridgo.data

/**
 * Predicts a sensible default shelf life (in days) for a grocery item from its
 * name, so the user usually doesn't have to set it manually. The prediction is
 * a *suggestion* only — the UI lets the user override it.
 *
 * Strategy:
 *   1. Exact/keyword match against a curated table of common groceries.
 *   2. Category fallback by matching food-group keywords (leafy greens, berries,
 *      root veg, dairy, meat, frozen, dry/pantry staples, canned, etc.).
 *   3. A safe default if nothing matches.
 *
 * Matching is case-insensitive and substring-based so "chicken breast",
 * "organic baby spinach", and "spinach" all resolve correctly.
 */
object ShelfLifePredictor {

    data class Prediction(val days: Int, val basis: String)

    // Curated keyword -> days. Ordered roughly specific -> generic; the first
    // keyword found inside the (normalized) item name wins.
    private val table: List<Pair<String, Int>> = listOf(
        // Highly perishable
        "spinach" to 5, "lettuce" to 6, "arugula" to 5, "kale" to 6,
        "basil" to 4, "cilantro" to 5, "herbs" to 5, "mushroom" to 6,
        "berry" to 4, "strawberr" to 4, "raspberr" to 3, "blueberr" to 9,
        "banana" to 6, "avocado" to 5, "asparagus" to 4, "corn" to 5,
        "fish" to 2, "salmon" to 2, "shrimp" to 3, "seafood" to 2,
        // Meat & poultry (fresh)
        "chicken" to 3, "beef" to 4, "pork" to 4, "mince" to 2,
        "ground" to 2, "sausage" to 4, "bacon" to 7, "steak" to 4, "meat" to 3,
        // Dairy & eggs
        "milk" to 9, "yogurt" to 14, "cream" to 7, "butter" to 60,
        "cheese" to 21, "egg" to 28, "tofu" to 7,
        // Fruit (sturdier)
        "apple" to 30, "orange" to 21, "lemon" to 28, "lime" to 28,
        "grape" to 9, "pear" to 12, "peach" to 5, "melon" to 7,
        "tomato" to 7, "cucumber" to 8,
        // Veg (sturdier / root)
        "broccoli" to 7, "cauliflower" to 8, "pepper" to 10, "zucchini" to 7,
        "carrot" to 30, "potato" to 45, "onion" to 35, "garlic" to 60,
        "ginger" to 30, "cabbage" to 30, "celery" to 14,
        // Bakery
        "bread" to 6, "bagel" to 6, "tortilla" to 14, "cake" to 4,
        // Pantry / dry
        "rice" to 365, "pasta" to 365, "noodle" to 365, "flour" to 240,
        "sugar" to 700, "salt" to 1000, "oil" to 365, "honey" to 1000,
        "cereal" to 180, "oats" to 240, "bean" to 365, "lentil" to 365,
        "sauce" to 30, "ketchup" to 180, "jam" to 180, "nut" to 120,
        // Frozen / canned hints
        "frozen" to 180, "canned" to 730, "can " to 730
    )

    fun predict(rawName: String): Prediction {
        val name = rawName.trim().lowercase()
        if (name.isEmpty()) return Prediction(7, "default")

        // 1 + 2: keyword/category match
        for ((keyword, days) in table) {
            if (name.contains(keyword)) {
                return Prediction(days, keyword)
            }
        }
        // 3: safe default
        return Prediction(7, "default")
    }

    /** Human-friendly label for a day count, e.g. "about 2 weeks". */
    fun humanize(days: Int): String = when {
        days <= 1 -> "about 1 day"
        days < 7 -> "about $days days"
        days < 14 -> "about 1 week"
        days < 30 -> "about ${days / 7} weeks"
        days < 60 -> "about 1 month"
        days < 365 -> "about ${days / 30} months"
        else -> "about ${days / 365} year" + if (days / 365 > 1) "s" else ""
    }
}
