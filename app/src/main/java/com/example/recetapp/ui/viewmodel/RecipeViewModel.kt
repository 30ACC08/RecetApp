package com.example.recetapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.*
import com.example.recetapp.data.repository.RecipeRepository
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository()

    // LiveData
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes

    private val _selectedRecipe = MutableLiveData<Recipe>()
    val selectedRecipe: LiveData<Recipe> = _selectedRecipe

    private val _favorites = MutableLiveData<List<Recipe>>()
    val favorites: LiveData<List<Recipe>> = _favorites

    private val _isRecipeFavorite = MutableLiveData<Boolean>()
    val isRecipeFavorite: LiveData<Boolean> = _isRecipeFavorite

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _currentFilter = MutableLiveData(RecipeFilter())
    val currentFilter: LiveData<RecipeFilter> = _currentFilter

    // === FAVORITOS ===

    fun checkIfFavorite(recipeId: String) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(recipeId)
            _isRecipeFavorite.value = isFav
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        val isCurrentlyFav = _isRecipeFavorite.value ?: false

        if (isCurrentlyFav) {
            repository.removeFavorite(recipe.id)
            _isRecipeFavorite.value = false
        } else {
            repository.addFavorite(recipe)
            _isRecipeFavorite.value = true
        }

        // Si estamos en la pantalla de favoritos, actualizar la lista con un pequeño delay
        if (_favorites.value != null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                loadFavorites()
            }
        }
    }

    fun loadFavorites() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getFavorites()
            result.onSuccess { _favorites.value = it }
                .onFailure { _error.value = "Error al cargar favoritos" }
            _isLoading.value = false
        }
    }

    // === MÉTODOS DE CARGA Y BÚSQUEDA ===

    fun loadFeaturedRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getRandomRecipes(10)
            result.onSuccess { _featuredRecipes.value = it }
            _isLoading.value = false
        }
    }

    fun searchRecipes(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val filter = _currentFilter.value?.copy(query = query) ?: RecipeFilter(query = query)
            _currentFilter.value = filter
            val result = repository.searchRecipes(filter)
            result.onSuccess { _recipes.value = it }.onFailure { _recipes.value = emptyList() }
            _isLoading.value = false
        }
    }

    fun applyFilter(filter: RecipeFilter) {
        _currentFilter.value = filter
        searchRecipes(filter.query)
    }

    fun updateFilter(category: String? = null, vegetarian: Boolean? = null, vegan: Boolean? = null) {
        val current = _currentFilter.value ?: RecipeFilter()
        val newFilter = current.copy(category = category ?: current.category, vegetarian = vegetarian ?: current.vegetarian, vegan = vegan ?: current.vegan)
        applyFilter(newFilter)
    }

    fun clearFilters() { applyFilter(RecipeFilter()) }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { _categories.value = it }
        }
    }

    fun setSelectedRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
    }

    fun searchByCategory(category: String) { updateFilter(category = category) }
    fun hasActiveFilters() = false // Simplificado
    fun getActiveFiltersCount() = 0 // Simplificado
}