package com.example.cteus.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExamViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _examList = MutableStateFlow<List<ExamPaper>>(emptyList())
    val examList: StateFlow<List<ExamPaper>> = _examList.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _examDetail = MutableStateFlow<ExamDetail?>(null)
    val examDetail: StateFlow<ExamDetail?> = _examDetail.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Any?>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, Any?>> = _userAnswers.asStateFlow()

    private val _examResult = MutableStateFlow<ExamResult?>(null)
    val examResult: StateFlow<ExamResult?> = _examResult.asStateFlow()

    private val _currentAttemptId = MutableStateFlow<Int?>(null)
    val currentAttemptId: StateFlow<Int?> = _currentAttemptId.asStateFlow()

    private val _startExamData = MutableStateFlow<StartExamData?>(null)
    val startExamData: StateFlow<StartExamData?> = _startExamData.asStateFlow()

    private val _historyExamData = MutableStateFlow<HistoryExamData?>(null)
    val historyExamData: StateFlow<HistoryExamData?> = _historyExamData.asStateFlow()

    private val _attemptAnalysisData = MutableStateFlow<AttemptAnalysisData?>(null)
    val attemptAnalysisData: StateFlow<AttemptAnalysisData?> = _attemptAnalysisData.asStateFlow()

    private val _examAnalysisData = MutableStateFlow<ExamAnalysisData?>(null)
    val examAnalysisData: StateFlow<ExamAnalysisData?> = _examAnalysisData.asStateFlow()

    private val _userLearningAnalysis = MutableStateFlow<UserLearningAnalysisData?>(null)
    val userLearningAnalysis: StateFlow<UserLearningAnalysisData?> = _userLearningAnalysis.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    companion object {
        private const val TAG = "ExamViewModel"
    }

    fun loadExamList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getExamList()
                if (response.code == 0 && response.data != null) {
                    _examList.value = response.data.papers
                    Log.d(TAG, "Loaded ${response.data.papers.size} exams")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load exam list: ${e.message}")
                _error.value = "加载试卷列表失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExamDetail(examId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _examResult.value = null
            _userAnswers.value = emptyMap()
            try {
                // 先检查是否有未完成的作答
                val startResponse = RetrofitClient.examService.startExam(examId)
                if (startResponse.code == 0 && startResponse.data != null) {
                    val startData = startResponse.data
                    _startExamData.value = startData
                    
                    if (startData.hasUnfinishedAttempt) {
                        // 有未完成的作答，等待用户选择
                        Log.d(TAG, "Found unfinished attempt ${startData.unfinishedAttemptId}")
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // 没有未完成的作答，继续加载试卷详情
                    _currentAttemptId.value = startData.attemptId
                } else {
                    _error.value = startResponse.message
                    _isLoading.value = false
                    return@launch
                }
                
                val response = RetrofitClient.examService.getExamDetail(examId)
                if (response.code == 0 && response.data != null) {
                    _examDetail.value = response.data
                    _startExamData.value = null  // 清除 startExamData 以显示答题界面
                    Log.d(TAG, "Loaded exam detail: ${response.data.examTitle}")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load exam detail: ${e.message}")
                _error.value = "加载试卷详情失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setAnswer(questionId: Int, answer: String) {
        val currentAnswers = _userAnswers.value.toMutableMap()
        currentAnswers[questionId] = answer
        _userAnswers.value = currentAnswers
    }

    fun submitExam() {
        val detail = _examDetail.value ?: return
        val answers = _userAnswers.value

        val results = mutableListOf<QuestionResult>()
        var correctCount = 0

        detail.sections.forEach { section ->
            section.questions.forEach { question ->
                val userAnswer = answers[question.questionId]
                val isCorrect = userAnswer == question.correctAnswer
                if (isCorrect) correctCount++

                results.add(
                    QuestionResult(
                        questionId = question.questionId,
                        userAnswer = userAnswer,
                        correctAnswer = question.correctAnswer,
                        isCorrect = isCorrect,
                        explanation = question.answerExplanation
                    )
                )
            }
        }

        val totalCount = results.size
        val earnedScore = (correctCount.toFloat() / totalCount * detail.totalScore).toInt()

        _examResult.value = ExamResult(
            examId = detail.examId,
            examTitle = detail.examTitle,
            totalScore = detail.totalScore,
            earnedScore = earnedScore,
            correctCount = correctCount,
            totalCount = totalCount,
            results = results
        )
    }

    fun clearExamDetail() {
        _examDetail.value = null
        _examResult.value = null
        _userAnswers.value = emptyMap()
        _currentAttemptId.value = null
        _startExamData.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearHistoryData() {
        _historyExamData.value = null
        _attemptAnalysisData.value = null
        _examAnalysisData.value = null
    }

    fun loadAttemptAnswers(attemptId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getAttemptAnswers(attemptId)
                if (response.code == 0 && response.data != null) {
                    val answersData = response.data
                    val answersMap = answersData.answers.associate { it.questionId to it.userAnswer }
                    _userAnswers.value = answersMap
                    Log.d(TAG, "Loaded ${answersData.answers.size} answers for attempt $attemptId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load attempt answers: ${e.message}")
                _error.value = "加载历史答案失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun continueExam(examId: Int, attemptId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentAttemptId.value = attemptId
            try {
                val answersResponse = RetrofitClient.examService.getAttemptAnswers(attemptId)
                if (answersResponse.code == 0 && answersResponse.data != null) {
                    val answersData = answersResponse.data
                    val answersMap = answersData.answers.associate { it.questionId to it.userAnswer }
                    _userAnswers.value = answersMap
                    Log.d(TAG, "Loaded ${answersData.answers.size} answers for attempt $attemptId")
                } else {
                    _error.value = answersResponse.message
                }

                val detailResponse = RetrofitClient.examService.getExamDetail(examId)
                if (detailResponse.code == 0 && detailResponse.data != null) {
                    _examDetail.value = detailResponse.data
                    _startExamData.value = null
                    Log.d(TAG, "Loaded exam detail for exam $examId")
                } else {
                    _error.value = detailResponse.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to continue exam: ${e.message}")
                _error.value = "继续作答失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startExam(examId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.startExam(examId)
                if (response.code == 0 && response.data != null) {
                    _startExamData.value = response.data
                    _currentAttemptId.value = response.data.attemptId
                    Log.d(TAG, "Started exam with attempt ${response.data.attemptId}")
                    
                    val detailResponse = RetrofitClient.examService.getExamDetail(examId)
                    if (detailResponse.code == 0 && detailResponse.data != null) {
                        _examDetail.value = detailResponse.data
                        _startExamData.value = null
                        Log.d(TAG, "Loaded exam detail for exam $examId")
                    } else {
                        _error.value = detailResponse.message
                    }
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start exam: ${e.message}")
                _error.value = "开始作答失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAttempt(attemptId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.deleteAttempt(attemptId)
                if (response.code == 0) {
                    Log.d(TAG, "Deleted attempt $attemptId")
                    onDeleted()
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete attempt: ${e.message}")
                _error.value = "删除作答记录失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExamHistory(examId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getExamHistory(examId)
                if (response.code == 0 && response.data != null) {
                    _historyExamData.value = response.data
                    Log.d(TAG, "Loaded history for exam $examId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load exam history: ${e.message}")
                _error.value = "加载作答历史失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAttemptAnalysis(attemptId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getAttemptAnalysis(attemptId)
                if (response.code == 0 && response.data != null) {
                    _attemptAnalysisData.value = response.data
                    Log.d(TAG, "Loaded analysis for attempt $attemptId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load attempt analysis: ${e.message}")
                _error.value = "加载测评分析失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExamAnalysis(examId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getExamAnalysis(examId)
                if (response.code == 0 && response.data != null) {
                    _examAnalysisData.value = response.data
                    Log.d(TAG, "Loaded exam analysis for exam $examId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load exam analysis: ${e.message}")
                _error.value = "加载综合分析失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserLearningAnalysis() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.examService.getUserLearningAnalysis()
                if (response.code == 0 && response.data != null) {
                    _userLearningAnalysis.value = response.data
                    Log.d(TAG, "Loaded user learning analysis")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user learning analysis: ${e.message}")
                _error.value = "加载用户综合分析失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAnswer(onSuccess: () -> Unit) {
        val attemptId = _currentAttemptId.value ?: return
        val answers = _userAnswers.value

        _isSaving.value = true
        _error.value = null

        val answerItems = answers.map { (questionId, answer) ->
            AnswerItem(questionId = questionId, userAnswer = answer)
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.examService.saveAnswer(
                    attemptId,
                    SaveAnswerRequest(answers = answerItems)
                )
                if (response.code == 0 && response.data != null) {
                    _successMessage.value = "答案已保存"
                    onSuccess()
                    Log.d(TAG, "Saved ${response.data.savedCount} answers")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save answer: ${e.message}")
                _error.value = "保存答案失败：${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun submitExam(onSuccess: (SubmitExamResponse) -> Unit, onValidationError: (String) -> Unit) {
        val attemptId = _currentAttemptId.value ?: return
        val detail = _examDetail.value ?: return
        val answers = _userAnswers.value

        val allQuestionIds = detail.sections.flatMap { it.questions.map { q -> q.questionId } }
        val unansweredQuestions = allQuestionIds.filter { 
            val answer = answers[it]
            answer == null || answer.toString().isBlank()
        }

        if (unansweredQuestions.isNotEmpty()) {
            onValidationError("第 ${unansweredQuestions.joinToString(", ")} 题还未作答")
            return
        }

        _isSubmitting.value = true
        _error.value = null

        val answerItems = answers.map { (questionId, answer) ->
            AnswerItem(questionId = questionId, userAnswer = answer)
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.examService.submitExam(
                    attemptId,
                    SaveAnswerRequest(answers = answerItems)
                )
                if (response.code == 0 && response.data != null) {
                    onSuccess(response.data)
                    Log.d(TAG, "Submitted exam")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit exam: ${e.message}")
                _error.value = "提交试卷失败：${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateExamTitle(examId: Int, newTitle: String) {
        val currentIndex = _examList.value.indexOfFirst { it.examId == examId }
        if (currentIndex != -1) {
            val currentList = _examList.value.toMutableList()
            currentList[currentIndex] = currentList[currentIndex].copy(examTitle = newTitle)
            _examList.value = currentList
        }
    }

    fun deleteExam(examId: Int) {
        val currentList = _examList.value.toMutableList()
        currentList.removeAll { it.examId == examId }
        _examList.value = currentList
    }
}
