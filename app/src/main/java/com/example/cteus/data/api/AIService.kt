package com.example.cteus.data.api

import com.example.cteus.data.model.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AIService {
    // 修复 422 错误：显式传递一个空的 Map 作为 Body，确保 POST 请求有正文
    @POST("api/v1/ai/session/create/")
    suspend fun createSession(@Body emptyBody: Map<String, String> = emptyMap()): Response<ApiResponse<AICreateSessionResponse>>

    @GET("api/v1/ai/session/list/")
    suspend fun getSessionList(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<ApiResponse<AISessionListResponse>>

    @GET("api/v1/ai/session/message/history/")
    suspend fun getSessionHistory(
        @Query("session_id") sessionId: Int? = null
    ): Response<ApiResponse<AISessionHistoryResponse>>

    @POST("api/v1/ai/session/message/send/")
    @Streaming
    suspend fun sendMessageStream(
        @Query("session_id") sessionId: Int,
        @Body request: AISendMessageRequest
    ): Response<ResponseBody>

    @PUT("api/v1/ai/session/title/update/")
    suspend fun updateSession(
        @Body request: AIUpdateSessionRequest
    ): Response<ApiResponse<Unit>>

    @HTTP(method = "DELETE", path = "api/v1/ai/session/delete/", hasBody = true)
    suspend fun deleteSession(
        @Body request: AIDeleteSessionRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/v1/ai/session/message/image/upload-url/")
    suspend fun getUploadUrls(
        @Body request: AIUploadUrlRequest
    ): Response<ApiResponse<AIUploadUrlResponse>>

    @PUT
    suspend fun uploadToOss(@Url url: String, @Body file: RequestBody): Response<Unit>
}
