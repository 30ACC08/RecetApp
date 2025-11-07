package com.example.recetapp.data.model

data class User(
    val id: String,
    val nombre: String,
    val email: String,
    val password: String,
    val rol: UserRole = UserRole.USER
)

enum class UserRole {
    ADMIN,
    USER
}