package com.example.recetapp.data.repository

import com.example.recetapp.data.mappers.*
import com.example.recetapp.data.model.*
import com.example.recetapp.data.network.ApiManager
import com.example.recetapp.data.network.SpoonacularApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RecipeRepository {

    private val mealDbApi = ApiManager.mealDbApi
    private val spoonacularApi = ApiManager.spoonacularApi

    // ==================== Búsqueda Combinada ====================

    suspend fun searchRecipes(filter: RecipeFilter): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()

            // Determinar qué APIs usar según los filtros
            val useMealDb = shouldUseMealDb(filter)
            val useSpoonacular = shouldUseSpoonacular(filter)

            coroutineScope {
                if (useMealDb) {
                    val mealDbDeferred = async { searchMealDb(filter) }
                    mealDbDeferred.await().getOrNull()?.let { recipes.addAll(it) }
                }

                if (useSpoonacular) {
                    val spoonDeferred = async { searchSpoonacular(filter) }
                    spoonDeferred.await().getOrNull()?.let { recipes.addAll(it) }
                }
            }

            // Aplicar ordenamiento
            val sorted = applySorting(recipes, filter.sortBy)

            Result.success(sorted)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun shouldUseMealDb(filter: RecipeFilter): Boolean {
        // MealDB no tiene filtros avanzados, solo usar si es búsqueda básica o sin fuente específica
        return filter.source == RecipeSource.THEMEALDB ||
                (filter.source == null && !hasAdvancedFilters(filter))
    }

    private fun shouldUseSpoonacular(filter: RecipeFilter): Boolean {
        // Spoonacular si tiene filtros avanzados o es la fuente especificada
        return filter.source == RecipeSource.SPOONACULAR ||
                (filter.source == null && hasAdvancedFilters(filter)) ||
                filter.source == null
    }

    private fun hasAdvancedFilters(filter: RecipeFilter): Boolean {
        return filter.maxReadyTime != null ||
                filter.minHealthScore != null ||
                filter.maxCalories != null ||
                filter.vegetarian != null ||
                filter.vegan != null ||
                filter.glutenFree != null ||
                filter.dairyFree != null
    }

    // ==================== TheMealDB ====================

    private suspend fun searchMealDb(filter: RecipeFilter): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()

            // Búsqueda por query
            if (filter.query.isNotBlank()) {
                val response = mealDbApi.searchRecipes(filter.query)
                if (response.isSuccessful) {
                    response.body()?.meals?.let {
                        recipes.addAll(it.toRecipeList())
                    }
                }
            }

            // Búsqueda por categoría
            if (filter.category != null) {
                val response = mealDbApi.getRecipesByCategory(filter.category)
                if (response.isSuccessful) {
                    response.body()?.meals?.let { meals ->
                        // MealDB devuelve recetas simplificadas, necesitamos obtener detalles
                        meals.forEach { meal ->
                            val detailResponse = mealDbApi.getRecipeById(meal.id)
                            detailResponse.body()?.meals?.firstOrNull()?.let {
                                recipes.add(it.toRecipe())
                            }
                        }
                    }
                }
            }

            // Búsqueda por área
            if (filter.area != null) {
                val response = mealDbApi.getRecipesByArea(filter.area)
                if (response.isSuccessful) {
                    response.body()?.meals?.let { meals ->
                        meals.forEach { meal ->
                            val detailResponse = mealDbApi.getRecipeById(meal.id)
                            detailResponse.body()?.meals?.firstOrNull()?.let {
                                recipes.add(it.toRecipe())
                            }
                        }
                    }
                }
            }

            // Si no hay filtros, obtener aleatorias
            if (recipes.isEmpty() && filter.query.isBlank() && filter.category == null && filter.area == null) {
                repeat(10) {
                    val response = mealDbApi.getRandomRecipe()
                    response.body()?.meals?.firstOrNull()?.let {
                        recipes.add(it.toRecipe())
                    }
                }
            }

            Result.success(recipes.distinctBy { it.id })

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Spoonacular ====================

    private suspend fun searchSpoonacular(filter: RecipeFilter): Result<List<Recipe>> {
        return try {
            val diet = when {
                filter.vegetarian == true -> "vegetarian"
                filter.vegan == true -> "vegan"
                filter.glutenFree == true -> "gluten free"
                else -> null
            }

            val sort = when (filter.sortBy) {
                SortOption.POPULARITY -> "popularity"
                SortOption.TIME -> "time"
                SortOption.HEALTH_SCORE -> "healthiness"
                SortOption.CALORIES -> "calories"
                SortOption.PRICE -> "price"
            }

            val response = spoonacularApi.searchRecipes(
                apiKey = SpoonacularApi.API_KEY,
                query = filter.query.takeIf { it.isNotBlank() },
                cuisine = filter.area,
                diet = diet,
                type = filter.category?.lowercase(),
                maxReadyTime = filter.maxReadyTime,
                minHealthScore = filter.minHealthScore,
                maxCalories = filter.maxCalories,
                number = 20,
                sort = sort
            )

            if (response.isSuccessful) {
                val recipes = response.body()?.results?.toRecipeList() ?: emptyList()
                Result.success(recipes)
            } else {
                Result.failure(Exception("Error en Spoonacular: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Detalle de Receta ====================

    suspend fun getRecipeDetail(recipeId: String, source: RecipeSource): Result<Recipe> {
        return try {
            when (source) {
                RecipeSource.THEMEALDB -> {
                    val response = mealDbApi.getRecipeById(recipeId)
                    if (response.isSuccessful) {
                        response.body()?.meals?.firstOrNull()?.let {
                            Result.success(it.toRecipe())
                        } ?: Result.failure(Exception("Receta no encontrada"))
                    } else {
                        Result.failure(Exception("Error al obtener receta"))
                    }
                }

                RecipeSource.SPOONACULAR -> {
                    val id = recipeId.removePrefix("spoon_").toInt()
                    val response = spoonacularApi.getRecipeById(id, SpoonacularApi.API_KEY)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            Result.success(it.toRecipe())
                        } ?: Result.failure(Exception("Receta no encontrada"))
                    } else {
                        Result.failure(Exception("Error al obtener receta"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Recetas Aleatorias ====================

    suspend fun getRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()

            coroutineScope {
                // MealDB: obtener recetas aleatorias
                val mealDbDeferred = async {
                    val mealDbRecipes = mutableListOf<Recipe>()
                    repeat(count / 2) {
                        val response = mealDbApi.getRandomRecipe()
                        response.body()?.meals?.firstOrNull()?.let {
                            mealDbRecipes.add(it.toRecipe())
                        }
                    }
                    mealDbRecipes
                }

                // Spoonacular: obtener recetas aleatorias
                val spoonDeferred = async {
                    val response = spoonacularApi.getRandomRecipes(
                        SpoonacularApi.API_KEY,
                        count / 2
                    )
                    response.body()?.recipes?.toRecipeDetailList() ?: emptyList()
                }

                recipes.addAll(mealDbDeferred.await())
                recipes.addAll(spoonDeferred.await())
            }

            Result.success(recipes.shuffled())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Categorías ====================

    suspend fun getCategories(): Result<List<String>> {
        return try {
            val response = mealDbApi.getCategories()
            if (response.isSuccessful) {
                val categories = response.body()?.categories?.map { it.name } ?: emptyList()
                Result.success(categories)
            } else {
                Result.failure(Exception("Error al obtener categorías"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Helpers ====================

    private fun applySorting(recipes: List<Recipe>, sortBy: SortOption): List<Recipe> {
        return when (sortBy) {
            SortOption.POPULARITY -> recipes // Ya vienen ordenadas
            SortOption.TIME -> recipes.sortedBy { it.readyInMinutes ?: Int.MAX_VALUE }
            SortOption.HEALTH_SCORE -> recipes.sortedByDescending { it.healthScore ?: 0.0 }
            SortOption.CALORIES -> recipes.sortedBy { it.nutrition?.calories ?: Double.MAX_VALUE }
            SortOption.PRICE -> recipes.sortedBy { it.pricePerServing ?: Double.MAX_VALUE }
        }
    }
}