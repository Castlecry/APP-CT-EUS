package com.example.cteus.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.model.*
import com.example.cteus.ui.screens.createTempFileFromUri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

class AIViewModel : ViewModel() {
    private val aiService = RetrofitClient.aiService
    private val gson = Gson()

    private val _sessions = MutableStateFlow<List<AISession>>(emptyList())
    val sessions: StateFlow<List<AISession>> = _sessions

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId

    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages: StateFlow<List<AIMessage>> = _messages

    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList())
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = aiService.getSessionList()
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessions.value = response.body()?.data?.sessions ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "获取列表异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSession(onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = aiService.createSession()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val newId = response.body()!!.data!!.sessionId
                    fetchSessions()
                    onSuccess(newId)
                }
            } catch (e: Exception) {
                _error.value = "创建异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchHistory(sessionId: Int?) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = aiService.getSessionHistory(sessionId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _messages.value = response.body()?.data?.messages ?: emptyList()
                    _currentSessionId.value = response.body()?.data?.sessionId
                } else if (sessionId == null) {
                    _messages.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "获取历史异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSelectedFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value + uri
    }

    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value - uri
    }

    fun sendMessage(context: Context, content: String) {
        val sessionId = _currentSessionId.value ?: return
        if (content.isBlank() && _selectedFiles.value.isEmpty()) return

        val localImageUris = _selectedFiles.value.map { it.toString() }

        viewModelScope.launch {
            _isSending.value = true
            try {
                var imageKeys: List<String>? = null
                
                if (_selectedFiles.value.isNotEmpty()) {
                    val fileNames = _selectedFiles.value.map { uri ->
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            cursor.getString(nameIndex)
                        } ?: "file_${System.currentTimeMillis()}"
                    }

                    val uploadUrlResponse = aiService.getUploadUrls(AIUploadUrlRequest(sessionId, fileNames))
                    if (uploadUrlResponse.isSuccessful && uploadUrlResponse.body()?.code == 0) {
                        val uploadInfos = uploadUrlResponse.body()!!.data!!.uploadInfoList
                        val keys = mutableListOf<String>()
                        for (i in _selectedFiles.value.indices) {
                            val file = context.createTempFileFromUri(_selectedFiles.value[i])
                            val res = aiService.uploadToOss(uploadInfos[i].uploadUrl, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                            if (res.isSuccessful) keys.add(uploadInfos[i].objectKey)
                        }
                        imageKeys = keys
                    }
                }

                // 2. 乐观更新用户消息（包含图片预览）
                val userMsg = AIMessage(sessionId, -1, "user", content, localImageUris, null, "")
                _messages.value = _messages.value + userMsg
                _selectedFiles.value = emptyList()

                // 3. 准备 AI 消息占位
                val aiPlaceholder = AIMessage(sessionId, -2, "assistant", "", null, null, "")
                _messages.value = _messages.value + aiPlaceholder

                // 4. 调用流式接口
                val response = aiService.sendMessageStream(sessionId, AISendMessageRequest(content, imageKeys))
                
                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.IO) {
                        val reader = response.body()!!.byteStream().bufferedReader()
                        var accumulatedContent = ""
                        var imageUrls: List<String>? = null
                        
                        reader.useLines { lines ->
                            lines.forEach { line ->
                                if (line.startsWith("data:")) {
                                    val data = line.substringAfter("data:").trim()
                                    try {
                                        val chunk = gson.fromJson(data, AIResponseChunk::class.java)
                                        if (chunk.chunkType == "content") {
                                            accumulatedContent += chunk.content
                                            updateLastMessageContent(accumulatedContent)
                                        }
                                        if (chunk.imageUrls != null && chunk.imageUrls.isNotEmpty()) {
                                            imageUrls = chunk.imageUrls
                                            updateLastMessageImages(imageUrls)
                                        }
                                    } catch (e: Exception) { }
                                }
                            }
                        }
                    }
                    fetchSessions() 
                } else {
                    _error.value = "连接 AI 失败"
                }
            } catch (e: Exception) {
                _error.value = "操作异常: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }

    private suspend fun updateLastMessageContent(newContent: String) {
        withContext(Dispatchers.Main) {
            val currentList = _messages.value.toMutableList()
            if (currentList.isNotEmpty() && currentList.last().role == "assistant") {
                val last = currentList.last()
                currentList[currentList.size - 1] = last.copy(content = newContent)
                _messages.value = currentList
            }
        }
    }

    private suspend fun updateLastMessageImages(imageUrls: List<String>?) {
        withContext(Dispatchers.Main) {
            val currentList = _messages.value.toMutableList()
            if (currentList.isNotEmpty() && currentList.last().role == "assistant") {
                val last = currentList.last()
                currentList[currentList.size - 1] = last.copy(imageUrls = imageUrls)
                _messages.value = currentList
            }
        }
    }

    fun deleteSession(sessionId: Int, onActiveDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = aiService.deleteSession(AIDeleteSessionRequest(sessionId))
                if (response.isSuccessful && response.body()?.code == 0) {
                    if (_currentSessionId.value == sessionId) {
                        _currentSessionId.value = null
                        _messages.value = emptyList()
                        onActiveDeleted()
                    }
                    fetchSessions()
                }
            } catch (e: Exception) { }
        }
    }

    fun updateSessionTitle(sessionId: Int, newTitle: String) {
        viewModelScope.launch {
            aiService.updateSession(AIUpdateSessionRequest(sessionId, newTitle))
            fetchSessions()
        }
    }

    fun clearError() { _error.value = null }
}

data class AIResponseChunk(
    val chunk_type: String? = null,
    val content: String = "",
    @SerializedName("image_urls") val imageUrls: List<String>? = null,
    val tool_calls: String? = null,
    val is_complete: Boolean = false,
    val message_id: Int? = null
) {
    val chunkType: String? get() = chunk_type
}
