package com.example.recetapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val rol: UserRole = UserRole.USER
) : Parcelable

enum class UserRole {
    ADMIN,
    USER
}