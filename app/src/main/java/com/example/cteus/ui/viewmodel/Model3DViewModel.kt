package com.example.cteus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.model.CaseDetailData
import com.example.cteus.data.model.CaseItem
import com.example.cteus.data.model.OrganPoints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Model3DViewModel : ViewModel() {
    private val _caseList = MutableStateFlow<List<CaseItem>>(emptyList())
    val caseList: StateFlow<List<CaseItem>> = _caseList

    private val _caseDetail = MutableStateFlow<CaseDetailData?>(null)
    val caseDetail: StateFlow<CaseDetailData?> = _caseDetail

    private val _organPointsList = MutableStateFlow<List<OrganPoints>>(emptyList())
    val organPointsList: StateFlow<List<OrganPoints>> = _organPointsList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun fetchCaseList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.caseService.getCaseList()
                if (response.isSuccessful) {
                    _caseList.value = response.body()?.data ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch cases: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchCaseDetail(caseId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _organPointsList.value = emptyList() // Reset points
            try {
                val response = RetrofitClient.caseService.getCaseDetail(caseId)
                if (response.isSuccessful) {
                    val detail = response.body()?.data
                    _caseDetail.value = detail
                    
                    if (detail != null && detail.status == 3) {
                        // Fetch JSON points for each model if available
                        val pointsList = mutableListOf<OrganPoints>()
                        detail.models.forEach { model ->
                            if (model.jsonUrl.isNotEmpty()) {
                                try {
                                    val pointsResponse = RetrofitClient.caseService.getOrganPoints(model.jsonUrl)
                                    if (pointsResponse.isSuccessful) {
                                        pointsResponse.body()?.let { pointsList.add(it) }
                                    }
                                } catch (e: Exception) {
                                    // Log or handle error for individual JSON fetch
                                }
                            }
                        }
                        _organPointsList.value = pointsList
                    }
                } else {
                    _errorMessage.value = "Failed to fetch case detail: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
