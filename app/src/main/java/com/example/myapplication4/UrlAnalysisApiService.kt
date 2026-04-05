package com.example.myapplication4

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UrlAnalysisApiService {
    @POST("apis/urls/analyse-url")
    suspend fun analyseUrl(
        @Body request: AnalyseUrlRequest
    ): Response<UrlAnalysisResponse>
}
