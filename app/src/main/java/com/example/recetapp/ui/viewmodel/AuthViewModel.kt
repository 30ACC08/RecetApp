package com.example.recetapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
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
        }

        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            _loginResult.value = result
        }
    }

    fun register(nombre: String, email: String, password: String, confirmarPassword: String) {
        when {
            nombre.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu nombre completo"
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

        viewModelScope.launch {
            val result = authRepository.registerUser(nombre, email, password)
            _registerResult.value = result
        }
    }

    fun updateUser(userId: String, newNombre: String) {
        if (newNombre.isEmpty()) {
            _validationError.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            val result = authRepository.updateUser(userId, newNombre)
            _updateResult.value = result
            if (result.isSuccess) {
                loadAllUsers()
            }
        }
    }

    fun deleteUser(userId: String, userEmail: String) {
        viewModelScope.launch {
            val result = authRepository.deleteUser(userId, userEmail)
            _deleteResult.value = result
            if (result.isSuccess) {
                loadAllUsers()
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            val users = authRepository.getAllUsers()
            _allUsers.value = users
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun logout() {
        authRepository.logout()
    }
}