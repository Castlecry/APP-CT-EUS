package com.example.cteus.data.model

import com.google.gson.annotations.SerializedName

data class ExamPaper(
    @SerializedName("exam_id") val examId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("question_count") val questionCount: Int,
    @SerializedName("created_at") val createdAt: String
)

data class ExamListData(
    val papers: List<ExamPaper>,
    val total: Int
)

data class ExamDetail(
    @SerializedName("exam_id") val examId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("question_count") val questionCount: Int,
    val sections: List<ExamSection>,
    @SerializedName("created_at") val createdAt: String
)

data class ExamSection(
    @SerializedName("section_name") val sectionName: String,
    @SerializedName("section_score") val sectionScore: Int,
    val questions: List<ExamQuestion>
)

data class ExamQuestion(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("question_type") val questionType: String,
    @SerializedName("question_text") val questionText: String,
    val options: List<String>,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("answer_explanation") val answerExplanation: String
)

data class QuestionResult(
    val questionId: Int,
    val userAnswer: Any?,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String
)

data class ExamResult(
    val examId: Int,
    val examTitle: String,
    val totalScore: Int,
    val earnedScore: Int,
    val correctCount: Int,
    val totalCount: Int,
    val results: List<QuestionResult>
)

data class UpdateTitleRequest(
    @SerializedName("exam_title") val examTitle: String
)

data class UpdateTitleData(
    @SerializedName("exam_id") val examId: Int,
    val message: String
)

data class DeleteExamData(
    @SerializedName("exam_id") val examId: Int,
    val message: String
)

data class StartExamData(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("start_time") val startTime: String,
    val message: String,
    @SerializedName("has_unfinished_attempt") val hasUnfinishedAttempt: Boolean,
    @SerializedName("unfinished_attempt_id") val unfinishedAttemptId: Int?
)

data class DeleteAttemptData(
    @SerializedName("attempt_id") val attemptId: Int,
    val deleted: Boolean
)

data class AnswerItem(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("user_answer") val userAnswer: Any?
)

data class SaveAnswerRequest(
    val answers: List<AnswerItem>
)

data class SaveAnswerResponse(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("saved_count") val savedCount: Int
)

data class SubmitExamResponse(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    val status: String,
    val message: String
)

data class UserAnswerItem(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("user_answer") val userAnswer: Any?
)

data class AttemptAnswersData(
    @SerializedName("attempt_id") val attemptId: Int,
    val status: String,
    val answers: List<UserAnswerItem>
)

data class HistoryExamData(
    @SerializedName("exam_id") val examId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("max_score") val maxScore: Int,
    val sections: List<ExamSectionWithAnswer>,
    val attempts: List<ExamAttemptSummary>
)

data class ExamSectionWithAnswer(
    @SerializedName("section_name") val sectionName: String,
    @SerializedName("section_score") val sectionScore: Int,
    val questions: List<ExamQuestionWithAnswer>
)

data class ExamQuestionWithAnswer(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("question_type") val questionType: String,
    @SerializedName("question_text") val questionText: String,
    val options: List<String>?,
    @SerializedName("correct_answer") val correctAnswer: String?,
    @SerializedName("answer_explanation") val answerExplanation: String?,
    @SerializedName("user_answer") val userAnswer: Any?,
    val score: Int?,
    val analysis: String?
)

data class ExamAttemptSummary(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("user_id") val userId: Int,
    val status: String,
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("submit_time") val submitTime: String?,
    val answers: List<AttemptAnswerItem>?
)

data class AttemptAnswerItem(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("question_type") val questionType: String,
    @SerializedName("user_answer") val userAnswer: Any?,
    val score: Int,
    val analysis: String?
)

data class ExamAnalysisData(
    @SerializedName("exam_id") val examId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("score_statistics") val scoreStatistics: ScoreStatistics,
    @SerializedName("trend_analysis") val trendAnalysis: TrendAnalysis,
    @SerializedName("attempt_history") val attemptHistory: List<AttemptHistoryItem>,
    @SerializedName("stability_metrics") val stabilityMetrics: StabilityMetrics,
    @SerializedName("overall_assessment") val overallAssessment: String,
    @SerializedName("trend_description") val trendDescription: String,
    @SerializedName("key_weaknesses") val keyWeaknesses: List<String>,
    @SerializedName("learning_suggestions") val learningSuggestions: List<String>,
    @SerializedName("last_analysis_time") val lastAnalysisTime: String
)

data class ScoreStatistics(
    @SerializedName("total_attempts") val totalAttempts: Int,
    @SerializedName("best_score") val bestScore: Int,
    @SerializedName("worst_score") val worstScore: Int,
    @SerializedName("average_score") val averageScore: Int,
    @SerializedName("latest_score") val latestScore: Int,
    @SerializedName("std_deviation") val stdDeviation: Float,
    @SerializedName("improvement_rate") val improvementRate: Float
)

data class TrendAnalysis(
    @SerializedName("trend_direction") val trendDirection: String,
    val confidence: Float,
    val slope: Float,
    val volatility: String,
    @SerializedName("key_turning_points") val keyTurningPoints: List<String>
)

data class AttemptHistoryItem(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("attempt_number") val attemptNumber: Int,
    val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    @SerializedName("score_percentage") val scorePercentage: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("submit_time") val submitTime: String
)

data class StabilityMetrics(
    val reason: String,
    @SerializedName("score_range") val scoreRange: Int,
    @SerializedName("consistency_level") val consistencyLevel: String,
    @SerializedName("coefficient_of_variation") val coefficientOfVariation: Float
)

data class AttemptAnalysisData(
    @SerializedName("attempt_id") val attemptId: Int,
    @SerializedName("exam_id") val examId: Int,
    @SerializedName("exam_title") val examTitle: String,
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("max_score") val maxScore: Int,
    @SerializedName("score_percentage") val scorePercentage: Int,
    @SerializedName("score_breakdown") val scoreBreakdown: ScoreBreakdown,
    @SerializedName("time_metrics") val timeMetrics: TimeMetrics,
    @SerializedName("question_type_analysis") val questionTypeAnalysis: List<QuestionTypeAnalysis>,
    @SerializedName("ability_dimensions") val abilityDimensions: List<AbilityDimension>,
    @SerializedName("knowledge_mastery_matrix") val knowledgeMasteryMatrix: List<String>,
    val weaknesses: List<WeaknessItem>,
    @SerializedName("learning_suggestions") val learningSuggestions: List<LearningSuggestionItem>,
    @SerializedName("overall_assessment") val overallAssessment: String,
    @SerializedName("strength_analysis") val strengthAnalysis: String,
    @SerializedName("analysis_time") val analysisTime: String
)

data class ScoreBreakdown(
    @SerializedName("total_score") val totalScore: Int,
    @SerializedName("objective_score") val objectiveScore: Int,
    @SerializedName("subjective_score") val subjectiveScore: Int,
    @SerializedName("objective_max_score") val objectiveMaxScore: Int,
    @SerializedName("subjective_max_score") val subjectiveMaxScore: Int
)

data class TimeMetrics(
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("expected_duration_seconds") val expectedDurationSeconds: Int,
    @SerializedName("efficiency_ratio") val efficiencyRatio: Float,
    @SerializedName("time_efficiency_score") val timeEfficiencyScore: Int
)

data class QuestionTypeAnalysis(
    @SerializedName("question_type") val questionType: String,
    @SerializedName("question_count") val questionCount: Int,
    @SerializedName("correct_count") val correctCount: Int,
    @SerializedName("avg_score_rate") val avgScoreRate: Float,
    @SerializedName("accuracy_rate") val accuracyRate: Float,
    val reason: String
)

data class AbilityDimension(
    @SerializedName("dimension_name") val dimensionName: String,
    val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    val percentage: Int,
    val level: String,
    val reason: String
)

data class WeaknessItem(
    @SerializedName("weakness_id") val weaknessId: Int,
    val category: String,
    val description: String,
    val severity: String,
    @SerializedName("related_chapters") val relatedChapters: List<String>,
    @SerializedName("improvement_priority") val improvementPriority: Int
)

data class LearningSuggestionItem(
    @SerializedName("suggestion_id") val suggestionId: Int,
    val category: String,
    val priority: Int,
    val title: String,
    val description: String,
    @SerializedName("estimated_hours") val estimatedHours: Float,
    @SerializedName("related_weakness_ids") val relatedWeaknessIds: List<Int>
)

data class UserLearningAnalysisData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("total_exams_taken") val totalExamsTaken: Int,
    @SerializedName("total_questions_answered") val totalQuestionsAnswered: Int,
    @SerializedName("overall_average_score") val overallAverageScore: Int,
    @SerializedName("global_ranking_percentile") val globalRankingPercentile: Int,
    @SerializedName("ability_radar") val abilityRadar: List<UserAbilityDimension>,
    @SerializedName("subject_performance") val subjectPerformance: List<String>,
    @SerializedName("learning_behavior") val learningBehavior: LearningBehavior,
    @SerializedName("knowledge_system_mastery") val knowledgeSystemMastery: KnowledgeSystemMastery,
    val milestones: List<String>,
    @SerializedName("overall_assessment") val overallAssessment: String,
    @SerializedName("knowledge_summary") val knowledgeSummary: String,
    @SerializedName("learning_progress_description") val learningProgressDescription: String,
    @SerializedName("comprehensive_suggestions") val comprehensiveSuggestions: List<String>,
    @SerializedName("last_analysis_time") val lastAnalysisTime: String
)

data class UserAbilityDimension(
    @SerializedName("dimension_name") val dimensionName: String,
    val score: Float,
    @SerializedName("max_score") val maxScore: Float,
    val percentage: Int,
    val level: String,
    val reason: String
)

data class LearningBehavior(
    @SerializedName("total_study_hours") val totalStudyHours: Float,
    @SerializedName("average_session_duration") val averageSessionDuration: Int,
    @SerializedName("study_frequency") val studyFrequency: String,
    @SerializedName("persistence_score") val persistenceScore: Int,
    @SerializedName("engagement_level") val engagementLevel: String
)

data class KnowledgeSystemMastery(
    val summary: String,
    val coverage: Int,
    val strengths: List<String>,
    val weaknesses: List<String>
)
