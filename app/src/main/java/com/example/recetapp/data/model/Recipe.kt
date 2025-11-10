package com.example.recetapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String,
    val name: String,
    val category: String,
    val area: String,
    val instructions: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val tags: List<String>,
    val ingredients: List<Ingredient>,
    val videoUrl: String?,
    val source: RecipeSource,

    // Campos de Spoonacular
    val readyInMinutes: Int? = null,
    val servings: Int? = null,
    val healthScore: Double? = null,
    val pricePerServing: Double? = null,
    val cheap: Boolean = false,
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
    val glutenFree: Boolean = false,
    val dairyFree: Boolean = false,
    val veryHealthy: Boolean = false,
    val sustainable: Boolean = false,
    val nutrition: Nutrition? = null
) : Parcelable

@Parcelize
data class Ingredient(
    val name: String,
    val measure: String
) : Parcelable

@Parcelize
data class Nutrition(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
) : Parcelable

enum class RecipeSource {
    THEMEALDB,
    SPOONACULAR
}

object RecipeCategories {
    const val BREAKFAST = "Breakfast"
    const val DESSERT = "Dessert"
    const val SEAFOOD = "Seafood"
    const val VEGETARIAN = "Vegetarian"
    const val BEEF = "Beef"
    const val CHICKEN = "Chicken"
    const val PASTA = "Pasta"
    const val PORK = "Pork"
    const val LAMB = "Lamb"
    const val STARTER = "Starter"
    const val VEGAN = "Vegan"
    const val SIDE = "Side"

    val ALL = listOf(
        BREAKFAST, DESSERT, SEAFOOD, VEGETARIAN, BEEF,
        CHICKEN, PASTA, PORK, LAMB, STARTER, VEGAN, SIDE
    )
}

object RecipeAreas {
    const val MEXICAN = "Mexican"
    const val ITALIAN = "Italian"
    const val AMERICAN = "American"
    const val BRITISH = "British"
    const val FRENCH = "French"
    const val CHINESE = "Chinese"
    const val JAPANESE = "Japanese"
    const val INDIAN = "Indian"
    const val THAI = "Thai"
    const val SPANISH = "Spanish"
    const val GREEK = "Greek"

    val ALL = listOf(
        MEXICAN, ITALIAN, AMERICAN, BRITISH, FRENCH,
        CHINESE, JAPANESE, INDIAN, THAI, SPANISH, GREEK
    )
}

data class RecipeFilter(
    val query: String = "",
    val category: String? = null,
    val area: String? = null,
    val source: RecipeSource? = null,
    val maxReadyTime: Int? = null,
    val minHealthScore: Int? = null,
    val maxCalories: Int? = null,
    val vegetarian: Boolean? = null,
    val vegan: Boolean? = null,
    val glutenFree: Boolean? = null,
    val dairyFree: Boolean? = null,
    val cheap: Boolean? = null,
    val sortBy: SortOption = SortOption.POPULARITY
)

enum class SortOption {
    POPULARITY,
    TIME,
    HEALTH_SCORE,
    CALORIES,
    PRICE
}