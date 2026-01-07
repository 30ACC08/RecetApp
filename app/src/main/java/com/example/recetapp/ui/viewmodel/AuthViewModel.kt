package com.example.recetapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recetapp.data.model.Notification
import com.example.recetapp.data.model.User
import com.example.recetapp.data.repository.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = FirebaseAuthRepository()
    private val auth = FirebaseAuth.getInstance()

    // === ESTADOS DE AUTENTICACIÓN ===
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Result<Boolean>>()
    val resetPasswordResult: LiveData<Result<Boolean>> = _resetPasswordResult

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _updateResult = MutableLiveData<Result<User>>()
    val updateResult: LiveData<Result<User>> = _updateResult

    // === ESTADOS COMUNES ===
    private val _validationError = MutableLiveData<String>()
    val validationError: LiveData<String> = _validationError

    private val _generalError = MutableLiveData<String?>()
    val generalError: LiveData<String?> = _generalError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // === ESTADOS DE ADMINISTRADOR ===
    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    // === ESTADOS SOCIALES ===
    private val _userStats = MutableLiveData<Pair<Int, Int>>()
    val userStats: LiveData<Pair<Int, Int>> = _userStats

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _followingList = MutableLiveData<List<User>>()
    val followingList: LiveData<List<User>> = _followingList

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    init {
        if (auth.currentUser != null) {
            loadCurrentUser()
        }
    }

    // ================= FUNCIONES DE LOGIN / REGISTRO =================

    fun login(email: String, pass: String, rememberMe: Boolean) {
        if (email.isBlank() || pass.isBlank()) {
            _validationError.value = "Por favor, ingresa tu correo y contraseña."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.loginUser(email, pass)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                loadUserStats()
                _loginResult.value = result
            } else {
                _loginResult.value = result
                // Opcional: Podrías poner un mensaje más específico aquí si falla
                _validationError.value = "Credenciales incorrectas. Verifícalas e intenta de nuevo."
            }
            _isLoading.value = false
        }
    }

    fun register(nombre: String, email: String, pass: String, confirmar: String) {
        if (nombre.isBlank() || email.isBlank() || pass.isBlank()) {
            _validationError.value = "Necesitamos todos tus datos para crear la cuenta."
            return
        }
        if (pass != confirmar) {
            _validationError.value = "Las contraseñas no coinciden. Revísalas."
            return
        }
        if (pass.length < 6) {
            _validationError.value = "La contraseña debe ser más segura (mínimo 6 caracteres)."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.registerUser(nombre, email, pass)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _validationError.value = "No pudimos registrarte. Tal vez el correo ya existe."
            }
            _registerResult.value = result
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _validationError.value = "Escribe tu correo para enviarte el enlace."
            return
        }
        viewModelScope.launch {
            _resetPasswordResult.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            if (user != null) loadUserStats()
        }
    }

    fun updateUserProfile(userId: String, nombre: String, imageUri: Uri?) {
        _isLoading.value = true
        viewModelScope.launch {
            var photoUrl: String? = null
            if (imageUri != null) {
                val uploadRes = authRepository.uploadProfileImage(imageUri)
                photoUrl = uploadRes.getOrNull()
            }
            val result = authRepository.updateUser(userId, nombre, photoUrl)
            _updateResult.value = result
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _generalError.value = "¡Tu perfil ha sido actualizado!"
            } else {
                _generalError.value = "No pudimos actualizar tu perfil. Intenta más tarde."
            }
            _isLoading.value = false
        }
    }

    // ================= FUNCIONES DE ADMINISTRADOR =================

    fun loadAllUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            authRepository.getAllUsers()
                .onSuccess { _allUsers.value = it }
                .onFailure { _generalError.value = "Problemas al cargar la lista de usuarios." }
            _isLoading.value = false
        }
    }

    fun updateUser(userId: String, newNombre: String) {
        if (newNombre.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.updateUser(userId, newNombre, null)
            if (result.isSuccess) {
                loadAllUsers()
                _generalError.value = "Usuario actualizado correctamente."
            } else {
                _generalError.value = "Hubo un error al guardar los cambios."
            }
            _isLoading.value = false
        }
    }

    fun deleteUser(userId: String, email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.deleteUser(userId, email)
            _deleteResult.value = result
            if (result.isSuccess) {
                loadAllUsers()
                _generalError.value = "El usuario ha sido eliminado del sistema."
            } else {
                _generalError.value = "No se pudo eliminar. Verifica permisos o conexión."
            }
            _isLoading.value = false
        }
    }

    // ================= FUNCIONES SOCIALES =================

    fun loadUserStats() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.getUserStats(uid).onSuccess { _userStats.value = it }
        }
    }

    fun toggleFollow(targetUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (uid == targetUserId) return

        viewModelScope.launch {
            if (authRepository.isFollowing(uid, targetUserId)) {
                authRepository.unfollowUser(uid, targetUserId)
                _isFollowing.value = false
            } else {
                authRepository.followUser(uid, targetUserId)
                _isFollowing.value = true
            }
            checkIfFollowing(targetUserId)
            loadUserStats()
        }
    }

    fun checkIfFollowing(targetUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isFollowing.value = authRepository.isFollowing(uid, targetUserId)
        }
    }

    fun loadFollowingList() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.getFollowingUsers(uid).onSuccess { _followingList.value = it }
        }
    }

    fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.getNotifications(uid).onSuccess { _notifications.value = it }
        }
    }

    fun markNotificationAsRead(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.markNotificationAsRead(uid, notifId)
            loadNotifications()
        }
    }
}