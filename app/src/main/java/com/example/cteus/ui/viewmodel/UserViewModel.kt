package com.example.cteus.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.local.UserSessionManager
import com.example.cteus.data.model.*
import com.example.cteus.ui.screens.createTempFileFromUri
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.net.SocketTimeoutException

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = UserSessionManager(application)
    private val userService = RetrofitClient.userService
    private val caseService = RetrofitClient.caseService

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _registrationSuccess = MutableSharedFlow<Boolean>()
    val registrationSuccess: SharedFlow<Boolean> = _registrationSuccess

    // 病例上传相关状态
    private val _isCaseUploading = MutableStateFlow(false)
    val isCaseUploading: StateFlow<Boolean> = _isCaseUploading

    private val _caseUploadStatus = MutableStateFlow("")
    val caseUploadStatus: StateFlow<String> = _caseUploadStatus

    // 病例列表状态
    private val _caseList = MutableStateFlow<List<CaseItem>>(emptyList())
    val caseList: StateFlow<List<CaseItem>> = _caseList

    private val _isCaseListLoading = MutableStateFlow(false)
    val isCaseListLoading: StateFlow<Boolean> = _isCaseListLoading

    companion object {
        private const val PROFILE_TIMEOUT = 20000L // 20秒超时
        private const val TAG = "UserViewModel"
    }

    init {
        // 修改：启动时不自动从 DataStore 恢复登录状态
        viewModelScope.launch {
            // 清理旧的 Session，确保每次重启都是未登录状态
            sessionManager.clearSession()
            RetrofitClient.setToken(null)
            _isLoggedIn.value = false
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
                        fetchProfileWithTimeout()
                    }
                } else {
                    _error.value = "登录失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    private fun fetchProfileWithTimeout() {
        viewModelScope.launch {
            try {
                withTimeout(PROFILE_TIMEOUT) {
                    val response = userService.getProfile()
                    if (response.isSuccessful) {
                        _userProfile.value = response.body()
                    } else {
                        val errorMsg = "获取资料失败: ${response.code()}"
                        _error.value = errorMsg
                        if (response.code() == 401 || response.code() == 403) {
                            logout()
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Profile fetch timed out")
                _error.value = "登录响应超时，请重新登录"
                logout() 
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Socket timeout during profile fetch")
                _error.value = "网络连接超时，请检查网络后重试"
                logout()
            } catch (e: Exception) {
                val msg = e.message ?: "未知网络错误"
                Log.e(TAG, "Error fetching profile: $msg")
                _error.value = "网络错误: $msg"
                if (msg.contains("timeout", ignoreCase = true)) {
                    logout()
                }
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = userService.register(RegisterRequest(username, password))
                if (response.isSuccessful) {
                    _error.value = "注册成功，请登录"
                    _registrationSuccess.emit(true)
                } else {
                    _error.value = "注册失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun fetchProfile() {
        fetchProfileWithTimeout()
    }

    fun updateProfile(nickname: String?, gender: Int?, bio: String?, phone: String?, email: String?) {
        viewModelScope.launch {
            try {
                val response = userService.updateProfile(UpdateProfileRequest(nickname, gender, bio, phone, email))
                if (response.isSuccessful) {
                    _userProfile.value = response.body()
                    _error.value = "资料更新成功"
                } else {
                    _error.value = "资料更新失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun uploadAvatar(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            try {
                val response = userService.uploadAvatar(imagePart)
                if (response.isSuccessful) {
                    _userProfile.value = response.body()
                    _error.value = "头像上传成功"
                } else {
                    _error.value = "头像上传失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            }
        }
    }

    fun uploadCase(context: Context, caseName: String, description: String, fileUri: Uri) {
        viewModelScope.launch {
            _isCaseUploading.value = true
            _caseUploadStatus.value = "正在申请上传权限..."
            
            try {
                val fileName = fileUri.path?.substringAfterLast("/") ?: "file.zip"
                val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ".zip"
                
                val applyResponse = caseService.applyUpload(CaseUploadApplyRequest(caseName, description, extension))
                
                if (applyResponse.isSuccessful && applyResponse.body()?.code == 0) {
                    val applyData = applyResponse.body()!!.data
                    _caseUploadStatus.value = "正在上传文件至服务器..."
                    
                    val file = context.createTempFileFromUri(fileUri)
                    val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    
                    val ossResponse = caseService.uploadToOss(applyData.uploadUrl, requestFile)
                    
                    if (ossResponse.isSuccessful) {
                        _caseUploadStatus.value = "文件上传成功，正在启动后台模型处理..."
                        
                        val completeResponse = caseService.completeUpload(CaseUploadCompleteRequest(applyData.caseId, 1))
                        
                        if (completeResponse.isSuccessful && completeResponse.body()?.code == 0) {
                            _error.value = "病例上传成功，后台已开始处理模型"
                            fetchCaseList() // 上传成功后刷新列表
                        } else {
                            _error.value = "通知处理失败: ${completeResponse.body()?.message}"
                        }
                    } else {
                        _error.value = "OSS 上传失败: ${ossResponse.message()}"
                    }
                } else {
                    _error.value = "权限申请失败: ${applyResponse.body()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "操作异常: ${e.message}"
            } finally {
                _isCaseUploading.value = false
                _caseUploadStatus.value = ""
            }
        }
    }

    fun fetchCaseList() {
        viewModelScope.launch {
            _isCaseListLoading.value = true
            try {
                val response = caseService.getCaseList()
                if (response.isSuccessful && response.body()?.code == 0) {
                    _caseList.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = "获取列表失败: ${response.body()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            } finally {
                _isCaseListLoading.value = false
            }
        }
    }

    fun deleteCase(caseId: Int) {
        viewModelScope.launch {
            try {
                val response = caseService.deleteCase(caseId)
                if (response.isSuccessful) {
                    fetchCaseList() // 删除成功后刷新列表
                    _error.value = "病例已成功删除"
                } else {
                    _error.value = "删除失败: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "操作异常: ${e.message}"
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
