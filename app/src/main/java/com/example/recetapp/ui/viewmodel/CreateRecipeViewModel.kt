package com.example.recetapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.Ingredient
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.data.model.RecipeTranslations
import com.example.recetapp.data.repository.RecipeRepository
import kotlinx.coroutines.launch
import java.util.UUID

class CreateRecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _createResult = MutableLiveData<Result<Boolean>>()
    val createResult: LiveData<Result<Boolean>> = _createResult

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private var editingRecipeId: String? = null
    private var currentImageUrl: String? = null

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
    }

    fun loadRecipeForEdit(recipe: Recipe) {
        editingRecipeId = recipe.id
        currentImageUrl = recipe.imageUrl
    }

    fun saveRecipe(
        name: String,
        categoryEsp: String,
        ingredientsText: String,
        instructions: String,
        readyInMinutes: String
    ) {
        if (name.isBlank() || ingredientsText.isBlank() || instructions.isBlank()) {
            _createResult.value = Result.failure(Exception("Completa los campos obligatorios"))
            return
        }

        if (_selectedImageUri.value == null && currentImageUrl == null) {
            _createResult.value = Result.failure(Exception("Debes seleccionar una foto"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            // 1. Obtener nombre del creador
            val creatorName = repository.getCurrentUserName()

            // 2. Subir imagen si cambió
            val imageUrl = if (_selectedImageUri.value != null) {
                repository.uploadImage(_selectedImageUri.value!!).getOrDefault(currentImageUrl ?: "")
            } else {
                currentImageUrl ?: ""
            }

            val categoryKey = RecipeTranslations.getCategoryKey(categoryEsp) ?: "Homemade"

            val ingredientsList = ingredientsText.lines()
                .filter { it.isNotBlank() }
                .map { line -> Ingredient(name = line.trim(), measure = "") }

            val recipe = Recipe(
                id = editingRecipeId ?: UUID.randomUUID().toString(),
                creatorName = creatorName, // <--- Guardamos el nombre aquí
                name = name,
                category = categoryKey,
                area = "Casera",
                instructions = instructions,
                imageUrl = imageUrl,
                thumbnailUrl = imageUrl,
                ingredients = ingredientsList,
                readyInMinutes = readyInMinutes.toIntOrNull(),
                source = RecipeSource.USER
            )

            val result = if (editingRecipeId == null) {
                repository.createRecipe(recipe)
            } else {
                repository.updateRecipe(recipe)
            }

            _createResult.value = result
            _isLoading.value = false
        }
    }
}