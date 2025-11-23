package com.example.recetapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.User
import com.example.recetapp.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = FirebaseAuthRepository()

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _validationError = MutableLiveData<String>()
    val validationError: LiveData<String> = _validationError

    private val _updateResult = MutableLiveData<Result<User>>()
    val updateResult: LiveData<Result<User>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String, rememberMe: Boolean) {
        when {
            email.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu correo electrónico"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _validationError.value = "Por favor ingresa un correo válido"
                return
            }
            password.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu contraseña"
                return
            }
            password.length < 6 -> {
                _validationError.value = "La contraseña debe tener al menos 6 caracteres"
                return
            }
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Iniciando login...")
                val result = authRepository.loginUser(email, password)
                _loginResult.value = result

                result.onSuccess { user ->
                    Log.d("AuthViewModel", "✅ Login exitoso: ${user.nombre} (${user.rol})")
                }.onFailure { error ->
                    Log.e("AuthViewModel", "❌ Login fallido: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado en login", e)
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(nombre: String, email: String, password: String, confirmarPassword: String) {
        when {
            nombre.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu nombre completo"
                return
            }
            nombre.length < 3 -> {
                _validationError.value = "El nombre debe tener al menos 3 caracteres"
                return
            }
            email.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu correo electrónico"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _validationError.value = "Por favor ingresa un correo válido"
                return
            }
            password.isEmpty() -> {
                _validationError.value = "Por favor ingresa una contraseña"
                return
            }
            password.length < 6 -> {
                _validationError.value = "La contraseña debe tener al menos 6 caracteres"
                return
            }
            password != confirmarPassword -> {
                _validationError.value = "Las contraseñas no coinciden"
                return
            }
        }

        _isLoading.value = true
        Log.d("AuthViewModel", "Iniciando registro...")

        viewModelScope.launch {
            try {
                val result = authRepository.registerUser(nombre, email, password)
                _registerResult.value = result

                result.onSuccess { user ->
                    Log.d("AuthViewModel", "✅ Registro exitoso: ${user.nombre}")
                }.onFailure { error ->
                    Log.e("AuthViewModel", "❌ Registro fallido: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado en registro", e)
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUser(userId: String, newNombre: String) {
        if (newNombre.isEmpty()) {
            _validationError.value = "El nombre no puede estar vacío"
            return
        }

        if (newNombre.length < 3) {
            _validationError.value = "El nombre debe tener al menos 3 caracteres"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.updateUser(userId, newNombre)
                _updateResult.value = result
                if (result.isSuccess) {
                    loadAllUsers()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(userId: String, userEmail: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.deleteUser(userId, userEmail)
                _deleteResult.value = result
                if (result.isSuccess) {
                    loadAllUsers()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllUsers() {
        Log.d("AuthViewModel", "Cargando todos los usuarios...")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val users = authRepository.getAllUsers()
                Log.d("AuthViewModel", "Usuarios cargados: ${users.size}")
                _allUsers.value = users
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cargar usuarios", e)
                _allUsers.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _currentUser.value = user
                Log.d("AuthViewModel", "Usuario actual: ${user?.nombre} (${user?.rol})")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cargar usuario actual", e)
                _currentUser.value = null
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }
}