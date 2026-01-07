package com.example.recetapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.*
import com.example.recetapp.data.repository.RecipeRepository
import com.example.recetapp.data.repository.RecipeTranslator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

data class HomeContent(
    val featured: List<Recipe>,
    val breakfast: List<Recipe>,
    val healthy: List<Recipe>
)

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository()
    private val translator = RecipeTranslator()
    private var searchJob: Job? = null

    // === ESTADOS UI ===
    private val _homeState = MutableLiveData<UiState<HomeContent>>()
    val homeState: LiveData<UiState<HomeContent>> = _homeState

    private val _searchState = MutableLiveData<UiState<List<Recipe>>>()
    val searchState: LiveData<UiState<List<Recipe>>> = _searchState

    private val _favoritesState = MutableLiveData<UiState<List<Recipe>>>()
    val favoritesState: LiveData<UiState<List<Recipe>>> = _favoritesState

    private val _myRecipesState = MutableLiveData<UiState<List<Recipe>>>()
    val myRecipesState: LiveData<UiState<List<Recipe>>> = _myRecipesState

    private val _currentFilter = MutableLiveData(RecipeFilter())
    val currentFilter: LiveData<RecipeFilter> = _currentFilter

    private val _selectedRecipe = MutableLiveData<Recipe>()
    val selectedRecipe: LiveData<Recipe> = _selectedRecipe

    private val _isRecipeFavorite = MutableLiveData<Boolean?>()
    val isRecipeFavorite: LiveData<Boolean?> = _isRecipeFavorite

    // Cache para traducción
    private var originalRecipeCache: Recipe? = null
    private var translatedRecipeCache: Recipe? = null

    private val _isTranslated = MutableLiveData(false)
    val isTranslated: LiveData<Boolean> = _isTranslated

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // === RESEÑAS ===
    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    private val _userReviews = MutableLiveData<List<Review>>()
    val userReviews: LiveData<List<Review>> = _userReviews

    private val _reviewUploadResult = MutableLiveData<Result<Boolean>?>()
    val reviewUploadResult: LiveData<Result<Boolean>?> = _reviewUploadResult

    private val _reviewActionState = MutableLiveData<Result<String>>()
    val reviewActionState: LiveData<Result<String>> = _reviewActionState

    private val _publicUserRecipes = MutableLiveData<List<Recipe>>()
    val publicUserRecipes: LiveData<List<Recipe>> = _publicUserRecipes
    private val _publicUserReviews = MutableLiveData<List<Review>>()
    val publicUserReviews: LiveData<List<Review>> = _publicUserReviews

    private val _isLoadingAction = MutableLiveData<Boolean>()
    val isLoadingAction: LiveData<Boolean> = _isLoadingAction

    init {
        viewModelScope.launch {
            try { translator.prepareModel() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // === HOME ===
    fun loadHomeContent() {
        _homeState.value = UiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val featuredDeferred = async { repository.getRandomRecipes(10).getOrDefault(emptyList()) }
                    val breakfastDeferred = async { repository.searchRecipes(RecipeFilter(category = "Breakfast")).getOrDefault(emptyList()) }
                    val healthyDeferred = async { repository.searchRecipes(RecipeFilter(vegetarian = true)).getOrDefault(emptyList()) }

                    val content = HomeContent(
                        featured = featuredDeferred.await(),
                        breakfast = breakfastDeferred.await(),
                        healthy = healthyDeferred.await()
                    )
                    _homeState.value = UiState.Success(content)
                }
            } catch (e: Exception) {
                _homeState.value = UiState.Error("Tuvimos problemas conectando con la cocina. Revisa tu internet.")
            }
        }
    }

    // === BÚSQUEDA ===
    fun searchRecipesDebounced(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query.length >= 3) performSearch(query)
        }
    }

    fun searchRecipes(query: String) { viewModelScope.launch { performSearch(query) } }

    private suspend fun performSearch(query: String) {
        _searchState.value = UiState.Loading
        val translatedQuery = try {
            RecipeTranslations.getCategoryKey(query)
                ?: RecipeTranslations.getAreaKey(query)
                ?: query
        } catch (e: Exception) { query }

        val filter = _currentFilter.value?.copy(query = translatedQuery) ?: RecipeFilter(query = translatedQuery)
        _currentFilter.value = filter

        repository.searchRecipes(filter)
            .onSuccess {
                if (it.isEmpty()) _searchState.value = UiState.Empty
                else _searchState.value = UiState.Success(it)
            }
            .onFailure { _searchState.value = UiState.Error("No pudimos realizar la búsqueda. Intenta de nuevo.") }
    }

    fun applyFilter(f: RecipeFilter) {
        _currentFilter.value = f
        viewModelScope.launch { performSearch(f.query) }
    }

    fun clearFilters() { applyFilter(RecipeFilter()) }

    fun toggleCategory(c: String) { applyFilter(_currentFilter.value!!.copy(category = if(_currentFilter.value?.category == c) null else c)) }
    fun toggleVegetarian() { applyFilter(_currentFilter.value!!.copy(vegetarian = if(_currentFilter.value?.vegetarian == true) null else true)) }
    fun toggleVegan() { applyFilter(_currentFilter.value!!.copy(vegan = if(_currentFilter.value?.vegan == true) null else true)) }

    // === FAVORITOS ===
    fun loadFavorites() {
        _favoritesState.value = UiState.Loading
        viewModelScope.launch {
            repository.getFavorites()
                .onSuccess {
                    if (it.isEmpty()) _favoritesState.value = UiState.Empty
                    else _favoritesState.value = UiState.Success(it)
                }
                .onFailure { _favoritesState.value = UiState.Error("No pudimos cargar tus favoritos.") }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            _toastMessage.value = "Inicia sesión para guardar tus recetas favoritas."
            return
        }

        viewModelScope.launch {
            val isCurrentlyFav = repository.isFavorite(recipe.id)
            if (isCurrentlyFav) {
                repository.removeFavorite(recipe.id)
                _isRecipeFavorite.value = false
                _toastMessage.value = "Receta eliminada de favoritos."
            } else {
                repository.addFavorite(recipe)
                _isRecipeFavorite.value = true
                _toastMessage.value = "¡Receta guardada en favoritos!"
            }
            if (_favoritesState.value is UiState.Success || _favoritesState.value is UiState.Empty) {
                loadFavorites()
            }
        }
    }

    fun checkIfFavorite(recipeId: String) {
        viewModelScope.launch { _isRecipeFavorite.value = repository.isFavorite(recipeId) }
    }

    fun clearToastMessage() { _toastMessage.value = null }

    // === MIS RECETAS ===
    fun loadMyRecipes() {
        _myRecipesState.value = UiState.Loading
        viewModelScope.launch {
            repository.getUserRecipes()
                .onSuccess {
                    if (it.isEmpty()) _myRecipesState.value = UiState.Empty
                    else _myRecipesState.value = UiState.Success(it)
                }
                .onFailure { _myRecipesState.value = UiState.Error("No pudimos abrir tu recetario personal.") }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
                .onSuccess { loadMyRecipes(); _toastMessage.value = "Receta eliminada correctamente." }
        }
    }

    // === DETALLES ===
    fun setSelectedRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                val formatted = try { translator.formatRecipeInstructions(recipe) } catch (e: Exception) { recipe }
                originalRecipeCache = formatted
                translatedRecipeCache = null
                _selectedRecipe.value = formatted
                _isTranslated.value = false
                _isRecipeFavorite.value = null
                _reviews.value = emptyList()
                _reviewUploadResult.value = null

                if (formatted.instructions.isBlank() && formatted.ingredients.isEmpty()) {
                    loadFullRecipeDetails(formatted.id)
                }
            } catch (e: Exception) {
                _selectedRecipe.value = recipe
            }
        }
    }

    fun loadFullRecipeDetails(recipeId: String) {
        _isLoadingAction.value = true
        viewModelScope.launch {
            repository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    val formatted = try { translator.formatRecipeInstructions(recipe) } catch(e:Exception) { recipe }
                    originalRecipeCache = formatted
                    translatedRecipeCache = null
                    _selectedRecipe.value = formatted
                }
                .onFailure { e ->
                    _toastMessage.value = "Mostrando información básica (Modo sin conexión o límite alcanzado)."
                }
            _isLoadingAction.value = false
        }
    }

    // === CORRECCIÓN AQUÍ: Traductor Seguro y Limpio ===
    fun toggleTranslation() {
        if (_isTranslated.value == true) {
            // Volver al original
            originalRecipeCache?.let { _selectedRecipe.value = it }
            _isTranslated.value = false
        } else {
            // Usamos 'let' para acceder de forma segura a la caché si existe
            translatedRecipeCache?.let { translated ->
                _selectedRecipe.value = translated
                _isTranslated.value = true
            } ?: run {
                // Si no existe (es null), ejecutamos la traducción
                _isLoadingAction.value = true
                viewModelScope.launch {
                    try {
                        originalRecipeCache?.let { original ->
                            val trans = translator.translateRecipe(original)
                            translatedRecipeCache = trans
                            _selectedRecipe.value = trans
                            _isTranslated.value = true
                        }
                    } catch (e: Exception) {
                        _toastMessage.value = "No pudimos traducir la receta. Verifica tu conexión."
                    }
                    _isLoadingAction.value = false
                }
            }
        }
    }

    // === RESEÑAS ===
    fun loadReviews(recipeId: String) {
        viewModelScope.launch {
            repository.getReviews(recipeId).onSuccess { list -> _reviews.value = list }
        }
    }

    fun submitReview(recipeId: String, rating: Float, comment: String) {
        if (rating == 0f) return
        val recipeName = _selectedRecipe.value?.name ?: "Receta"
        val recipeImage = _selectedRecipe.value?.imageUrl ?: ""
        val currentUser = FirebaseAuth.getInstance().currentUser

        _isLoadingAction.value = true
        viewModelScope.launch {
            val result = repository.addReview(recipeId, recipeName, recipeImage, rating, comment)
            _reviewUploadResult.value = result

            if (result.isSuccess) {
                val newReview = Review(
                    id = "temp_${System.currentTimeMillis()}",
                    recipeId = recipeId,
                    userId = currentUser?.uid ?: "",
                    userName = currentUser?.displayName ?: "Yo",
                    userPhotoUrl = currentUser?.photoUrl?.toString() ?: "",
                    rating = rating,
                    comment = comment,
                    timestamp = Date()
                )
                val currentList = _reviews.value.orEmpty().toMutableList()
                currentList.add(0, newReview)
                _reviews.value = currentList
            } else {
                _toastMessage.value = "No pudimos publicar tu reseña."
            }
            _isLoadingAction.value = false
        }
    }

    fun loadUserReviews() {
        _isLoadingAction.value = true
        viewModelScope.launch {
            repository.getUserReviews().onSuccess { _userReviews.value = it }
            _isLoadingAction.value = false
        }
    }

    fun deleteReview(review: Review) {
        _isLoadingAction.value = true
        viewModelScope.launch {
            repository.deleteReview(review.recipeId, review.id).onSuccess {
                _reviewActionState.value = Result.success("Reseña eliminada")
                loadUserReviews()
            }
            _isLoadingAction.value = false
        }
    }

    fun editReview(review: Review, newRating: Float, newComment: String) {
        _isLoadingAction.value = true
        viewModelScope.launch {
            repository.updateReview(review.recipeId, review.id, newRating, newComment).onSuccess {
                _reviewActionState.value = Result.success("Reseña actualizada")
                loadUserReviews()
            }
            _isLoadingAction.value = false
        }
    }

    // === PERFIL PÚBLICO ===
    fun loadPublicUserContent(userId: String) {
        _isLoadingAction.value = true
        viewModelScope.launch {
            repository.getUserRecipes(userId).onSuccess { _publicUserRecipes.value = it }
            repository.getUserReviews(userId).onSuccess { _publicUserReviews.value = it }
            _isLoadingAction.value = false
        }
    }
}