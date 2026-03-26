package com.example.cteus.data.model

import com.google.gson.annotations.SerializedName

data class KnowledgeCard(
    @SerializedName("card_id") val cardId: Int,
    @SerializedName("session_id") val sessionId: Int,
    val title: String,
    val content: String,
    @SerializedName("image_keys") val imageKeys: List<String>?,
    @SerializedName("image_urls") val imageUrls: List<String>?,
    @SerializedName("importance_level") val importanceLevel: Int,
    @SerializedName("created_at") val createdAt: String
)

data class ExtractKnowledgeCardRequest(
    @SerializedName("message_ids") val messageIds: List<Int>,
    @SerializedName("importance_level") val importanceLevel: Int = 0
)

data class ExtractKnowledgeCardResponse(
    @SerializedName("card_id") val cardId: Int,
    @SerializedName("session_id") val sessionId: Int,
    val title: String,
    val content: String,
    @SerializedName("image_keys") val imageKeys: List<String>?,
    @SerializedName("image_urls") val imageUrls: List<String>?,
    @SerializedName("importance_level") val importanceLevel: Int,
    @SerializedName("created_at") val createdAt: String
)

data class DeleteKnowledgeCardData(
    @SerializedName("card_id") val cardId: Int,
    val message: String
)

data class UpdateKnowledgeCardRequest(
    val title: String,
    val content: String?,
    @SerializedName("importance_level") val importanceLevel: Int
)
