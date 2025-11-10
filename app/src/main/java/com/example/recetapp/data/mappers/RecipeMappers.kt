// Ruta: app/src/main/java/com/example/recetapp/data/mappers/RecipeMappers.kt
package com.example.recetapp.data.mappers

import com.example.recetapp.data.model.*
import com.example.recetapp.data.network.response.*

// ==================== TheMealDB Mapper ====================

fun MealDbRecipe.toRecipe(): Recipe {
    val ingredients = mutableListOf<Ingredient>()

    // Recopilar ingredientes y medidas
    val ingredientsList = listOf(
        ingredient1, ingredient2, ingredient3, ingredient4, ingredient5,
        ingredient6, ingredient7, ingredient8, ingredient9, ingredient10,
        ingredient11, ingredient12, ingredient13, ingredient14, ingredient15,
        ingredient16, ingredient17, ingredient18, ingredient19, ingredient20
    )

    val measuresList = listOf(
        measure1, measure2, measure3, measure4, measure5,
        measure6, measure7, measure8, measure9, measure10,
        measure11, measure12, measure13, measure14, measure15,
        measure16, measure17, measure18, measure19, measure20
    )

    ingredientsList.forEachIndexed { index, ingredient ->
        if (!ingredient.isNullOrBlank()) {
            val measure = measuresList.getOrNull(index) ?: ""
            ingredients.add(Ingredient(ingredient, measure))
        }
    }

    return Recipe(
        id = id,
        name = name,
        category = category ?: "General",
        area = area ?: "International",
        instructions = instructions ?: "",
        thumbnailUrl = thumbnail ?: "",
        imageUrl = thumbnail ?: "",
        tags = tags?.split(",")?.map { it.trim() } ?: emptyList(),
        ingredients = ingredients,
        videoUrl = youtubeUrl,
        source = RecipeSource.THEMEALDB
    )
}

// ==================== Spoonacular Mapper ====================

fun SpoonacularRecipe.toRecipe(): Recipe {
    return Recipe(
        id = "spoon_$id",
        name = title,
        category = dishTypes?.firstOrNull() ?: "General",
        area = cuisines?.firstOrNull() ?: "International",
        instructions = summary ?: "",
        thumbnailUrl = image ?: "",
        imageUrl = image ?: "",
        tags = (cuisines ?: emptyList()) + (dishTypes ?: emptyList()),
        ingredients = emptyList(),
        videoUrl = null,
        source = RecipeSource.SPOONACULAR,

        readyInMinutes = readyInMinutes,
        servings = servings,
        healthScore = healthScore,
        pricePerServing = pricePerServing,
        cheap = cheap ?: false,
        vegetarian = vegetarian ?: false,
        vegan = vegan ?: false,
        glutenFree = glutenFree ?: false,
        dairyFree = dairyFree ?: false,
        veryHealthy = veryHealthy ?: false,
        sustainable = sustainable ?: false,
        nutrition = nutrition?.let {
            Nutrition(
                calories = it.getCalories() ?: 0.0,
                protein = it.getProtein() ?: 0.0,
                fat = it.getFat() ?: 0.0,
                carbs = it.getCarbs() ?: 0.0
            )
        }
    )
}

fun SpoonacularRecipeDetail.toRecipe(): Recipe {
    val ingredients = extendedIngredients.map {
        Ingredient(it.name, "${it.amount} ${it.unit}")
    }

    return Recipe(
        id = "spoon_$id",
        name = title,
        category = dishTypes.firstOrNull() ?: "General",
        area = cuisines.firstOrNull() ?: "International",
        instructions = instructions ?: summary,
        thumbnailUrl = image ?: "",
        imageUrl = image ?: "",
        tags = cuisines + dishTypes,
        ingredients = ingredients,
        videoUrl = null,
        source = RecipeSource.SPOONACULAR,

        readyInMinutes = readyInMinutes,
        servings = servings,
        healthScore = healthScore,
        pricePerServing = pricePerServing,
        cheap = cheap,
        vegetarian = vegetarian,
        vegan = vegan,
        glutenFree = glutenFree,
        dairyFree = dairyFree,
        veryHealthy = veryHealthy,
        sustainable = sustainable,
        nutrition = nutrition?.let {
            Nutrition(
                calories = it.getCalories() ?: 0.0,
                protein = it.getProtein() ?: 0.0,
                fat = it.getFat() ?: 0.0,
                carbs = it.getCarbs() ?: 0.0
            )
        }
    )
}

// ==================== Helper Functions CON @JvmName ====================

@JvmName("mealDbToRecipeList")
fun List<MealDbRecipe>.toRecipeList(): List<Recipe> = map { it.toRecipe() }

@JvmName("spoonacularToRecipeList")
fun List<SpoonacularRecipe>.toRecipeList(): List<Recipe> = map { it.toRecipe() }

@JvmName("spoonacularDetailToRecipeList")
fun List<SpoonacularRecipeDetail>.toRecipeDetailList(): List<Recipe> = map { it.toRecipe() }