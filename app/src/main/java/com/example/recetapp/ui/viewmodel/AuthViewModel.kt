package com.example.recetapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recetapp.data.local.SessionManager
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.data.repository.AuthRepository

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val sessionManager = SessionManager(application)

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

        val result = authRepository.loginUser(email, password)
        if (result.isSuccess) {
            val user = result.getOrNull()
            user?.let {
                sessionManager.saveLoginSession(it.id, it.nombre, it.email, it.rol, rememberMe)
            }
        }
        _loginResult.value = result
    }

    fun register(nombre: String, email: String, password: String, confirmarPassword: String): Boolean {
        when {
            nombre.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu nombre completo"
                return false
            }
            email.isEmpty() -> {
                _validationError.value = "Por favor ingresa tu correo electrónico"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _validationError.value = "Por favor ingresa un correo válido"
                return false
            }
            password.isEmpty() -> {
                _validationError.value = "Por favor ingresa una contraseña"
                return false
            }
            password.length < 8 -> {
                _validationError.value = "La contraseña debe tener al menos 8 caracteres"
                return false
            }
            password != confirmarPassword -> {
                _validationError.value = "Las contraseñas no coinciden"
                return false
            }
        }

        val result = authRepository.registerUser(nombre, email, password)
        _registerResult.value = result
        return result.isSuccess
    }

    fun updateUser(email: String, newNombre: String, newPassword: String?) {
        if (newNombre.isEmpty()) {
            _validationError.value = "El nombre no puede estar vacío"
            return
        }

        if (newPassword != null && newPassword.length < 8) {
            _validationError.value = "La contraseña debe tener al menos 8 caracteres"
            return
        }

        val result = authRepository.updateUser(email, newNombre, newPassword)
        _updateResult.value = result
    }

    fun deleteUser(email: String) {
        val result = authRepository.deleteUser(email)
        _deleteResult.value = result
    }

    fun getAllUsers(): List<User> {
        return authRepository.getAllUsers()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    fun isAdmin(): Boolean {
        return sessionManager.isAdmin()
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun getCurrentUserName(): String? {
        return sessionManager.getUserName()
    }

    fun getCurrentUserEmail(): String? {
        return sessionManager.getUserEmail()
    }

    fun getCurrentUserRole(): UserRole {
        return sessionManager.getUserRole()
    }
}