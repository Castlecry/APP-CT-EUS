package com.example.cteus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.local.UserSessionManager
import com.example.cteus.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = UserSessionManager(application)
    private val userService = RetrofitClient.userService

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            val token = sessionManager.accessToken.first()
            if (token != null) {
                RetrofitClient.setToken(token)
                _isLoggedIn.value = true
                fetchProfile()
            } else {
                _isLoggedIn.value = false
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = userService.login(username, password)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        sessionManager.saveSession(loginResponse.accessToken, loginResponse.username)
                        RetrofitClient.setToken(loginResponse.accessToken)
                        _isLoggedIn.value = true
                        fetchProfile()
                    }
                } else {
                    _error.value = "登录失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = userService.register(RegisterRequest(username, password))
                if (response.isSuccessful) {
                    // Registration success, maybe auto login or redirect to login screen
                    _error.value = "注册成功，请登录"
                } else {
                    _error.value = "注册失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val response = userService.getProfile()
                if (response.isSuccessful) {
                    _userProfile.value = response.body()?.data
                } else {
                    _error.value = "获取资料失败: ${response.body()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userService.logout()
            } catch (e: Exception) {
                // Ignore logout network error
            } finally {
                sessionManager.clearSession()
                RetrofitClient.setToken(null)
                _isLoggedIn.value = false
                _userProfile.value = null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
