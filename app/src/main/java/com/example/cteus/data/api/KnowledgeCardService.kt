package com.example.cteus.data.api

import com.example.cteus.data.model.ApiResponse
import com.example.cteus.data.model.DeleteKnowledgeCardData
import com.example.cteus.data.model.ExtractKnowledgeCardRequest
import com.example.cteus.data.model.ExtractKnowledgeCardResponse
import com.example.cteus.data.model.KnowledgeCard
import com.example.cteus.data.model.UpdateKnowledgeCardRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface KnowledgeCardService {
    @POST("api/v1/knowledge-cards/extract/")
    suspend fun extractKnowledgeCard(
        @Body request: ExtractKnowledgeCardRequest
    ): ApiResponse<ExtractKnowledgeCardResponse>

    @GET("api/v1/knowledge-cards/list/")
    suspend fun getKnowledgeCardList(): ApiResponse<List<KnowledgeCard>>

    @GET("api/v1/knowledge-cards/{card_id}/")
    suspend fun getKnowledgeCardDetail(
        @Path("card_id") cardId: Int
    ): ApiResponse<KnowledgeCard>

    @DELETE("api/v1/knowledge-cards/delete/{card_id}/")
    suspend fun deleteKnowledgeCard(
        @Path("card_id") cardId: Int
    ): ApiResponse<DeleteKnowledgeCardData>

    @PUT("api/v1/knowledge-cards/{card_id}/update/")
    suspend fun updateKnowledgeCard(
        @Path("card_id") cardId: Int,
        @Body request: UpdateKnowledgeCardRequest
    ): ApiResponse<KnowledgeCard>
}
