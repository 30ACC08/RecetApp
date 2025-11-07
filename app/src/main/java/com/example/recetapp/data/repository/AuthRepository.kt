package com.example.recetapp.data.repository

import android.content.Context
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("users_db", Context.MODE_PRIVATE)

    companion object {
        // Credenciales del administrador (puedes cambiarlas)
        const val ADMIN_EMAIL = "admin@recetapp.com"
        const val ADMIN_PASSWORD = "Admin123"
        const val ADMIN_NAME = "Administrador"
    }

    init {
        // Crear usuario administrador si no existe
        createAdminIfNotExists()
    }

    private fun createAdminIfNotExists() {
        if (!isEmailRegistered(ADMIN_EMAIL)) {
            val adminId = UUID.randomUUID().toString()
            prefs.edit().apply {
                putString("user_${ADMIN_EMAIL}_id", adminId)
                putString("user_${ADMIN_EMAIL}_nombre", ADMIN_NAME)
                putString("user_${ADMIN_EMAIL}_password", ADMIN_PASSWORD)
                putString("user_${ADMIN_EMAIL}_rol", UserRole.ADMIN.name)
                apply()
            }
        }
    }

    fun registerUser(nombre: String, email: String, password: String): Result<User> {
        return try {
            // No permitir registrar con email de admin
            if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                return Result.failure(Exception("Este correo está reservado"))
            }

            if (isEmailRegistered(email)) {
                Result.failure(Exception("El correo electrónico ya está registrado"))
            } else {
                val userId = UUID.randomUUID().toString()
                val user = User(userId, nombre, email, password, UserRole.USER)

                prefs.edit().apply {
                    putString("user_${email}_id", userId)
                    putString("user_${email}_nombre", nombre)
                    putString("user_${email}_password", password)
                    putString("user_${email}_rol", UserRole.USER.name)
                    apply()
                }

                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginUser(email: String, password: String): Result<User> {
        return try {
            val savedPassword = prefs.getString("user_${email}_password", null)

            if (savedPassword == null) {
                Result.failure(Exception("Usuario no encontrado"))
            } else if (savedPassword != password) {
                Result.failure(Exception("Contraseña incorrecta"))
            } else {
                val userId = prefs.getString("user_${email}_id", "") ?: ""
                val nombre = prefs.getString("user_${email}_nombre", "") ?: ""
                val rolString = prefs.getString("user_${email}_rol", UserRole.USER.name) ?: UserRole.USER.name
                val rol = try {
                    UserRole.valueOf(rolString)
                } catch (e: Exception) {
                    UserRole.USER
                }

                val user = User(userId, nombre, email, password, rol)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isEmailRegistered(email: String): Boolean {
        return prefs.contains("user_${email}_password")
    }

    fun getUserByEmail(email: String): User? {
        return try {
            val userId = prefs.getString("user_${email}_id", null)
            val nombre = prefs.getString("user_${email}_nombre", null)
            val password = prefs.getString("user_${email}_password", null)
            val rolString = prefs.getString("user_${email}_rol", UserRole.USER.name)

            if (userId != null && nombre != null && password != null) {
                val rol = try {
                    UserRole.valueOf(rolString ?: UserRole.USER.name)
                } catch (e: Exception) {
                    UserRole.USER
                }
                User(userId, nombre, email, password, rol)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getAllUsers(): List<User> {
        val allEntries = prefs.all
        val users = mutableListOf<User>()

        val emails = mutableSetOf<String>()
        for ((key, _) in allEntries) {
            if (key.startsWith("user_") && key.endsWith("_id")) {
                val email = key.removePrefix("user_").removeSuffix("_id")
                emails.add(email)
            }
        }

        for (email in emails) {
            val id = prefs.getString("user_${email}_id", "") ?: ""
            val nombre = prefs.getString("user_${email}_nombre", "") ?: ""
            val password = prefs.getString("user_${email}_password", "") ?: ""
            val rolString = prefs.getString("user_${email}_rol", UserRole.USER.name) ?: UserRole.USER.name

            if (id.isNotEmpty() && nombre.isNotEmpty()) {
                val rol = try {
                    UserRole.valueOf(rolString)
                } catch (e: Exception) {
                    UserRole.USER
                }
                users.add(User(id, nombre, email, password, rol))
            }
        }

        return users.sortedBy { it.nombre }
    }

    fun updateUser(email: String, newNombre: String, newPassword: String?): Result<User> {
        return try {
            if (!isEmailRegistered(email)) {
                return Result.failure(Exception("Usuario no encontrado"))
            }

            val userId = prefs.getString("user_${email}_id", "") ?: ""
            val rolString = prefs.getString("user_${email}_rol", UserRole.USER.name) ?: UserRole.USER.name
            val rol = try {
                UserRole.valueOf(rolString)
            } catch (e: Exception) {
                UserRole.USER
            }

            val finalPassword = newPassword ?: prefs.getString("user_${email}_password", "") ?: ""

            prefs.edit().apply {
                putString("user_${email}_nombre", newNombre)
                if (newPassword != null) {
                    putString("user_${email}_password", newPassword)
                }
                apply()
            }

            val updatedUser = User(userId, newNombre, email, finalPassword, rol)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteUser(email: String): Result<Boolean> {
        return try {
            // No permitir eliminar al admin
            if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                return Result.failure(Exception("No se puede eliminar al administrador"))
            }

            if (!isEmailRegistered(email)) {
                return Result.failure(Exception("Usuario no encontrado"))
            }

            prefs.edit().apply {
                remove("user_${email}_id")
                remove("user_${email}_nombre")
                remove("user_${email}_password")
                remove("user_${email}_rol")
                apply()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}