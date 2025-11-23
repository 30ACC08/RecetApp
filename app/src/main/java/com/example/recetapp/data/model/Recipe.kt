package com.example.recetapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String = "",
    val userId: String = "",
    val creatorName: String = "", // <--- NUEVO: Nombre del chef
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

enum class RecipeSource { THEMEALDB, SPOONACULAR, USER }

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

// DICCIONARIO DE TRADUCCIÓN (Igual que antes)
object RecipeTranslations {
    val CATEGORIES = mapOf(
        "Breakfast" to "Desayuno", "Dessert" to "Postres", "Seafood" to "Mariscos",
        "Vegetarian" to "Vegetariano", "Beef" to "Res", "Chicken" to "Pollo",
        "Pasta" to "Pasta", "Pork" to "Cerdo", "Lamb" to "Cordero",
        "Starter" to "Entradas", "Vegan" to "Vegano", "Side" to "Guarnición"
    )
    val AREAS = mapOf(
        "Mexican" to "Mexicana", "Italian" to "Italiana", "American" to "Americana",
        "British" to "Británica", "French" to "Francesa", "Chinese" to "China",
        "Japanese" to "Japonesa", "Indian" to "India", "Spanish" to "Española"
    )
    fun categoryName(original: String): String = CATEGORIES[original] ?: original
    fun areaName(original: String): String = AREAS[original] ?: original
    fun getCategoryKey(spanish: String): String? = CATEGORIES.entries.find { it.value == spanish }?.key
    fun getAreaKey(spanish: String): String? = AREAS.entries.find { it.value == spanish }?.key
}
object RecipeCategories { val ALL = RecipeTranslations.CATEGORIES.keys.toList() }
object RecipeAreas { val ALL = RecipeTranslations.AREAS.keys.toList() }