package com.example.cteus.data.api

import com.example.cteus.data.model.ApiResponse
import com.example.cteus.data.model.AttemptAnswersData
import com.example.cteus.data.model.AttemptAnalysisData
import com.example.cteus.data.model.DeleteAttemptData
import com.example.cteus.data.model.DeleteExamData
import com.example.cteus.data.model.ExamAnalysisData
import com.example.cteus.data.model.ExamDetail
import com.example.cteus.data.model.ExamListData
import com.example.cteus.data.model.HistoryExamData
import com.example.cteus.data.model.SaveAnswerRequest
import com.example.cteus.data.model.SaveAnswerResponse
import com.example.cteus.data.model.StartExamData
import com.example.cteus.data.model.SubmitExamResponse
import com.example.cteus.data.model.UpdateTitleData
import com.example.cteus.data.model.UpdateTitleRequest
import com.example.cteus.data.model.UserLearningAnalysisData
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ExamService {
    @GET("api/v1/exam/list/")
    suspend fun getExamList(): ApiResponse<ExamListData>

    @GET("api/v1/exam/{exam_id}/")
    suspend fun getExamDetail(@Path("exam_id") examId: Int): ApiResponse<ExamDetail>

    @PUT("api/v1/exam/{exam_id}/update-title/")
    suspend fun updateExamTitle(
        @Path("exam_id") examId: Int,
        @Body request: UpdateTitleRequest
    ): ApiResponse<UpdateTitleData>

    @DELETE("api/v1/exam/{exam_id}/delete/")
    suspend fun deleteExam(@Path("exam_id") examId: Int): ApiResponse<DeleteExamData>

    @POST("api/v1/exam/{exam_id}/start/")
    suspend fun startExam(@Path("exam_id") examId: Int, @Body request: Map<String, String> = emptyMap()): ApiResponse<StartExamData>

    @DELETE("api/v1/exam/attempt/{attempt_id}/")
    suspend fun deleteAttempt(@Path("attempt_id") attemptId: Int): ApiResponse<DeleteAttemptData>

    @PUT("api/v1/exam/attempt/{attempt_id}/answer/")
    suspend fun saveAnswer(
        @Path("attempt_id") attemptId: Int,
        @Body request: SaveAnswerRequest
    ): ApiResponse<SaveAnswerResponse>

    @POST("api/v1/exam/attempt/{attempt_id}/submit/")
    suspend fun submitExam(
        @Path("attempt_id") attemptId: Int,
        @Body request: SaveAnswerRequest
    ): ApiResponse<SubmitExamResponse>

    @GET("api/v1/exam/attempt/{attempt_id}/answers/")
    suspend fun getAttemptAnswers(
        @Path("attempt_id") attemptId: Int
    ): ApiResponse<AttemptAnswersData>

    @GET("api/v1/exam/{exam_id}/history/")
    suspend fun getExamHistory(
        @Path("exam_id") examId: Int
    ): ApiResponse<HistoryExamData>

    @GET("api/v1/exam/attempt/{attempt_id}/analysis/")
    suspend fun getAttemptAnalysis(
        @Path("attempt_id") attemptId: Int
    ): ApiResponse<AttemptAnalysisData>

    @GET("api/v1/exam/{exam_id}/analysis/")
    suspend fun getExamAnalysis(
        @Path("exam_id") examId: Int
    ): ApiResponse<ExamAnalysisData>

    @GET("api/v1/exam/user/learning-analysis/")
    suspend fun getUserLearningAnalysis(): ApiResponse<UserLearningAnalysisData>
}
