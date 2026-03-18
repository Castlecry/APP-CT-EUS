package com.example.cteus.data.model

import com.google.gson.annotations.SerializedName

data class CaseUploadApplyRequest(
    @SerializedName("case_name") val caseName: String,
    val description: String,
    @SerializedName("file_extension") val fileExtension: String
)

data class CaseUploadApplyResponse(
    val code: Int,
    val message: String,
    val data: CaseUploadApplyData
)

data class CaseUploadApplyData(
    @SerializedName("case_id") val caseId: Int,
    @SerializedName("upload_url") val uploadUrl: String
)

data class CaseUploadCompleteRequest(
    @SerializedName("case_id") val caseId: Int,
    val status: Int = 1
)

data class CaseUploadCompleteResponse(
    val code: Int,
    val message: String,
    val data: Any?
)

data class CaseItem(
    @SerializedName("case_id") val caseId: Int,
    @SerializedName("case_name") val caseName: String,
    val description: String,
    @SerializedName("created_at") val createdAt: String
)

data class CaseListResponse(
    val code: Int,
    val message: String,
    val data: List<CaseItem>
)

// 3D 模型相关模型
data class CaseDetailResponse(
    val code: Int,
    val message: String,
    val data: CaseDetailData
)

data class CaseDetailData(
    @SerializedName("case_id") val caseId: Int,
    @SerializedName("case_name") val caseName: String,
    val description: String,
    val status: Int,
    @SerializedName("status_message") val statusMessage: String? = null,
    @SerializedName("created_at") val createdAt: String,
    val models: List<ModelItem>
)

data class ModelItem(
    @SerializedName("model_type") val modelType: String,
    @SerializedName("glb_url") val glbUrl: String,
    @SerializedName("json_url") val jsonUrl: String,
    @SerializedName("color_config") val colorConfig: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("is_visible") var isVisible: Boolean = true
)

data class OrganPoints(
    val organ: String,
    @SerializedName("point_count") val pointCount: Int,
    val points: List<List<Double>>
)
