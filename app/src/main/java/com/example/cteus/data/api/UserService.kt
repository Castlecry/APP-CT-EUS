package com.example.cteus.data.api

import com.example.cteus.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    @POST("api/v1/user/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @FormUrlEncoded
    @POST("api/v1/user/login/")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("api/v1/user/profile/")
    suspend fun getProfile(): Response<ApiResponse<User>>

    @PATCH("api/v1/user/updateprofile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<User>>

    @Multipart
    @POST("api/v1/user/upload_avatar/")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponse<User>>

    @POST("api/v1/user/logout/")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
