package com.example.cteus.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.model.ExtractKnowledgeCardRequest
import com.example.cteus.data.model.KnowledgeCard
import com.example.cteus.data.model.UpdateKnowledgeCardRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KnowledgeCardViewModel : ViewModel() {
    private val TAG = "KnowledgeCardViewModel"

    private val _knowledgeCards = MutableStateFlow<List<KnowledgeCard>>(emptyList())
    val knowledgeCards: StateFlow<List<KnowledgeCard>> = _knowledgeCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedCard = MutableStateFlow<KnowledgeCard?>(null)
    val selectedCard: StateFlow<KnowledgeCard?> = _selectedCard.asStateFlow()

    fun loadKnowledgeCards() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.knowledgeCardService.getKnowledgeCardList()
                if (response.code == 0 && response.data != null) {
                    _knowledgeCards.value = response.data
                    Log.d(TAG, "Loaded ${response.data.size} knowledge cards")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load knowledge cards: ${e.message}")
                _error.value = "加载知识卡片列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCardDetail(cardId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.knowledgeCardService.getKnowledgeCardDetail(cardId)
                if (response.code == 0 && response.data != null) {
                    _selectedCard.value = response.data
                    Log.d(TAG, "Loaded card detail: ${response.data.title}")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load card detail: ${e.message}")
                _error.value = "加载知识卡片详情失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.knowledgeCardService.deleteKnowledgeCard(cardId)
                if (response.code == 0) {
                    _successMessage.value = "知识卡片删除成功"
                    _knowledgeCards.value = _knowledgeCards.value.filter { it.cardId != cardId }
                    Log.d(TAG, "Deleted card $cardId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete card: ${e.message}")
                _error.value = "删除知识卡片失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun extractKnowledgeCard(messageIds: List<Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.knowledgeCardService.extractKnowledgeCard(
                    ExtractKnowledgeCardRequest(messageIds = messageIds, importanceLevel = 0)
                )
                if (response.code == 0) {
                    _successMessage.value = "知识卡片提取成功"
                    onSuccess()
                    Log.d(TAG, "Extracted knowledge card")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract card: ${e.message}")
                _error.value = "提取知识卡片失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun updateCard(cardId: Int, title: String, content: String?, importanceLevel: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.knowledgeCardService.updateKnowledgeCard(
                    cardId,
                    UpdateKnowledgeCardRequest(title = title, content = content, importanceLevel = importanceLevel)
                )
                if (response.code == 0 && response.data != null) {
                    _successMessage.value = "知识卡片更新成功"
                    _selectedCard.value = response.data
                    _knowledgeCards.value = _knowledgeCards.value.map {
                        if (it.cardId == cardId) response.data else it
                    }
                    Log.d(TAG, "Updated card $cardId")
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update card: ${e.message}")
                _error.value = "更新知识卡片失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
