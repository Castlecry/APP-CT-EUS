package com.example.cteus.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val nickname: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val gender: Int,
    val bio: String?,
    @SerializedName("created_at") val createdAt: String,
    val phone: String?,
    val email: String?
)

data class RegisterRequest(
    val username: String,
    val password: String
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: Int,
    val username: String,
    val nickname: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("create_time") val createTime: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("user_id") val userId: Int,
    val username: String
)

data class UpdateProfileRequest(
    val nickname: String?,
    val gender: Int?,
    val bio: String?,
    val phone: String?,
    val email: String?
)

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class ErrorDetail(
    val detail: String
)
