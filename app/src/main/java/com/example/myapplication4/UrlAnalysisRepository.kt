package com.example.myapplication4

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

class UrlAnalysisRepository(private val apiService: UrlAnalysisApiService) {

    suspend fun analyseUrl(url: String): ApiResult<UrlAnalysisResponse> {
        return try {
            val response = apiService.analyseUrl(AnalyseUrlRequest(url))
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Empty response body")
            } else {
                ApiResult.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: HttpException) {
            ApiResult.Error("HTTP Error: ${e.code()} ${e.message()}", e)
        } catch (e: IOException) {
            ApiResult.Error("Network Error: Check your internet connection", e)
        } catch (e: Exception) {
            ApiResult.Error("Unknown Error: ${e.localizedMessage}", e)
        }
    }

    fun parseBedrockAnalysis(raw: String): BedrockResult? {
        return try {
            // Regex to extract content between ```json and ```
            val regex = "```json\\s*(\\{.*?\\})\\s*```".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(raw)
            val jsonString = match?.groupValues?.get(1) ?: raw
            
            Gson().fromJson(jsonString, BedrockResult::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
