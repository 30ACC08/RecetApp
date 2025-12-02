package com.example.recetapp.data.model

import java.util.Date

data class Review(
    val id: String = "",
    val recipeId: String = "",
    val recipeName: String = "",
    val recipeImageUrl: String = "",
    val userId: String = "",
    val userName: String = "Anónimo",
    val userPhotoUrl: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Date = Date()
)