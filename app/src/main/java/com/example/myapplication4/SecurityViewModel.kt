package com.example.myapplication4

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

sealed class ScanState {
    data object Idle : ScanState()
    data object Loading : ScanState()
    data class Success(
        val url: String,
        val analysis: BedrockResult,
        val vtStats: VtRawStats,
        val rawResponse: UrlAnalysisResponse
    ) : ScanState()
    data class Error(val message: String) : ScanState()
}

class SecurityViewModel(
    private val repository: UrlAnalysisRepository = UrlAnalysisRepository(RetrofitClient.apiService)
) : ViewModel() {

    private val _scanState = MutableLiveData<ScanState>(ScanState.Idle)
    val scanState: LiveData<ScanState> = _scanState

    private val _recentScans = MutableLiveData<List<RecentScanInfo>>(emptyList())
    val recentScans: LiveData<List<RecentScanInfo>> = _recentScans

    fun scanUrl(url: String) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isEmpty()) {
            _scanState.value = ScanState.Error("Enter a URL to scan")
            return
        }

        viewModelScope.launch {
            _scanState.value = ScanState.Loading

            when (val result = repository.analyseUrl(normalizedUrl)) {
                is ApiResult.Success -> {
                    val response = result.data
                    val analysis = repository.parseBedrockAnalysis(response.bedrockAnalysis)
                        ?: fallbackAnalysis(response)

                    val success = ScanState.Success(
                        url = response.url,
                        analysis = analysis,
                        vtStats = response.vtRawStats,
                        rawResponse = response
                    )

                    _scanState.value = success
                    addRecentScan(success)
                }

                is ApiResult.Error -> {
                    _scanState.value = ScanState.Error(result.message)
                }

                ApiResult.Idle -> {
                    _scanState.value = ScanState.Idle
                }

                ApiResult.Loading -> {
                    _scanState.value = ScanState.Loading
                }
            }
        }
    }

    fun reset() {
        _scanState.value = ScanState.Idle
    }

    fun removeRecentScan(url: String) {
        _recentScans.value = _recentScans.value.orEmpty().filterNot { it.url == url }
    }

    private fun addRecentScan(scan: ScanState.Success) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val newItem = RecentScanInfo(
            url = scan.url,
            verdict = scan.analysis.verdict.uppercase(),
            time = timestamp,
            confidence = "${scan.analysis.confidenceScore}%"
        )

        val updatedScans = buildList {
            add(newItem)
            addAll(_recentScans.value.orEmpty().filterNot { it.url == newItem.url })
        }.take(10)

        _recentScans.value = updatedScans
    }

    private fun fallbackAnalysis(response: UrlAnalysisResponse): BedrockResult {
        val vtStats = response.vtRawStats
        val maliciousSignals = vtStats.malicious + vtStats.suspicious
        val harmlessSignals = vtStats.harmless + vtStats.undetected

        val verdict = when {
            vtStats.malicious > 0 -> "Malicious"
            vtStats.suspicious > 0 -> "Suspicious"
            else -> "Safe"
        }

        val confidenceScore = when {
            maliciousSignals > 0 -> (60 + maliciousSignals * 10).coerceAtMost(99)
            harmlessSignals > 0 -> 80
            else -> 50
        }

        return BedrockResult(
            verdict = verdict,
            confidenceScore = confidenceScore,
            summary = response.bedrockAnalysis.ifBlank {
                "No structured AI summary was returned for this URL."
            }
        )
    }
}