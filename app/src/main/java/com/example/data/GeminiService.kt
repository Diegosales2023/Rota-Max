package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class ParsedStop(
    val address: String,
    val recipientName: String,
    val notes: String = "",
    val estimatedTime: String = ""
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    val moshiInstance = moshi
}

object GeminiParser {
    private const val TAG = "GeminiParser"

    suspend fun parseManifestText(text: String): List<ParsedStop> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder!")
            return@withContext emptyList()
        }

        val prompt = """
            Você é um assistente de entrega profissional. Analise o seguinte texto de manifesto de rotas e extraia todas as paradas e endereços de entrega estruturados em JSON.
            Retorne obrigatoriamente um array JSON de objetos onde cada objeto possui:
            - "address" (Endereço completo da entrega)
            - "recipientName" (Nome do destinatário, se houver)
            - "notes" (Instruções adicionais, se houver)
            - "estimatedTime" (Horário previsto de entrega no formato HH:MM se houver, ou string vazia)

            Texto do Manifesto:
            $text
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = GeminiClient.api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext emptyList()

            parseJsonStops(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            emptyList()
        }
    }

    suspend fun parseManifestImage(bitmap: Bitmap, textPrompt: String = ""): List<ParsedStop> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder!")
            return@withContext emptyList()
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Você é um assistente de entrega profissional. Analise a imagem fornecida (que é uma foto de um manifesto de entrega, planilha ou papel de rota) e extraia todas as paradas e endereços estruturados em JSON.
            Se houver anotações por cima ou dados adicionais fornecidos: $textPrompt
            Retorne obrigatoriamente um array JSON de objetos onde cada objeto possui:
            - "address" (Endereço de entrega completo e bem formado)
            - "recipientName" (Nome do destinatário, se houver)
            - "notes" (Anotações ou instruções adicionais de entrega, se houver)
            - "estimatedTime" (Horário previsto de entrega no formato HH:MM se houver, ou string vazia)
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = GeminiClient.api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext emptyList()

            parseJsonStops(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API with image", e)
            emptyList()
        }
    }

    private fun parseJsonStops(jsonText: String): List<ParsedStop> {
        return try {
            val type = Types.newParameterizedType(List::class.java, ParsedStop::class.java)
            val adapter = GeminiClient.moshiInstance.adapter<List<ParsedStop>>(type)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON from Gemini response: $jsonText", e)
            emptyList()
        }
    }
}
