package com.guruai.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TermuxClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun chat(prompt: String): String {
        return try {
            val cleanPrompt = extractLatestMessage(prompt)
            val json = JSONObject().put("message", cleanPrompt).toString()
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://127.0.0.1:5000/v1/chat")
                .addHeader("Authorization", "Bearer GURU-AI-SECRET-KEY-999")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "(Termux server error: ${response.code})"
                }
                val respBody = response.body?.string() ?: "{}"
                JSONObject(respBody).optString("reply", "(Empty reply from Termux server)")
            }
        } catch (e: Exception) {
            "(Termux server error: ${e.message})"
        }
    }

    private fun extractLatestMessage(prompt: String): String {
        val idx = prompt.lastIndexOf("User:")
        return if (idx >= 0) {
            prompt.substring(idx + "User:".length).trim()
        } else {
            prompt.trim()
        }
    }
}
