package com.example.cteus.data.model

import com.google.gson.annotations.SerializedName

data class AISession(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("session_title") val sessionTitle: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("first_message") val firstMessage: String?
)

data class AISessionListResponse(
    val sessions: List<AISession>,
    val total: Int
)

data class AIMessage(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("message_id") val messageId: Int,
    val role: String, // "user" or "assistant"
    val content: String,
    @SerializedName("image_urls") val imageUrls: List<String>?, // 后端返回的具体 URL 列表
    @SerializedName("tool_calls") val toolCalls: String?,
    @SerializedName("created_at") val createdAt: String
)

data class AISessionHistoryResponse(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("session_title") val sessionTitle: String,
    val messages: List<AIMessage>
)

data class AICreateSessionResponse(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("session_title") val sessionTitle: String,
    @SerializedName("created_at") val createdAt: String
)

data class AISendMessageRequest(
    val content: String,
    @SerializedName("image_keys") val imageKeys: List<String>?
)

data class AIUpdateSessionRequest(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("session_title") val sessionTitle: String
)

data class AIDeleteSessionRequest(
    @SerializedName("session_id") val sessionId: Int
)

data class AIUploadUrlRequest(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("image_files") val imageFiles: List<String>
)

data class AIUploadUrlResponse(
    @SerializedName("upload_info_list") val uploadInfoList: List<AIUploadInfo>,
    @SerializedName("session_id") val sessionId: Int
)

data class AIUploadInfo(
    @SerializedName("original_name") val originalName: String,
    @SerializedName("object_key") val objectKey: String,
    @SerializedName("upload_url") val uploadUrl: String
)
