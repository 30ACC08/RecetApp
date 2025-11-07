package com.example.recetapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.recetapp.data.model.UserRole

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_REMEMBER_ME = "remember_me"
    }

    fun saveLoginSession(userId: String, userName: String, userEmail: String, userRol: UserRole, rememberMe: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ROL, userRol.name)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserRole(): UserRole {
        val rolString = sharedPreferences.getString(KEY_USER_ROL, UserRole.USER.name)
        return try {
            UserRole.valueOf(rolString ?: UserRole.USER.name)
        } catch (e: Exception) {
            UserRole.USER
        }
    }

    fun isAdmin(): Boolean {
        return getUserRole() == UserRole.ADMIN
    }

    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clearSession() {
        sharedPreferences.edit().apply {
            clear()
            apply()
        }
    }
}