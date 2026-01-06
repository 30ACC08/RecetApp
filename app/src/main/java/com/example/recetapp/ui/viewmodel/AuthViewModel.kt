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

    // Estados de UI
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Result<Boolean>>()
    val resetPasswordResult: LiveData<Result<Boolean>> = _resetPasswordResult

    private val _validationError = MutableLiveData<String>()
    val validationError: LiveData<String> = _validationError

    private val _generalError = MutableLiveData<String>()
    val generalError: LiveData<String> = _generalError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _updateResult = MutableLiveData<Result<User>>()
    val updateResult: LiveData<Result<User>> = _updateResult

    // Admin
    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    // Social
    private val _userStats = MutableLiveData<Pair<Int, Int>>()
    val userStats: LiveData<Pair<Int, Int>> = _userStats

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _followingList = MutableLiveData<List<User>>()
    val followingList: LiveData<List<User>> = _followingList

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    init {
        loadCurrentUser()
    }

    // Login
    fun login(email: String, pass: String, rememberMe: Boolean) {
        if (email.isBlank() || pass.isBlank()) {
            _validationError.value = "Completa todos los campos"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.loginUser(email, pass)
            _loginResult.value = result
            if (result.isSuccess) loadCurrentUser()
            _isLoading.value = false
        }
    }

    // Registro
    fun register(nombre: String, email: String, pass: String, confirmar: String) {
        if (nombre.isBlank() || email.isBlank() || pass.isBlank()) {
            _validationError.value = "Completa todos los campos"
            return
        }
        if (pass != confirmar) {
            _validationError.value = "Las contraseñas no coinciden"
            return
        }
        if (pass.length < 6) {
            _validationError.value = "Mínimo 6 caracteres"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.registerUser(nombre, email, pass)
            _registerResult.value = result
            if (result.isSuccess) loadCurrentUser()
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _validationError.value = "Ingresa tu correo"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _resetPasswordResult.value = authRepository.sendPasswordResetEmail(email)
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
            loadUserStats()
        }
    }

    // Perfil
    fun updateUserProfile(userId: String, nombre: String, imageUri: Uri?) {
        _isLoading.value = true
        viewModelScope.launch {
            var photoUrl: String? = null
            if (imageUri != null) {
                val uploadRes = authRepository.uploadProfileImage(imageUri)
                if (uploadRes.isSuccess) photoUrl = uploadRes.getOrNull()
            }
            val result = authRepository.updateUser(userId, nombre, photoUrl)
            _updateResult.value = result
            loadCurrentUser()
            _isLoading.value = false
        }
    }

    // Admin
    fun loadAllUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            authRepository.getAllUsers().onSuccess { _allUsers.value = it }
            _isLoading.value = false
        }
    }

    fun updateUser(userId: String, newNombre: String) {
        viewModelScope.launch {
            authRepository.updateUser(userId, newNombre, null).onSuccess { loadAllUsers() }
        }
    }

    fun deleteUser(userId: String, email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            authRepository.deleteUser(userId, email).onSuccess {
                _deleteResult.value = Result.success(true)
                loadAllUsers()
            }
            _isLoading.value = false
        }
    }

    // Social
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
            loadUserStats()
            checkIfFollowing(targetUserId)
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
        _isLoading.value = true
        viewModelScope.launch {
            authRepository.getFollowingUsers(uid).onSuccess { _followingList.value = it }
            _isLoading.value = false
        }
    }

    fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.getNotifications(uid).onSuccess { _notifications.value = it }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.markNotificationAsRead(uid, notificationId)
            loadNotifications()
        }
    }
}