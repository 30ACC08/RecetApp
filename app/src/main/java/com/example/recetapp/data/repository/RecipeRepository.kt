package com.example.recetapp.data.repository

import android.util.Log
import com.example.recetapp.data.mappers.*
import com.example.recetapp.data.model.*
import com.example.recetapp.data.network.ApiManager
import com.example.recetapp.data.network.SpoonacularApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class RecipeRepository {

    private val mealDbApi = ApiManager.mealDbApi
    private val spoonacularApi = ApiManager.spoonacularApi
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ==================== GESTIÓN DE FAVORITOS ====================

    suspend fun isFavorite(recipeId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            // Consulta rápida para saber si existe
            val doc = firestore.collection("usuarios").document(userId)
                .collection("favoritos").document(recipeId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun addFavorite(recipe: Recipe) {
        val userId = auth.currentUser?.uid ?: return
        // Guardar sin esperar respuesta (background)
        firestore.collection("usuarios").document(userId)
            .collection("favoritos").document(recipe.id)
            .set(recipe)
    }

    fun removeFavorite(recipeId: String) {
        val userId = auth.currentUser?.uid ?: return
        // Borrar sin esperar respuesta (background)
        firestore.collection("usuarios").document(userId)
            .collection("favoritos").document(recipeId)
            .delete()
    }

    suspend fun getFavorites(): Result<List<Recipe>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
            val snapshot = firestore.collection("usuarios").document(userId)
                .collection("favoritos").get().await()
            val recipes = snapshot.toObjects(Recipe::class.java)
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== BÚSQUEDA (Mantenemos funcionalidad) ====================

    suspend fun searchRecipes(filter: RecipeFilter): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()
            coroutineScope {
                // Cargar MealDB si aplica
                if (filter.source != RecipeSource.SPOONACULAR) {
                    val mealDbDeferred = async {
                        val list = mutableListOf<Recipe>()
                        if (filter.query.isNotBlank()) {
                            mealDbApi.searchRecipes(filter.query).body()?.meals?.let { list.addAll(it.toRecipeList()) }
                        } else if (filter.category != null) {
                            mealDbApi.getRecipesByCategory(filter.category).body()?.meals?.forEach {
                                mealDbApi.getRecipeById(it.id).body()?.meals?.firstOrNull()?.let { d -> list.add(d.toRecipe()) }
                            }
                        } else if (filter.area != null) {
                            mealDbApi.getRecipesByArea(filter.area).body()?.meals?.forEach {
                                mealDbApi.getRecipeById(it.id).body()?.meals?.firstOrNull()?.let { d -> list.add(d.toRecipe()) }
                            }
                        } else {
                            repeat(5) { mealDbApi.getRandomRecipe().body()?.meals?.firstOrNull()?.let { list.add(it.toRecipe()) } }
                        }
                        list
                    }
                    recipes.addAll(mealDbDeferred.await())
                }

                // Cargar Spoonacular si aplica
                if (filter.source != RecipeSource.THEMEALDB) {
                    val spoonDeferred = async {
                        spoonacularApi.searchRecipes(
                            SpoonacularApi.API_KEY,
                            query = filter.query.ifBlank { null },
                            type = filter.category?.lowercase(),
                            cuisine = filter.area,
                            number = 10
                        ).body()?.results?.toRecipeList() ?: emptyList()
                    }
                    recipes.addAll(spoonDeferred.await())
                }
            }
            Result.success(recipes.distinctBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomRecipes(count: Int): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()
            coroutineScope {
                val t1 = async {
                    val l = mutableListOf<Recipe>()
                    repeat(count/2) { mealDbApi.getRandomRecipe().body()?.meals?.firstOrNull()?.let { l.add(it.toRecipe()) } }
                    l
                }
                val t2 = async {
                    spoonacularApi.getRandomRecipes(SpoonacularApi.API_KEY, count/2).body()?.recipes?.toRecipeDetailList() ?: emptyList()
                }
                recipes.addAll(t1.await())
                recipes.addAll(t2.await())
            }
            Result.success(recipes)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRecipeDetail(id: String, source: RecipeSource): Result<Recipe> {
        return try {
            if (source == RecipeSource.THEMEALDB) {
                val r = mealDbApi.getRecipeById(id).body()?.meals?.firstOrNull()
                if (r != null) Result.success(r.toRecipe()) else Result.failure(Exception("No encontrada"))
            } else {
                val r = spoonacularApi.getRecipeById(id.removePrefix("spoon_").toInt(), SpoonacularApi.API_KEY).body()
                if (r != null) Result.success(r.toRecipe()) else Result.failure(Exception("No encontrada"))
            }
        } catch(e: Exception) { Result.failure(e) }
    }

    suspend fun getCategories() = try {
        val r = mealDbApi.getCategories()
        Result.success(r.body()?.categories?.map { it.name } ?: emptyList())
    } catch(e:Exception) { Result.failure(e) }
}