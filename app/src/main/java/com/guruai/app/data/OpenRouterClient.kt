package com.guruai.app.data

import com.guruai.app.util.Constants
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(userMessage: String, history: List<Pair<String, String>> = emptyList()): String {
        if (apiKey.isBlank()) {
            return "OpenRouter API key missing. Add it in Settings."
        }

        var attempt = 0
        var delayMs = 1000L
        val maxAttempts = 4

        while (attempt < maxAttempts) {
            val result = callOnce(userMessage, history)
            if (result.code != 429) {
                return result.text
            }
            attempt++
            if (attempt >= maxAttempts) {
                return "OpenRouter is getting a lot of requests right now (rate limit). Please try again in a minute."
            }
            delay(delayMs)
            delayMs *= 2
        }
        return "Something went wrong. Please try again."
    }

    private data class Result(val text: String, val code: Int)

    private fun callOnce(userMessage: String, history: List<Pair<String, String>>): Result {
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val messages = JSONArray()
        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", Constants.SYSTEM_PROMPT)
        )
        history.takeLast(16).forEach { (role, text) ->
            val r = if (role == "assistant") "assistant" else "user"
            messages.put(
                JSONObject()
                    .put("role", r)
                    .put("content", text)
            )
        }
        messages.put(
            JSONObject()
                .put("role", "user")
                .put("content", userMessage)
        )

        val body = JSONObject()
            .put("model", "openrouter/free")
            .put("messages", messages)
            .put("temperature", 0.85)
            .toString()
            .toRequestBody(jsonMedia)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return Result("OpenRouter error ${resp.code}: ${raw.take(200)}", resp.code)
            }
            val text = try {
                val root = JSONObject(raw)
                root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: Exception) {
                "Could not parse reply: ${e.message}"
            }
            return Result(text, resp.code)
        }
    }
}
