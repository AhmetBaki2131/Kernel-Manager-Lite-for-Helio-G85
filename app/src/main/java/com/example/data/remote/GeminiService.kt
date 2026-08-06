package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiPart(val text: String? = null)

data class GeminiContent(val parts: List<GeminiPart>? = null)

data class GeminiResponseFormatText(
    val mimeType: String = "application/json"
)

data class GeminiResponseFormat(
    val text: GeminiResponseFormatText? = GeminiResponseFormatText()
)

data class GeminiGenerationConfig(
    val responseFormat: GeminiResponseFormat? = GeminiResponseFormat(),
    val temperature: Float? = 0.2f
)

data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

data class GeminiCandidate(val content: GeminiContent? = null)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

interface GeminiRestApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

class GeminiService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: GeminiRestApi = retrofit.create(GeminiRestApi::class.java)

    suspend fun generateOptimizationRecommendations(
        apiKey: String,
        systemInstructionText: String,
        promptText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = GeminiGenerateRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = promptText)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
            )

            val response = api.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!responseText.isNullOrBlank()) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("Gemini API returned an empty response."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
