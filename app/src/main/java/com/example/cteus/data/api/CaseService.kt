package com.example.cteus.data.api

import com.example.cteus.data.model.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface CaseService {
    @POST("api/v1/case/upload/apply/")
    suspend fun applyUpload(@Body request: CaseUploadApplyRequest): Response<CaseUploadApplyResponse>

    @POST("api/v1/case/upload/complete/")
    suspend fun completeUpload(@Body request: CaseUploadCompleteRequest): Response<CaseUploadCompleteResponse>

    @PUT
    suspend fun uploadToOss(@Url url: String, @Body file: RequestBody): Response<Unit>

    @GET("api/v1/case/list/")
    suspend fun getCaseList(): Response<CaseListResponse>

    @GET("api/v1/case/{case_id}/")
    suspend fun getCaseDetail(@Path("case_id") caseId: Int): Response<CaseDetailResponse>

    @GET
    suspend fun getOrganPoints(@Url url: String): Response<OrganPoints>

    @DELETE("api/v1/case/delete/{case_id}/")
    suspend fun deleteCase(@Path("case_id") caseId: Int): Response<Unit>
}
