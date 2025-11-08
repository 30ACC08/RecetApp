package com.example.recetapp.data.repository

import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        const val ADMIN_EMAIL = "admin@recetapp.com"
    }

    suspend fun registerUser(nombre: String, email: String, password: String): Result<User> {
        return try {
            if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                return Result.failure(Exception("Este correo está reservado"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid

            if (userId == null) {
                return Result.failure(Exception("Error al crear usuario"))
            }

            val userData = hashMapOf(
                "nombre" to nombre,
                "email" to email,
                "rol" to UserRole.USER.name
            )

            firestore.collection("usuarios")
                .document(userId)
                .set(userData)
                .await()

            val user = User(userId, nombre, email, UserRole.USER)
            Result.success(user)

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Error al registrar usuario"
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid

            if (userId == null) {
                return Result.failure(Exception("Error al iniciar sesión"))
            }

            val document = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val nombre = document.getString("nombre") ?: ""
                val rolString = document.getString("rol") ?: UserRole.USER.name
                val rol = try {
                    UserRole.valueOf(rolString)
                } catch (ex: Exception) {
                    UserRole.USER
                }

                val user = User(userId, nombre, email, rol)
                Result.success(user)
            } else {
                Result.failure(Exception("Usuario no encontrado en la base de datos"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Email o contraseña incorrectos"))
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection("usuarios")
                .get()
                .await()

            val users = mutableListOf<User>()
            val documents = snapshot.documents

            for (document in documents) {
                val id = document.id
                val nombre = document.getString("nombre") ?: ""
                val email = document.getString("email") ?: ""
                val rolString = document.getString("rol") ?: UserRole.USER.name
                val rol = try {
                    UserRole.valueOf(rolString)
                } catch (ex: Exception) {
                    UserRole.USER
                }

                users.add(User(id, nombre, email, rol))
            }

            users.sortedBy { it.nombre }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateUser(userId: String, newNombre: String): Result<User> {
        return try {
            val updates = hashMapOf<String, Any>(
                "nombre" to newNombre
            )

            firestore.collection("usuarios")
                .document(userId)
                .update(updates)
                .await()

            val document = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()

            val email = document.getString("email") ?: ""
            val rolString = document.getString("rol") ?: UserRole.USER.name
            val rol = try {
                UserRole.valueOf(rolString)
            } catch (ex: Exception) {
                UserRole.USER
            }

            val user = User(userId, newNombre, email, rol)
            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String, userEmail: String): Result<Boolean> {
        return try {
            if (userEmail.equals(ADMIN_EMAIL, ignoreCase = true)) {
                return Result.failure(Exception("No se puede eliminar al administrador"))
            }

            firestore.collection("usuarios")
                .document(userId)
                .delete()
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                return null
            }

            val document = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val nombre = document.getString("nombre") ?: ""
                val email = document.getString("email") ?: ""
                val rolString = document.getString("rol") ?: UserRole.USER.name
                val rol = try {
                    UserRole.valueOf(rolString)
                } catch (ex: Exception) {
                    UserRole.USER
                }

                User(userId, nombre, email, rol)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}