package com.cnlab.fridgo.data

/**
 * Coarse grocery categories used for two things:
 *   1. A friendly emoji + label on each fridge card.
 *   2. Routing the user to the *right* store when shopping (fresh produce sells
 *      far better on Kurly/Coupang Fresh than on general Naver Shopping).
 *
 * Classification is name-based and substring/case-insensitive, sharing the same
 * spirit as [ShelfLifePredictor]. The first matching keyword wins, so the table
 * is ordered specific -> generic.
 */
enum class FoodCategory(val label: String, val emoji: String) {
    FRUIT("Fruit", "🍎"),        // 🍎
    VEGETABLE("Vegetable", "🥦"),// 🥦
    MEAT("Meat", "🥩"),          // 🥩
    SEAFOOD("Seafood", "🐟"),    // 🐟
    DAIRY("Dairy & Eggs", "🥛"), // 🥛
    BAKERY("Bakery", "🍞"),      // 🍞
    FROZEN("Frozen", "🧊"),      // 🧊
    BEVERAGE("Drinks", "🥤"),    // 🥤
    PANTRY("Pantry", "🥫"),      // 🥫
    OTHER("Other", "🍽️");  // 🍽️

    /** Fresh produce is the case Naver Shopping handles poorly. */
    val isProduce: Boolean get() = this == FRUIT || this == VEGETABLE

    companion object {
        // keyword -> category, ordered specific first.
        private val table: List<Pair<String, FoodCategory>> = listOf(
            // Fruit
            "strawberr" to FRUIT, "raspberr" to FRUIT, "blueberr" to FRUIT, "berry" to FRUIT,
            "apple" to FRUIT, "banana" to FRUIT, "orange" to FRUIT, "lemon" to FRUIT,
            "lime" to FRUIT, "grape" to FRUIT, "pear" to FRUIT, "peach" to FRUIT,
            "melon" to FRUIT, "mango" to FRUIT, "kiwi" to FRUIT, "avocado" to FRUIT,
            "cherry" to FRUIT, "plum" to FRUIT, "pineapple" to FRUIT, "fruit" to FRUIT,
            // Vegetables & fresh herbs
            "spinach" to VEGETABLE, "lettuce" to VEGETABLE, "arugula" to VEGETABLE,
            "kale" to VEGETABLE, "cabbage" to VEGETABLE, "broccoli" to VEGETABLE,
            "cauliflower" to VEGETABLE, "carrot" to VEGETABLE, "potato" to VEGETABLE,
            "onion" to VEGETABLE, "garlic" to VEGETABLE, "ginger" to VEGETABLE,
            "pepper" to VEGETABLE, "zucchini" to VEGETABLE, "cucumber" to VEGETABLE,
            "tomato" to VEGETABLE, "mushroom" to VEGETABLE, "asparagus" to VEGETABLE,
            "celery" to VEGETABLE, "corn" to VEGETABLE, "bean sprout" to VEGETABLE,
            "scallion" to VEGETABLE, "leek" to VEGETABLE, "radish" to VEGETABLE,
            "eggplant" to VEGETABLE, "basil" to VEGETABLE, "cilantro" to VEGETABLE,
            "parsley" to VEGETABLE, "herb" to VEGETABLE, "veg" to VEGETABLE,
            // Seafood (before meat so "fish" etc. win)
            "salmon" to SEAFOOD, "tuna" to SEAFOOD, "shrimp" to SEAFOOD,
            "prawn" to SEAFOOD, "squid" to SEAFOOD, "octopus" to SEAFOOD,
            "crab" to SEAFOOD, "clam" to SEAFOOD, "oyster" to SEAFOOD,
            "mackerel" to SEAFOOD, "anchovy" to SEAFOOD, "seafood" to SEAFOOD,
            "fish" to SEAFOOD,
            // Meat
            "chicken" to MEAT, "beef" to MEAT, "pork" to MEAT, "lamb" to MEAT,
            "bacon" to MEAT, "sausage" to MEAT, "ham" to MEAT, "steak" to MEAT,
            "mince" to MEAT, "ground" to MEAT, "meat" to MEAT,
            // Dairy & eggs
            "milk" to DAIRY, "yogurt" to DAIRY, "yoghurt" to DAIRY, "cream" to DAIRY,
            "butter" to DAIRY, "cheese" to DAIRY, "egg" to DAIRY, "tofu" to DAIRY,
            // Bakery
            "bread" to BAKERY, "bagel" to BAKERY, "tortilla" to BAKERY,
            "cake" to BAKERY, "bun" to BAKERY, "croissant" to BAKERY,
            // Frozen
            "frozen" to FROZEN, "ice cream" to FROZEN, "dumpling" to FROZEN,
            // Beverage
            "juice" to BEVERAGE, "soda" to BEVERAGE, "cola" to BEVERAGE,
            "water" to BEVERAGE, "tea" to BEVERAGE, "coffee" to BEVERAGE,
            "beer" to BEVERAGE, "wine" to BEVERAGE, "drink" to BEVERAGE,
            // Pantry / dry
            "rice" to PANTRY, "pasta" to PANTRY, "noodle" to PANTRY, "flour" to PANTRY,
            "sugar" to PANTRY, "salt" to PANTRY, "oil" to PANTRY, "honey" to PANTRY,
            "cereal" to PANTRY, "oats" to PANTRY, "bean" to PANTRY, "lentil" to PANTRY,
            "sauce" to PANTRY, "ketchup" to PANTRY, "jam" to PANTRY, "nut" to PANTRY,
            "canned" to PANTRY, "spice" to PANTRY, "vinegar" to PANTRY
        )

        fun of(rawName: String): FoodCategory {
            val name = rawName.trim().lowercase()
            if (name.isEmpty()) return OTHER
            for ((keyword, cat) in table) {
                if (name.contains(keyword)) return cat
            }
            return OTHER
        }

        fun emojiFor(rawName: String): String = of(rawName).emoji
    }
}
