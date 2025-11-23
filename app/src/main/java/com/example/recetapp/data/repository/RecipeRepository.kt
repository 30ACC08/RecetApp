package com.example.recetapp.data.repository

import android.net.Uri
import android.util.Log
import com.example.recetapp.data.mappers.*
import com.example.recetapp.data.model.*
import com.example.recetapp.data.network.ApiManager
import com.example.recetapp.data.network.SpoonacularApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RecipeRepository {

    private val mealDbApi = ApiManager.mealDbApi
    private val spoonacularApi = ApiManager.spoonacularApi
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // === NUEVO: Helper para obtener nombre ===
    suspend fun getCurrentUserName(): String {
        val uid = auth.currentUser?.uid ?: return "Anónimo"
        return try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            doc.getString("nombre") ?: "Anónimo"
        } catch (e: Exception) { "Anónimo" }
    }

    // === CRUD ===
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val filename = UUID.randomUUID().toString()
            val ref = storage.reference.child("recipe_images/$filename")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createRecipe(recipe: Recipe): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No logueado"))
            val recipeWithUser = recipe.copy(userId = userId)

            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipe.id)
                .set(recipeWithUser).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No logueado"))
            if (recipe.userId != userId) return Result.failure(Exception("No tienes permiso"))

            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipe.id)
                .set(recipe).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No logueado"))
            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipeId)
                .delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUserRecipes(): Result<List<Recipe>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
            val snapshot = firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").get().await()
            Result.success(snapshot.toObjects(Recipe::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    // === BÚSQUEDA GLOBAL ===
    suspend fun searchRecipes(filter: RecipeFilter): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()
            coroutineScope {
                // 1. Firestore (Usuarios)
                val firestoreDeferred = async {
                    try {
                        val query = firestore.collectionGroup("mis_recetas")
                        val snapshot = query.get().await()
                        val allUserRecipes = snapshot.toObjects(Recipe::class.java)

                        allUserRecipes.filter { r ->
                            val matchesQuery = filter.query.isBlank() || r.name.contains(filter.query, ignoreCase = true)
                            val matchesCategory = filter.category == null || r.category.equals(filter.category, ignoreCase = true)
                            matchesQuery && matchesCategory
                        }
                    } catch (e: Exception) { emptyList<Recipe>() }
                }

                // 2. MealDB
                val mealDbDeferred = async {
                    if (filter.source != RecipeSource.SPOONACULAR) {
                        val list = mutableListOf<Recipe>()
                        if (filter.query.isNotBlank()) {
                            mealDbApi.searchRecipes(filter.query).body()?.meals?.let { list.addAll(it.toRecipeList()) }
                        } else if (filter.category != null) {
                            mealDbApi.getRecipesByCategory(filter.category).body()?.meals?.forEach {
                                mealDbApi.getRecipeById(it.id).body()?.meals?.firstOrNull()?.let { d -> list.add(d.toRecipe()) }
                            }
                        } else {
                            repeat(3) { mealDbApi.getRandomRecipe().body()?.meals?.firstOrNull()?.let { list.add(it.toRecipe()) } }
                        }
                        list
                    } else emptyList()
                }

                // 3. Spoonacular
                val spoonDeferred = async {
                    if (filter.source != RecipeSource.THEMEALDB) {
                        val diet = if(filter.vegetarian == true) "vegetarian" else null
                        spoonacularApi.searchRecipes(
                            SpoonacularApi.API_KEY,
                            query = filter.query.ifBlank { null },
                            type = filter.category?.lowercase(),
                            diet = diet, number = 10
                        ).body()?.results?.toRecipeList() ?: emptyList()
                    } else emptyList()
                }

                recipes.addAll(firestoreDeferred.await())
                recipes.addAll(mealDbDeferred.await())
                recipes.addAll(spoonDeferred.await())
            }
            Result.success(recipes.distinctBy { it.id })
        } catch (e: Exception) { Result.failure(e) }
    }

    // ... Resto de funciones (Favoritos, etc.) se mantienen igual ...
    suspend fun isFavorite(recipeId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("usuarios").document(userId).collection("favoritos").document(recipeId).get().await()
            doc.exists()
        } catch (e: Exception) { false }
    }
    fun addFavorite(recipe: Recipe) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(userId).collection("favoritos").document(recipe.id).set(recipe)
    }
    fun removeFavorite(recipeId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(userId).collection("favoritos").document(recipeId).delete()
    }
    suspend fun getFavorites(): Result<List<Recipe>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
            val snapshot = firestore.collection("usuarios").document(userId).collection("favoritos").get().await()
            Result.success(snapshot.toObjects(Recipe::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getRandomRecipes(count: Int): Result<List<Recipe>> {
        val list = mutableListOf<Recipe>()
        try {
            coroutineScope {
                val t1 = async { repeat(count/2) { mealDbApi.getRandomRecipe().body()?.meals?.firstOrNull()?.let { list.add(it.toRecipe()) } } }
                val t2 = async { spoonacularApi.getRandomRecipes(SpoonacularApi.API_KEY, count/2).body()?.recipes?.toRecipeDetailList()?.let { list.addAll(it) } }
                t1.await(); t2.await()
            }
            return Result.success(list)
        } catch(e: Exception) { return Result.failure(e) }
    }
    suspend fun getRecipeDetail(id: String, source: RecipeSource): Result<Recipe> {
        return try {
            if (source == RecipeSource.THEMEALDB) {
                val r = mealDbApi.getRecipeById(id).body()?.meals?.firstOrNull()
                if (r != null) Result.success(r.toRecipe()) else Result.failure(Exception("No encontrada"))
            } else if (source == RecipeSource.SPOONACULAR) {
                val r = spoonacularApi.getRecipeById(id.removePrefix("spoon_").toInt(), SpoonacularApi.API_KEY).body()
                if (r != null) Result.success(r.toRecipe()) else Result.failure(Exception("No encontrada"))
            } else {
                // Para recetas de usuario, el objeto ya debería venir completo desde la lista o Firestore
                Result.failure(Exception("Cargado localmente"))
            }
        } catch(e: Exception) { Result.failure(e) }
    }
    suspend fun getCategories() = try {
        Result.success(mealDbApi.getCategories().body()?.categories?.map { it.name } ?: emptyList())
    } catch(e:Exception) { Result.failure(e) }
}