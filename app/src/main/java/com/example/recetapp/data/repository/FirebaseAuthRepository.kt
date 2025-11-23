package com.example.recetapp.data.repository

import android.util.Log
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore

    init {
        firestore = FirebaseFirestore.getInstance()

        // Configurar Firestore con persistencia
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Log.d(TAG, "✅ Firestore configurado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar Firestore", e)
        }

        Log.d(TAG, "FirebaseAuthRepository inicializado")
    }

    companion object {
        const val ADMIN_EMAIL = "admin@recetapp.com"
        private const val TAG = "FirebaseAuthRepo"
    }

    /**
     * Registrar un nuevo usuario
     */
    suspend fun registerUser(nombre: String, email: String, password: String): Result<User> {
        return try {
            // Verificar que no sea el email del admin
            if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                Log.w(TAG, "Intento de registro con email reservado: $email")
                return Result.failure(Exception("Este correo está reservado para el administrador"))
            }

            Log.d(TAG, "=== REGISTRO DE USUARIO ===")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Nombre: $nombre")

            // Crear usuario en Authentication
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid

            if (userId == null) {
                Log.e(TAG, "❌ Error: UID es null después de crear usuario")
                return Result.failure(Exception("Error al crear usuario"))
            }

            Log.d(TAG, "✅ Usuario creado en Auth con UID: $userId")

            // Crear documento en Firestore
            try {
                val userData = hashMapOf(
                    "nombre" to nombre,
                    "email" to email,
                    "rol" to UserRole.USER.name
                )

                firestore.collection("usuarios")
                    .document(userId)
                    .set(userData)
                    .await()

                Log.d(TAG, "✅ Documento creado en Firestore para: $email")
            } catch (firestoreError: Exception) {
                Log.e(TAG, "⚠️ Error al crear documento en Firestore", firestoreError)
                // Continuar aunque falle Firestore
            }

            val user = User(userId, nombre, email, UserRole.USER)
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al registrar usuario", e)
            val errorMessage = when {
                e.message?.contains("email address is already", ignoreCase = true) == true ->
                    "Este correo ya está registrado"
                e.message?.contains("password", ignoreCase = true) == true ->
                    "La contraseña debe tener al menos 6 caracteres"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> e.message ?: "Error al registrar usuario"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Iniciar sesión
     */
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "=== INICIO DE LOGIN ===")
            Log.d(TAG, "Email: $email")

            // Iniciar sesión en Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid

            if (userId == null) {
                Log.e(TAG, "❌ Error: UID es null")
                return Result.failure(Exception("Error al iniciar sesión"))
            }

            Log.d(TAG, "✅ Autenticación exitosa. UID: $userId")

            // Verificar si es admin
            val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)

            if (isAdmin) {
                Log.d(TAG, "Usuario identificado como ADMIN")
                return handleAdminLogin(userId, email)
            } else {
                Log.d(TAG, "Usuario identificado como NORMAL")
                return handleNormalUserLogin(userId, email)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR EN LOGIN:", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Sin conexión a internet"
                e.message?.contains("password is invalid", ignoreCase = true) == true ||
                        e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ||
                        e.message?.contains("INVALID_PASSWORD", ignoreCase = true) == true ->
                    "Email o contraseña incorrectos"
                else -> "Error al iniciar sesión: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Maneja el login de administrador
     */
    private suspend fun handleAdminLogin(userId: String, email: String): Result<User> {
        return try {
            Log.d(TAG, "Verificando documento de admin...")

            val adminDoc = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()

            if (!adminDoc.exists()) {
                Log.d(TAG, "Creando documento de admin...")
                val adminData = hashMapOf(
                    "nombre" to "Administrador",
                    "email" to email,
                    "rol" to UserRole.ADMIN.name,
                    "fechaCreacion" to System.currentTimeMillis()
                )

                firestore.collection("usuarios")
                    .document(userId)
                    .set(adminData)
                    .await()
            }

            val nombre = adminDoc.getString("nombre") ?: "Administrador"
            Log.d(TAG, "✅ Login admin exitoso: $nombre")
            Result.success(User(userId, nombre, email, UserRole.ADMIN))

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error en Firestore, usando datos básicos", e)
            // Si falla Firestore, aún permitir login
            Result.success(User(userId, "Administrador", email, UserRole.ADMIN))
        }
    }

    /**
     * Maneja el login de usuario normal
     */
    private suspend fun handleNormalUserLogin(userId: String, email: String): Result<User> {
        return try {
            Log.d(TAG, "Buscando documento de usuario...")

            val document = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()

            if (!document.exists()) {
                Log.e(TAG, "❌ Documento no encontrado para usuario: $email")
                // Crear documento básico si no existe
                Log.d(TAG, "Creando documento básico para usuario...")
                val userData = hashMapOf(
                    "nombre" to email.substringBefore("@"),
                    "email" to email,
                    "rol" to UserRole.USER.name,
                    "fechaCreacion" to System.currentTimeMillis()
                )

                firestore.collection("usuarios")
                    .document(userId)
                    .set(userData)
                    .await()

                return Result.success(User(userId, email.substringBefore("@"), email, UserRole.USER))
            }

            val nombre = document.getString("nombre") ?: email.substringBefore("@")
            val rolString = document.getString("rol") ?: UserRole.USER.name

            val rol = try {
                UserRole.valueOf(rolString)
            } catch (ex: Exception) {
                UserRole.USER
            }

            Log.d(TAG, "✅ Login usuario exitoso: $nombre")
            Result.success(User(userId, nombre, email, rol))

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error al acceder a Firestore", e)
            // Si falla Firestore, crear usuario con datos básicos
            val nombre = email.substringBefore("@")
            Log.d(TAG, "Usando datos básicos: $nombre")
            Result.success(User(userId, nombre, email, UserRole.USER))
        }
    }

    /**
     * Obtener todos los usuarios (solo para admin)
     */
    suspend fun getAllUsers(): List<User> {
        return try {
            Log.d(TAG, "=== OBTENIENDO LISTA DE USUARIOS ===")

            val snapshot = firestore.collection("usuarios")
                .get()
                .await()

            Log.d(TAG, "Documentos encontrados: ${snapshot.size()}")

            val users = mutableListOf<User>()

            for (document in snapshot.documents) {
                try {
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
                    Log.d(TAG, "Usuario agregado: $nombre ($email)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar documento: ${document.id}", e)
                }
            }

            Log.d(TAG, "✅ Total de usuarios obtenidos: ${users.size}")
            users.sortedBy { it.nombre }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener usuarios", e)
            emptyList()
        }
    }

    /**
     * Actualizar usuario
     */
    suspend fun updateUser(userId: String, newNombre: String): Result<User> {
        return try {
            Log.d(TAG, "=== ACTUALIZANDO USUARIO ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Nuevo nombre: $newNombre")

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
            Log.d(TAG, "✅ Usuario actualizado exitosamente")
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al actualizar usuario", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar usuario
     */
    suspend fun deleteUser(userId: String, userEmail: String): Result<Boolean> {
        return try {
            if (userEmail.equals(ADMIN_EMAIL, ignoreCase = true)) {
                Log.w(TAG, "Intento de eliminar al administrador")
                return Result.failure(Exception("No se puede eliminar al administrador"))
            }

            Log.d(TAG, "=== ELIMINANDO USUARIO ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Email: $userEmail")

            firestore.collection("usuarios")
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "✅ Usuario eliminado exitosamente")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar usuario", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener usuario actual
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val userId = auth.currentUser?.uid
            val userEmail = auth.currentUser?.email

            if (userId == null || userEmail == null) {
                Log.d(TAG, "No hay usuario autenticado actualmente")
                return null
            }

            Log.d(TAG, "=== OBTENIENDO USUARIO ACTUAL ===")
            Log.d(TAG, "UID: $userId")
            Log.d(TAG, "Email: $userEmail")

            // Verificar si es admin
            val isAdmin = userEmail.equals(ADMIN_EMAIL, ignoreCase = true)
            Log.d(TAG, "¿Es admin?: $isAdmin")

            try {
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
                        if (isAdmin) UserRole.ADMIN else UserRole.USER
                    }

                    Log.d(TAG, "✅ Usuario actual: $nombre ($rol)")
                    User(userId, nombre, userEmail, rol)
                } else {
                    // Si es admin y no tiene documento, crearlo
                    if (isAdmin) {
                        Log.d(TAG, "Creando documento de admin automáticamente")
                        val adminData = hashMapOf(
                            "nombre" to "Administrador",
                            "email" to userEmail,
                            "rol" to UserRole.ADMIN.name
                        )

                        firestore.collection("usuarios")
                            .document(userId)
                            .set(adminData)
                            .await()

                        User(userId, "Administrador", userEmail, UserRole.ADMIN)
                    } else {
                        Log.w(TAG, "Documento no encontrado para usuario actual")
                        null
                    }
                }
            } catch (firestoreError: Exception) {
                Log.e(TAG, "Error al obtener usuario de Firestore", firestoreError)
                // Si falla Firestore pero hay usuario autenticado
                if (isAdmin) {
                    User(userId, "Administrador", userEmail, UserRole.ADMIN)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener usuario actual", e)
            null
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        Log.d(TAG, "=== CERRANDO SESIÓN ===")
        Log.d(TAG, "Usuario antes de logout: ${auth.currentUser?.email}")
        auth.signOut()
        Log.d(TAG, "✅ Sesión cerrada")
    }

    /**
     * Verificar si hay usuario logueado
     */
    fun isLoggedIn(): Boolean {
        val isLogged = auth.currentUser != null
        Log.d(TAG, "¿Usuario logueado?: $isLogged")
        if (isLogged) {
            Log.d(TAG, "Usuario: ${auth.currentUser?.email}")
        }
        return isLogged
    }

    /**
     * Método de prueba de conexión
     */
    suspend fun testConnection(): String {
        return try {
            Log.d(TAG, "=== PRUEBA DE CONEXIÓN ===")

            // Prueba 1: Firebase Auth
            val authUser = auth.currentUser
            val authStatus = if (authUser != null) {
                "✅ Auth OK (${authUser.email})"
            } else {
                "⚠️ No hay usuario autenticado"
            }
            Log.d(TAG, "Auth: $authStatus")

            // Prueba 2: Firestore
            Log.d(TAG, "Probando Firestore...")
            val testDoc = firestore.collection("test")
                .document("connection")
                .get()
                .await()

            Log.d(TAG, "✅ Firestore OK")

            "$authStatus\n✅ Firestore OK"

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en prueba de conexión", e)
            "❌ Error: ${e.message}"
        }
    }
}