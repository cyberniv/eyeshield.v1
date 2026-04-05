package com.example.myapplication4

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
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

    private val _lastRequestedUrl = MutableLiveData<String?>(null)
    val lastRequestedUrl: LiveData<String?> = _lastRequestedUrl

    private val _recentScans = MutableLiveData<List<RecentScanInfo>>(emptyList())
    val recentScans: LiveData<List<RecentScanInfo>> = _recentScans

    fun scanUrl(url: String) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isEmpty()) {
            _scanState.value = ScanState.Error("Enter a URL to scan")
            return
        }

        _lastRequestedUrl.value = normalizedUrl

        viewModelScope.launch {
            _scanState.value = ScanState.Loading

            when (val result = repository.analyseUrl(normalizedUrl)) {
                is ApiResult.Success -> {
                    val response = result.data
                    val analysis = classifyAnalysis(response)

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
                    _scanState.value = ScanState.Error("Scan failed. Try again")
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

    fun retryLastScan() {
        val lastUrl = _lastRequestedUrl.value?.trim().orEmpty()
        if (lastUrl.isNotBlank()) {
            scanUrl(lastUrl)
        }
    }

    fun removeRecentScan(url: String) {
        _recentScans.value = _recentScans.value.orEmpty().filterNot { it.url == url }
    }

    private fun addRecentScan(scan: ScanState.Success) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val totalEngines = totalPrimaryEngines(scan.vtStats)
        val newItem = RecentScanInfo(
            url = scan.url,
            verdict = scan.analysis.verdict.uppercase(),
            time = timestamp,
            confidence = "$totalEngines engines"
        )

        val updatedScans = buildList {
            add(newItem)
            addAll(_recentScans.value.orEmpty().filterNot { it.url == newItem.url })
        }.take(10)

        _recentScans.value = updatedScans
    }

    private fun classifyAnalysis(response: UrlAnalysisResponse): BedrockResult {
        val riskEvaluation = evaluateCyberRisk(
            url = response.url,
            vtStats = response.vtRawStats
        )

        return BedrockResult(
            verdict = riskEvaluation.verdict,
            confidenceScore = riskEvaluation.confidenceScore,
            summary = riskEvaluation.summary
        )
    }
}

data class CyberRiskEvaluation(
    val verdict: String,
    val riskScore: Int,
    val confidenceScore: Int,
    val title: String,
    val totalEngines: Int,
    val flaggedEngines: Int,
    val connectionMessage: String,
    val limitedDataMessage: String?,
    val isTrustedDomain: Boolean,
    val summary: String,
    val supportText: String,
    val recommendation: String,
    val reasonText: String
)

private val trustedDomains = setOf(
    "youtube.com",
    "google.com",
    "facebook.com",
    "microsoft.com",
    "apple.com"
)

fun evaluateCyberRisk(
    url: String,
    vtStats: VtRawStats
): CyberRiskEvaluation {
    val maliciousCount = vtStats.malicious
    val suspiciousCount = vtStats.suspicious
    val harmlessCount = vtStats.harmless
    val totalEngines = totalPrimaryEngines(vtStats)
    val flaggedCount = maliciousCount + suspiciousCount
    val parsedUri = runCatching {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        Uri.parse(normalized)
    }.getOrNull()
    val host = parsedUri?.host?.lowercase().orEmpty()
    val isTrustedDomain = trustedDomains.any { domain ->
        host == domain || host.endsWith(".$domain")
    }
    val isSecureConnection = parsedUri?.scheme.equals("https", ignoreCase = true)
    val limitedDataMessage = if (totalEngines < 5) "Limited data, results may vary" else null

    val verdict = when {
        isTrustedDomain && maliciousCount <= 1 -> "SAFE"
        maliciousCount >= 3 -> "HIGH_RISK"
        maliciousCount >= 1 -> "MEDIUM_RISK"
        suspiciousCount >= 2 -> "CAUTION"
        else -> "SAFE"
    }

    val title = when (verdict) {
        "HIGH_RISK" -> "Malicious Site"
        "MEDIUM_RISK" -> "Suspicious Site"
        "CAUTION" -> "Suspicious Activity"
        else -> "Site Appears Safe"
    }

    val summary = when {
        maliciousCount > 0 -> if (maliciousCount == 1) "1 engine flagged as malicious" else "$maliciousCount engines flagged as malicious"
        suspiciousCount > 0 -> if (suspiciousCount == 1) "1 suspicious indicator detected" else "$suspiciousCount suspicious indicators detected"
        harmlessCount > 0 -> if (harmlessCount == 1) "1 engine marked harmless" else "$harmlessCount engines marked harmless"
        totalEngines > 0 -> "$totalEngines engines analyzed"
        else -> "No VirusTotal data available"
    }

    val connectionMessage = if (isSecureConnection) {
        "HTTPS connection is secure"
    } else {
        "HTTP connection is not secure"
    }

    val supportText = when {
        limitedDataMessage != null -> limitedDataMessage
        isTrustedDomain && flaggedCount > 0 -> "Trusted domain override applied"
        else -> connectionMessage
    }

    val recommendation = when (verdict) {
        "HIGH_RISK" -> "This site was flagged by multiple security engines."
        "MEDIUM_RISK", "CAUTION" -> "Proceed with caution."
        else -> "Safe to continue."
    }

    val reasonText = when {
        limitedDataMessage != null -> limitedDataMessage
        isTrustedDomain && flaggedCount > 0 -> "Trusted domain override applied"
        else -> connectionMessage
    }

    return CyberRiskEvaluation(
        verdict = verdict,
        riskScore = flaggedCount,
        confidenceScore = totalEngines,
        title = title,
        totalEngines = totalEngines,
        flaggedEngines = flaggedCount,
        connectionMessage = connectionMessage,
        limitedDataMessage = limitedDataMessage,
        isTrustedDomain = isTrustedDomain,
        summary = summary,
        supportText = supportText,
        recommendation = recommendation,
        reasonText = reasonText
    )
}

fun totalPrimaryEngines(vtStats: VtRawStats): Int {
    return vtStats.malicious + vtStats.suspicious + vtStats.harmless + vtStats.undetected
}