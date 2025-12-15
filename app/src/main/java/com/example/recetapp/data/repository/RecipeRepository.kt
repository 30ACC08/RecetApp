package com.example.recetapp.data.repository

import android.net.Uri
import com.example.recetapp.data.mappers.*
import com.example.recetapp.data.model.*
import com.example.recetapp.data.network.ApiManager
import com.example.recetapp.data.network.SpoonacularApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class RecipeRepository {

    private val mealDbApi = ApiManager.mealDbApi
    private val spoonacularApi = ApiManager.spoonacularApi
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // === Helper ===
    suspend fun getCurrentUserName(): String = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext "Anónimo"
        try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            doc.getString("nombre") ?: "Anónimo"
        } catch (e: Exception) { "Anónimo" }
    }

    // === CRUD Recetas ===
    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val filename = UUID.randomUUID().toString()
            val ref = storage.reference.child("recipe_images/$filename")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createRecipe(recipe: Recipe): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("No logueado"))
            val recipeWithUser = recipe.copy(userId = userId)
            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipe.id)
                .set(recipeWithUser).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("No logueado"))
            if (recipe.userId != userId) return@withContext Result.failure(Exception("No tienes permiso"))
            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipe.id)
                .set(recipe).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("No logueado"))
            firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").document(recipeId)
                .delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUserRecipes(targetUserId: String? = null): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        try {
            val userId = targetUserId ?: auth.currentUser?.uid ?: return@withContext Result.success(emptyList())
            val snapshot = firestore.collection("usuarios").document(userId)
                .collection("mis_recetas").get().await()
            Result.success(snapshot.toObjects(Recipe::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    // === BUSCAR RECETA POR ID ===
    suspend fun getRecipeById(id: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            when {
                id.startsWith("spoon_") -> {
                    val spoonId = id.removePrefix("spoon_").toIntOrNull() ?: return@withContext Result.failure(Exception("ID inválido"))
                    val response = spoonacularApi.getRecipeById(spoonId, SpoonacularApi.API_KEY)
                    val body = response.body()
                    if (body != null) Result.success(body.toRecipe()) else Result.failure(Exception("Error API Spoonacular"))
                }
                id.all { it.isDigit() } -> {
                    val response = mealDbApi.getRecipeById(id)
                    val meal = response.body()?.meals?.firstOrNull()
                    if (meal != null) Result.success(meal.toRecipe()) else Result.failure(Exception("Error API MealDB"))
                }
                else -> {
                    val snapshot = firestore.collectionGroup("mis_recetas")
                        .whereEqualTo(FieldPath.documentId(), id)
                        .get().await()
                    val recipe = snapshot.documents.firstOrNull()?.toObject(Recipe::class.java)
                        ?: firestore.collectionGroup("mis_recetas").whereEqualTo("id", id).get().await().documents.firstOrNull()?.toObject(Recipe::class.java)
                    if (recipe != null) Result.success(recipe) else Result.failure(Exception("Receta no encontrada"))
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // === BÚSQUEDA ===
    suspend fun searchRecipes(filter: RecipeFilter): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        try {
            val recipes = mutableListOf<Recipe>()
            coroutineScope {
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

    // === FAVORITOS & LIKES ===
    suspend fun isFavorite(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext false
        try {
            val doc = firestore.collection("usuarios").document(userId).collection("favoritos").document(recipeId).get().await()
            doc.exists()
        } catch (e: Exception) { false }
    }

    suspend fun addFavorite(recipe: Recipe) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        try {
            firestore.collection("usuarios").document(userId).collection("favoritos").document(recipe.id).set(recipe).await()
            if (recipe.userId.isNotEmpty() && recipe.userId != userId) {
                sendNotification(recipe.userId, NotificationType.LIKE, "¡Nuevo Like!", "A alguien le gustó tu receta ${recipe.name}", recipe.id)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun removeFavorite(recipeId: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        try {
            firestore.collection("usuarios").document(userId).collection("favoritos").document(recipeId).delete().await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun getFavorites(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.success(emptyList())
            val snapshot = firestore.collection("usuarios").document(userId).collection("favoritos").get().await()
            Result.success(snapshot.toObjects(Recipe::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    // === APIS EXTERNAS ===
    suspend fun getRandomRecipes(count: Int): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Recipe>()
        try {
            coroutineScope {
                val t1 = async { repeat(count/2) { mealDbApi.getRandomRecipe().body()?.meals?.firstOrNull()?.let { list.add(it.toRecipe()) } } }
                val t2 = async { spoonacularApi.getRandomRecipes(SpoonacularApi.API_KEY, count/2).body()?.recipes?.toRecipeDetailList()?.let { list.addAll(it) } }
                t1.await(); t2.await()
            }
            Result.success(list)
        } catch(e: Exception) { Result.failure(e) }
    }

    suspend fun getCategories() = withContext(Dispatchers.IO) {
        try {
            Result.success(mealDbApi.getCategories().body()?.categories?.map { it.name } ?: emptyList())
        } catch(e:Exception) { Result.failure(e) }
    }

    // ==================== RESEÑAS / REVIEWS ====================
    suspend fun addReview(recipeId: String, recipeName: String, recipeImageUrl: String, rating: Float, comment: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(Exception("Debes iniciar sesión"))
            val userDoc = firestore.collection("usuarios").document(user.uid).get().await()
            val userName = userDoc.getString("nombre") ?: "Anónimo"
            val userPhoto = userDoc.getString("photoUrl") ?: ""

            val reviewId = UUID.randomUUID().toString()
            val review = Review(reviewId, recipeId, recipeName, recipeImageUrl, user.uid, userName, userPhoto, rating, comment, java.util.Date())

            firestore.collection("recetas_data").document(recipeId)
                .collection("reviews").document(reviewId)
                .set(review).await()

            val recipeDocs = firestore.collectionGroup("mis_recetas").whereEqualTo("id", recipeId).get().await()
            val ownerId = recipeDocs.documents.firstOrNull()?.getString("userId")

            if (ownerId != null) {
                sendNotification(ownerId, NotificationType.REVIEW, "Nueva reseña", "$userName comentó en $recipeName: \"$comment\"", recipeId)
            }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUserReviews(targetUserId: String? = null): Result<List<Review>> = withContext(Dispatchers.IO) {
        try {
            val userId = targetUserId ?: auth.currentUser?.uid ?: return@withContext Result.failure(Exception("No logueado"))
            val snapshot = firestore.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Review::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteReview(recipeId: String, reviewId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("recetas_data").document(recipeId)
                .collection("reviews").document(reviewId)
                .delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateReview(recipeId: String, reviewId: String, newRating: Float, newComment: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf("rating" to newRating, "comment" to newComment, "timestamp" to java.util.Date())
            firestore.collection("recetas_data").document(recipeId)
                .collection("reviews").document(reviewId)
                .update(updates).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getReviews(recipeId: String): Result<List<Review>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("recetas_data").document(recipeId)
                .collection("reviews")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Review::class.java))
        } catch (e: Exception) { Result.success(emptyList()) }
    }

    private suspend fun sendNotification(targetUserId: String, type: NotificationType, title: String, message: String, relatedId: String) {
        try {
            val currentUser = auth.currentUser ?: return
            if (currentUser.uid == targetUserId) return
            val userDoc = firestore.collection("usuarios").document(currentUser.uid).get().await()
            val userName = userDoc.getString("nombre") ?: "Alguien"
            val userPhoto = userDoc.getString("photoUrl") ?: ""
            val notifId = UUID.randomUUID().toString()
            val notification = Notification(notifId, type, title, message, currentUser.uid, userName, userPhoto, relatedId, false, java.util.Date())
            firestore.collection("usuarios").document(targetUserId).collection("notificaciones").document(notifId).set(notification).await()
        } catch (e: Exception) { e.printStackTrace() }
    }
}