package com.example.recetapp.data.repository

import android.net.Uri
import android.util.Log
import com.example.recetapp.data.model.Notification
import com.example.recetapp.data.model.NotificationType
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore
    private val storage = FirebaseStorage.getInstance()

    init {
        firestore = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e(TAG, "Error config Firestore", e)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "admin@recetapp.com"
        private const val TAG = "FirebaseAuthRepo"
    }

    // === AUTH & USER CRUD ===

    suspend fun registerUser(nombre: String, email: String, password: String): Result<User> {
        return try {
            if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                return Result.failure(Exception("Email reservado para admin"))
            }
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Error UID nulo"))

            val userData = hashMapOf(
                "nombre" to nombre,
                "email" to email,
                "photoUrl" to "",
                "rol" to UserRole.USER.name,
                "fechaCreacion" to System.currentTimeMillis()
            )

            firestore.collection("usuarios").document(userId).set(userData)
                .addOnFailureListener { e -> Log.e(TAG, "Error guardando en background", e) }

            Result.success(User(userId, nombre, email, "", UserRole.USER))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Error UID"))

            if (email.equals(ADMIN_EMAIL, true)) {
                handleAdminLogin(userId, email)
            } else {
                handleNormalUserLogin(userId, email)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No usuario"))
            val filename = "profile_$userId.jpg"
            val ref = storage.reference.child("profile_images/$filename")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, newNombre: String, newPhotoUrl: String? = null): Result<User> {
        return try {
            val updates = mutableMapOf<String, Any>("nombre" to newNombre)
            if (newPhotoUrl != null) {
                updates["photoUrl"] = newPhotoUrl
            }
            firestore.collection("usuarios").document(userId).update(updates).await()
            val currentUserDoc = firestore.collection("usuarios").document(userId).get().await()
            val photo = currentUserDoc.getString("photoUrl") ?: ""
            val rolStr = currentUserDoc.getString("rol") ?: "USER"
            val rol = try { UserRole.valueOf(rolStr) } catch (e: Exception) { UserRole.USER }
            val email = currentUserDoc.getString("email") ?: ""
            Result.success(User(userId, newNombre, email, photo, rol))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteUser(userId: String, userEmail: String): Result<Boolean> {
        return try {
            if (userEmail.equals(ADMIN_EMAIL, true)) return Result.failure(Exception("No se puede borrar al admin"))
            firestore.collection("usuarios").document(userId).delete().await()
            val currentUser = auth.currentUser
            if (currentUser?.uid == userId) {
                currentUser.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("usuarios").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val nombre = doc.getString("nombre") ?: "Sin nombre"
                    val email = doc.getString("email") ?: "Sin email"
                    val photo = doc.getString("photoUrl") ?: ""
                    val rolStr = doc.getString("rol")?.uppercase() ?: "USER"
                    val rol = try { UserRole.valueOf(rolStr) } catch (e: Exception) { UserRole.USER }
                    User(id, nombre, email, photo, rol)
                } catch (e: Exception) { null }
            }
            Result.success(users.sortedBy { it.nombre })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        val email = auth.currentUser?.email ?: return null
        if (email.equals(ADMIN_EMAIL, true)) return User(uid, "Admin", email, "", UserRole.ADMIN)
        return try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            val nombre = doc.getString("nombre") ?: email.substringBefore("@")
            val photo = doc.getString("photoUrl") ?: ""
            val rolStr = doc.getString("rol")?.uppercase() ?: "USER"
            val rol = try { UserRole.valueOf(rolStr) } catch (e:Exception) { UserRole.USER }
            User(uid, nombre, email, photo, rol)
        } catch (e: Exception) {
            User(uid, email.substringBefore("@"), email, "", UserRole.USER)
        }
    }

    private suspend fun handleAdminLogin(userId: String, email: String): Result<User> {
        val docRef = firestore.collection("usuarios").document(userId)
        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val data = hashMapOf("nombre" to "Administrador", "email" to email, "rol" to UserRole.ADMIN.name)
                docRef.set(data)
            }
        }
        return Result.success(User(userId, "Administrador", email, "", UserRole.ADMIN))
    }

    private suspend fun handleNormalUserLogin(userId: String, email: String): Result<User> {
        return try {
            val doc = firestore.collection("usuarios").document(userId).get().await()
            if (doc.exists()) {
                val nombre = doc.getString("nombre") ?: email.substringBefore("@")
                val photo = doc.getString("photoUrl") ?: ""
                val rolStr = doc.getString("rol")?.uppercase() ?: "USER"
                val rol = try { UserRole.valueOf(rolStr) } catch (e: Exception) { UserRole.USER }
                Result.success(User(userId, nombre, email, photo, rol))
            } else {
                val nombre = email.substringBefore("@")
                val data = hashMapOf("nombre" to nombre, "email" to email, "rol" to UserRole.USER.name)
                firestore.collection("usuarios").document(userId).set(data)
                Result.success(User(userId, nombre, email, "", UserRole.USER))
            }
        } catch (e: Exception) {
            Result.success(User(userId, email.substringBefore("@"), email, "", UserRole.USER))
        }
    }

    fun isLoggedIn() = auth.currentUser != null
    fun logout() = auth.signOut()

    // === SEGUIDORES Y NOTIFICACIONES ===

    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Boolean> {
        return try {
            val batch = firestore.batch()
            val myFollowingRef = firestore.collection("usuarios").document(currentUserId)
                .collection("siguiendo").document(targetUserId)
            batch.set(myFollowingRef, mapOf("timestamp" to System.currentTimeMillis()))
            val theirFollowersRef = firestore.collection("usuarios").document(targetUserId)
                .collection("seguidores").document(currentUserId)
            batch.set(theirFollowersRef, mapOf("timestamp" to System.currentTimeMillis()))

            // NUEVO: Crear Notificación
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                val notifId = UUID.randomUUID().toString()
                val notifRef = firestore.collection("usuarios").document(targetUserId)
                    .collection("notificaciones").document(notifId)

                val notification = Notification(
                    id = notifId,
                    type = NotificationType.FOLLOW,
                    title = "¡Nuevo seguidor!",
                    message = "${currentUser.nombre} ha comenzado a seguirte.",
                    fromUserId = currentUserId,
                    fromUserName = currentUser.nombre,
                    fromUserPhotoUrl = currentUser.photoUrl,
                    relatedId = currentUserId,
                    timestamp = java.util.Date()
                )
                batch.set(notifRef, notification)
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Boolean> {
        return try {
            val batch = firestore.batch()
            val myFollowingRef = firestore.collection("usuarios").document(currentUserId)
                .collection("siguiendo").document(targetUserId)
            batch.delete(myFollowingRef)
            val theirFollowersRef = firestore.collection("usuarios").document(targetUserId)
                .collection("seguidores").document(currentUserId)
            batch.delete(theirFollowersRef)
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val doc = firestore.collection("usuarios").document(currentUserId)
                .collection("siguiendo").document(targetUserId).get().await()
            doc.exists()
        } catch (e: Exception) { false }
    }

    suspend fun getUserStats(userId: String): Result<Pair<Int, Int>> {
        return try {
            val followers = firestore.collection("usuarios").document(userId).collection("seguidores").get().await().size()
            val following = firestore.collection("usuarios").document(userId).collection("siguiendo").get().await().size()
            Result.success(Pair(followers, following))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getFollowingUsers(currentUserId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("usuarios").document(currentUserId)
                .collection("siguiendo").get().await()

            val followingIds = snapshot.documents.map { it.id }
            if (followingIds.isEmpty()) return Result.success(emptyList())

            val users = mutableListOf<User>()
            for (id in followingIds) {
                try {
                    val doc = firestore.collection("usuarios").document(id).get().await()
                    if (doc.exists()) {
                        val nombre = doc.getString("nombre") ?: "Usuario"
                        val email = doc.getString("email") ?: ""
                        val photo = doc.getString("photoUrl") ?: ""
                        users.add(User(id, nombre, email, photo, UserRole.USER))
                    }
                } catch (e: Exception) { continue }
            }
            Result.success(users)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getNotifications(userId: String): Result<List<Notification>> {
        return try {
            val snapshot = firestore.collection("usuarios").document(userId)
                .collection("notificaciones")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Notification::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }
}