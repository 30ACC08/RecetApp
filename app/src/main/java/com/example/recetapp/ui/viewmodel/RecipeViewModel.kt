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

    // ==================== LiveData ====================

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes

    private val _selectedRecipe = MutableLiveData<Recipe>()
    val selectedRecipe: LiveData<Recipe> = _selectedRecipe

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _currentFilter = MutableLiveData(RecipeFilter())
    val currentFilter: LiveData<RecipeFilter> = _currentFilter

    // ==================== Búsqueda y Filtros ====================

    fun searchRecipes(query: String = "") {
        viewModelScope.launch {
            _isLoading.value = true

            val filter = _currentFilter.value?.copy(query = query) ?: RecipeFilter(query = query)
            _currentFilter.value = filter

            val result = repository.searchRecipes(filter)

            result.onSuccess { recipeList ->
                _recipes.value = recipeList
                _error.value = null
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error al buscar recetas"
                _recipes.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    fun applyFilter(filter: RecipeFilter) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentFilter.value = filter

            val result = repository.searchRecipes(filter)

            result.onSuccess { recipeList ->
                _recipes.value = recipeList
                _error.value = null
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error al filtrar recetas"
            }

            _isLoading.value = false
        }
    }

    fun updateFilter(
        category: String? = _currentFilter.value?.category,
        area: String? = _currentFilter.value?.area,
        source: RecipeSource? = _currentFilter.value?.source,
        maxReadyTime: Int? = _currentFilter.value?.maxReadyTime,
        minHealthScore: Int? = _currentFilter.value?.minHealthScore,
        maxCalories: Int? = _currentFilter.value?.maxCalories,
        vegetarian: Boolean? = _currentFilter.value?.vegetarian,
        vegan: Boolean? = _currentFilter.value?.vegan,
        glutenFree: Boolean? = _currentFilter.value?.glutenFree,
        dairyFree: Boolean? = _currentFilter.value?.dairyFree,
        sortBy: SortOption = _currentFilter.value?.sortBy ?: SortOption.POPULARITY
    ) {
        val currentQuery = _currentFilter.value?.query ?: ""
        val newFilter = RecipeFilter(
            query = currentQuery,
            category = category,
            area = area,
            source = source,
            maxReadyTime = maxReadyTime,
            minHealthScore = minHealthScore,
            maxCalories = maxCalories,
            vegetarian = vegetarian,
            vegan = vegan,
            glutenFree = glutenFree,
            dairyFree = dairyFree,
            sortBy = sortBy
        )
        applyFilter(newFilter)
    }

    fun clearFilters() {
        applyFilter(RecipeFilter(query = _currentFilter.value?.query ?: ""))
    }

    // ==================== Recetas Destacadas ====================

    fun loadFeaturedRecipes() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getRandomRecipes(10)

            result.onSuccess { recipeList ->
                _featuredRecipes.value = recipeList
                _error.value = null
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error al cargar recetas destacadas"
                _featuredRecipes.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    // ==================== Detalle de Receta ====================

    fun loadRecipeDetail(recipeId: String, source: RecipeSource) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getRecipeDetail(recipeId, source)

            result.onSuccess { recipe ->
                _selectedRecipe.value = recipe
                _error.value = null
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error al cargar detalle de receta"
            }

            _isLoading.value = false
        }
    }

    fun setSelectedRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
    }

    // ==================== Categorías ====================

    fun loadCategories() {
        viewModelScope.launch {
            val result = repository.getCategories()

            result.onSuccess { categoriesList ->
                _categories.value = categoriesList
            }.onFailure {
                _categories.value = RecipeCategories.ALL
            }
        }
    }

    // ==================== Búsqueda por Categoría ====================

    fun searchByCategory(category: String) {
        updateFilter(category = category)
    }

    fun searchByArea(area: String) {
        updateFilter(area = area)
    }

    // ==================== Helpers ====================

    fun hasActiveFilters(): Boolean {
        val filter = _currentFilter.value ?: return false
        return filter.category != null ||
                filter.area != null ||
                filter.source != null ||
                filter.maxReadyTime != null ||
                filter.minHealthScore != null ||
                filter.maxCalories != null ||
                filter.vegetarian == true ||
                filter.vegan == true ||
                filter.glutenFree == true ||
                filter.dairyFree == true
    }

    fun getActiveFiltersCount(): Int {
        val filter = _currentFilter.value ?: return 0
        var count = 0
        if (filter.category != null) count++
        if (filter.area != null) count++
        if (filter.source != null) count++
        if (filter.maxReadyTime != null) count++
        if (filter.minHealthScore != null) count++
        if (filter.maxCalories != null) count++
        if (filter.vegetarian == true) count++
        if (filter.vegan == true) count++
        if (filter.glutenFree == true) count++
        if (filter.dairyFree == true) count++
        return count
    }
}