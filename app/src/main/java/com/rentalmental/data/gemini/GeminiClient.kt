package com.rentalmental.data.gemini

import com.rentalmental.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    fun transcribeAndExtract(audioFile: File, prompt: String, mimeType: String = "audio/mp4"): String {
        val audioBase64 = android.util.Base64.encodeToString(
            audioFile.readBytes(), android.util.Base64.NO_WRAP
        )

        val requestJson = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray()
                                .put(JSONObject().apply { put("text", prompt) })
                                .put(
                                    JSONObject().apply {
                                        put(
                                            "inline_data",
                                            JSONObject().apply {
                                                put("mime_type", mimeType)
                                                put("data", audioBase64)
                                            }
                                        )
                                    }
                                )
                        )
                    }
                )
            )
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$endpoint?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Gemini request failed: ${response.code} ${response.body?.string()}")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val responseJson = JSONObject(responseBody)
            return responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }
}
