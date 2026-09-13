package com.guruai.app.agent

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchTool(
    private val apiKey: String,
    private val searchEngineId: String
) : Tool {
    override val name = "web_search"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun execute(args: String): String {
        val query = args.trim()
        if (query.isEmpty()) return "Search query khali hai."
        if (apiKey.isBlank() || searchEngineId.isBlank()) {
            return "Web search set up nahi hai. Settings mein Search API key aur Search Engine ID daalo."
        }

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$searchEngineId&q=$encodedQuery&num=5"

        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return "Search error ${resp.code}: ${raw.take(200)}"
                }
                val root = JSONObject(raw)
                val items = root.optJSONArray("items")
                if (items == null || items.length() == 0) {
                    return "\"$query\" ke liye koi result nahi mila."
                }
                val results = StringBuilder()
                for (i in 0 until minOf(items.length(), 5)) {
                    val item = items.getJSONObject(i)
                    val title = item.optString("title")
                    val snippet = item.optString("snippet")
                    val link = item.optString("link")
                    results.append("${i + 1}. $title\n$snippet\n$link\n\n")
                }
                results.toString().trim()
            }
        } catch (e: Exception) {
            "Search failed: ${e.message}"
        }
    }
}
