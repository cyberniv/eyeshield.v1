package com.example.myapplication4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UrlAnalysisViewModel(private val repository: UrlAnalysisRepository) : ViewModel() {

    private val _analysisState = MutableStateFlow<ApiResult<UrlAnalysisResponse>>(ApiResult.Idle)
    val analysisState: StateFlow<ApiResult<UrlAnalysisResponse>> = _analysisState.asStateFlow()

    fun analyseUrl(url: String) {
        viewModelScope.launch {
            _analysisState.value = ApiResult.Loading
            val result = repository.analyseUrl(url)
            _analysisState.value = result
        }
    }
}

// Add Idle state to ApiResult or handle initial state
// Modified ApiResult in UrlAnalysisModels.kt to include Idle
