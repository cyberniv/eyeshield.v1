package com.example.myapplication4

import com.google.gson.annotations.SerializedName

data class AnalyseUrlRequest(
    val url: String
)

data class VtRawStats(
    val malicious: Int,
    val undetected: Int,
    val harmless: Int,
    val suspicious: Int,
    val timeout: Int,
    @SerializedName("confirmed_timeout") val confirmedTimeout: Int,
    val failure: Int,
    @SerializedName("type_unsupported") val typeUnsupported: Int
)

data class UrlAnalysisResponse(
    val url: String,
    @SerializedName("vt_raw_stats") val vtRawStats: VtRawStats,
    @SerializedName("bedrock_analysis") val bedrockAnalysis: String
)

typealias AnalyseUrlResponse = UrlAnalysisResponse

data class BedrockResult(
    val verdict: String,
    @SerializedName("confidence_score") val confidenceScore: Int,
    val summary: String
)

sealed class ApiResult<out T> {
    object Idle : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ApiResult<Nothing>()
}
