package com.example.recetapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val area: String = "",
    val instructions: String = "",
    val thumbnailUrl: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val videoUrl: String? = null,
    val source: RecipeSource = RecipeSource.THEMEALDB,

    // Campos opcionales con valores por defecto
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
    val name: String = "",
    val measure: String = ""
) : Parcelable

@Parcelize
data class Nutrition(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0
) : Parcelable

enum class RecipeSource {
    THEMEALDB,
    SPOONACULAR
}

object RecipeCategories {
    val ALL = listOf("Breakfast", "Dessert", "Seafood", "Vegetarian", "Beef", "Chicken", "Pasta", "Pork", "Lamb", "Starter", "Vegan", "Side")
}

object RecipeAreas {
    val ALL = listOf("Mexican", "Italian", "American", "British", "French", "Chinese", "Japanese", "Indian", "Thai", "Spanish", "Greek")
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

enum class SortOption { POPULARITY, TIME, HEALTH_SCORE, CALORIES, PRICE }