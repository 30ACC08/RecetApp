package com.example.recetapp.data.model

import java.util.Date

data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.INFO,
    val title: String = "",
    val message: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhotoUrl: String = "",
    val relatedId: String = "", // ID de la receta o usuario relacionado
    val read: Boolean = false,
    val timestamp: Date = Date()
)

enum class NotificationType {
    FOLLOW, // Alguien te siguió
    REVIEW, // Alguien comentó tu receta
    LIKE,   // Alguien dio like
    INFO    // Sistema
}