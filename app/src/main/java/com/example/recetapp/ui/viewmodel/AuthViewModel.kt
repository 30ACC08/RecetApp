package com.example.recetapp.ui.viewmodel

import android.app.Application
import android.net.Uri
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

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    private val _generalError = MutableLiveData<String>()
    val generalError: LiveData<String> = _generalError

    private val _updateResult = MutableLiveData<Result<User>>()
    val updateResult: LiveData<Result<User>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    private val _resetPasswordResult = MutableLiveData<Result<Boolean>>()
    val resetPasswordResult: LiveData<Result<Boolean>> = _resetPasswordResult

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String, rememberMe: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _validationError.value = "Completa todos los campos"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _loginResult.value = authRepository.loginUser(email, password)
            _isLoading.value = false
        }
    }

    fun register(nombre: String, email: String, password: String, confirmar: String) {
        if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
            _validationError.value = "Completa todos los campos"
            return
        }
        if (password != confirmar) {
            _validationError.value = "Las contraseñas no coinciden"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _registerResult.value = authRepository.registerUser(nombre, email, password)
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _validationError.value = "Ingresa tu correo para recuperar la contraseña"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _resetPasswordResult.value = authRepository.sendPasswordResetEmail(email)
            _isLoading.value = false
        }
    }

    fun loadAllUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.getAllUsers()
            result.onSuccess { users ->
                _allUsers.value = users
            }.onFailure { e ->
                _allUsers.value = emptyList()
                _generalError.value = "Error cargando usuarios: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    // Actualización simple (solo nombre, usada por Admin)
    fun updateUser(userId: String, newNombre: String) {
        if (newNombre.length < 3) {
            _validationError.value = "Nombre muy corto"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.updateUser(userId, newNombre)
            _updateResult.value = result
            if (result.isSuccess) loadAllUsers()
            _isLoading.value = false
        }
    }

    // Actualización completa (Perfil propio con foto)
    fun updateUserProfile(userId: String, newNombre: String, imageUri: Uri?) {
        if (newNombre.length < 3) {
            _validationError.value = "El nombre es muy corto"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            var photoUrl: String? = null

            // 1. Si hay imagen, subirla
            if (imageUri != null) {
                val uploadResult = authRepository.uploadProfileImage(imageUri)
                uploadResult.onSuccess { url ->
                    photoUrl = url
                }.onFailure {
                    _isLoading.value = false
                    _validationError.value = "Error subiendo imagen: ${it.message}"
                    return@launch
                }
            }

            // 2. Actualizar datos
            val result = authRepository.updateUser(userId, newNombre, photoUrl)
            _updateResult.value = result

            // 3. Actualizar memoria local
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            }

            _isLoading.value = false
        }
    }

    fun deleteUser(userId: String, userEmail: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.deleteUser(userId, userEmail)
            _deleteResult.value = result
            if (result.isSuccess) loadAllUsers()
            _isLoading.value = false
        }
    }

    suspend fun loadCurrentUser() {
        _currentUser.value = authRepository.getCurrentUser()
    }

    fun isLoggedIn() = authRepository.isLoggedIn()
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }
}