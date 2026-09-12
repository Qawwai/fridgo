# Fridgo

An Android app for tracking fridge ingredients and finding recipes that use what you already have.

Fridgo sorts ingredients by estimated expiry, searches for recipes using the most urgent items, and ranks results by the number of ingredients still needed. Built in Kotlin as a mobile app programming project.

## Features

- **Fridge inventory:** add and remove ingredients, with the most urgent items shown first.
- **Shelf-life estimates:** name-based estimates with a manual override.
- **Recipe discovery:** retrieve recipes from TheMealDB using ingredients already in the fridge.
- **Ingredient matching:** compare recipes with the inventory and rank by the fewest missing items.
- **Shopping links:** open Naver Shopping searches for missing ingredients.
- **Recipe details:** view ingredients, instructions, and recipe images.

## Run the app

1. Clone this repository and open its root folder in Android Studio.
2. Use JDK 17 for Gradle and install Android SDK 34.
3. Allow Gradle to sync and download the dependencies.
4. Run the `app` configuration on an emulator or device running Android 7.0 (API 24) or later.

The fridge starts with sample ingredients. Add an ingredient, select **What can I make?**, open a recipe, and use **Shop missing items** to browse the ingredients you need.

To build a debug APK with the Android SDK configured:

```bash
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/`.

## Implementation

Kotlin · Android Views / XML · View Binding · ViewModel · Coroutines · Retrofit · OkHttp · Gson · Glide

```text
app/src/main/java/com/cnlab/fridgo/
├── ui/       Activities, adapters, animations, and RecipeViewModel
├── data/     API client, recipe repository, inventory, and shelf-life rules
└── model/    FridgeItem, Recipe, and Ingredient
```

The recipe flow uses a ViewModel and repository. Network requests run through Kotlin coroutines. Recipe candidates are retrieved by ingredient, enriched with their full details, and compared against the inventory using normalized name matching.

## Current scope

- Inventory is stored locally in Room and persists across app restarts.
- Add ingredients manually or look up EAN/UPC product barcodes using the camera, an image, or manual code entry. QR codes are not supported.
- Shelf-life estimates use predefined rules, not a trained model or a food-safety assessment.
- Recipe search and images require an internet connection. Ingredient matching uses text containment and may produce imperfect matches.
- The app opens shopping searches; it does not place orders or modify a shopping cart.

## Dependencies and services

- [TheMealDB](https://www.themealdb.com) — recipe data
- [Retrofit](https://github.com/square/retrofit) and [OkHttp](https://github.com/square/okhttp) — networking
- [Gson](https://github.com/google/gson) — JSON serialization
- [Glide](https://github.com/bumptech/glide) — image loading

## Author

[Theodore Furui Widyatmoko](https://github.com/Qawwai)

