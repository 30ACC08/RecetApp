package com.example.recetapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.*
import com.example.recetapp.data.repository.RecipeRepository
import com.example.recetapp.data.repository.RecipeTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository()
    private val translator = RecipeTranslator()
    private var searchJob: Job? = null

    // Listas
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    // NUEVO: Lista para mis recetas (Perfil)
    private val _myRecipes = MutableLiveData<List<Recipe>>()
    val myRecipes: LiveData<List<Recipe>> = _myRecipes

    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes
    private val _breakfastRecipes = MutableLiveData<List<Recipe>>()
    val breakfastRecipes: LiveData<List<Recipe>> = _breakfastRecipes
    private val _healthyRecipes = MutableLiveData<List<Recipe>>()
    val healthyRecipes: LiveData<List<Recipe>> = _healthyRecipes
    private val _favorites = MutableLiveData<List<Recipe>>()
    val favorites: LiveData<List<Recipe>> = _favorites
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    // Selección y Estado
    private val _selectedRecipe = MutableLiveData<Recipe>()
    val selectedRecipe: LiveData<Recipe> = _selectedRecipe
    private val _isRecipeFavorite = MutableLiveData<Boolean?>()
    val isRecipeFavorite: LiveData<Boolean?> = _isRecipeFavorite

    private val _currentFilter = MutableLiveData(RecipeFilter())
    val currentFilter: LiveData<RecipeFilter> = _currentFilter
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Traducción
    private var originalRecipeCache: Recipe? = null
    private var translatedRecipeCache: Recipe? = null
    private val _isTranslated = MutableLiveData(false)
    val isTranslated: LiveData<Boolean> = _isTranslated

    init {
        viewModelScope.launch { translator.prepareModel() }
    }

    // === NUEVO: CARGAR MIS RECETAS (Para el perfil) ===
    fun loadMyRecipes() {
        viewModelScope.launch {
            repository.getUserRecipes().onSuccess {
                _myRecipes.value = it
            }
        }
    }

    // ... (RESTO DE MÉTODOS IGUAL QUE ANTES) ...

    fun setSelectedRecipe(recipe: Recipe) {
        viewModelScope.launch {
            val formatted = translator.formatRecipeInstructions(recipe)
            originalRecipeCache = formatted
            translatedRecipeCache = null
            _selectedRecipe.value = formatted
            _isTranslated.value = false
            _isRecipeFavorite.value = null
        }
    }

    fun checkIfFavorite(recipeId: String) { viewModelScope.launch { _isRecipeFavorite.value = repository.isFavorite(recipeId) } }

    fun toggleFavorite(recipe: Recipe) {
        val isCurrentlyFav = _isRecipeFavorite.value ?: false
        if (isCurrentlyFav) {
            repository.removeFavorite(recipe.id)
            _isRecipeFavorite.value = false
        } else {
            repository.addFavorite(recipe)
            _isRecipeFavorite.value = true
        }
        if (_favorites.value != null) { viewModelScope.launch { delay(200); loadFavorites() } }
    }

    fun loadFavorites() { viewModelScope.launch { repository.getFavorites().onSuccess { _favorites.value = it } } }

    fun toggleTranslation() {
        if (_isTranslated.value == true) {
            originalRecipeCache?.let { _selectedRecipe.value = it }
            _isTranslated.value = false
        } else {
            if (translatedRecipeCache != null) {
                _selectedRecipe.value = translatedRecipeCache
                _isTranslated.value = true
            } else {
                _isLoading.value = true
                viewModelScope.launch {
                    originalRecipeCache?.let {
                        val trans = translator.translateRecipe(it)
                        translatedRecipeCache = trans
                        _selectedRecipe.value = trans
                        _isTranslated.value = true
                    }
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadHomeContent() {
        _isLoading.value = true
        viewModelScope.launch {
            launch { repository.getRandomRecipes(10).onSuccess { _featuredRecipes.value = it } }
            launch { repository.searchRecipes(RecipeFilter(category = "Breakfast")).onSuccess { _breakfastRecipes.value = it } }
            launch { repository.searchRecipes(RecipeFilter(vegetarian = true)).onSuccess { _healthyRecipes.value = it } }
            delay(500)
            _isLoading.value = false
        }
    }

    fun searchRecipesDebounced(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query.length >= 3) performSearch(query)
        }
    }

    fun searchRecipes(query: String) { viewModelScope.launch { performSearch(query) } }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        val filter = _currentFilter.value?.copy(query = query) ?: RecipeFilter(query = query)
        _currentFilter.value = filter
        repository.searchRecipes(filter).onSuccess { _recipes.value = it }.onFailure { _recipes.value = emptyList() }
        _isLoading.value = false
    }

    fun toggleCategory(c: String) { applyFilter(_currentFilter.value!!.copy(category = if(_currentFilter.value?.category == c) null else c)) }
    fun toggleVegetarian() { applyFilter(_currentFilter.value!!.copy(vegetarian = if(_currentFilter.value?.vegetarian == true) null else true)) }
    fun toggleVegan() { applyFilter(_currentFilter.value!!.copy(vegan = if(_currentFilter.value?.vegan == true) null else true)) }
    fun applyFilter(f: RecipeFilter) { _currentFilter.value = f; searchRecipes(f.query) }
    fun clearFilters() { applyFilter(RecipeFilter()) }

    fun updateFilter(category: String? = null, vegetarian: Boolean? = null, vegan: Boolean? = null) {
        val c = _currentFilter.value!!
        applyFilter(c.copy(category = category ?: c.category, vegetarian = vegetarian ?: c.vegetarian, vegan = vegan ?: c.vegan))
    }
    fun searchByCategory(c: String) { updateFilter(category = c) }
    fun loadCategories() { viewModelScope.launch { repository.getCategories().onSuccess { _categories.value = it } } }
    fun hasActiveFilters() = false
    fun getActiveFiltersCount() = 0
    fun loadFeaturedRecipes() { loadHomeContent() }
}