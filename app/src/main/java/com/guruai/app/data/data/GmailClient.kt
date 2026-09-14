package com.guruai.app.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

data class GmailMessage(
    val id: String,
    val from: String,
    val subject: String,
    val snippet: String
)

object GmailClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun listRecent(accessToken: String, maxResults: Int = 5): List<GmailMessage> {
        val listUrl = "https://www.googleapis.com/gmail/v1/users/me/messages?maxResults=$maxResults&labelIds=INBOX"
        val listReq = Request.Builder()
            .url(listUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val ids = mutableListOf<String>()
        try {
            client.newCall(listReq).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string().orEmpty()
                val root = JSONObject(body)
                val arr = root.optJSONArray("messages") ?: return emptyList()
                for (i in 0 until arr.length()) {
                    ids.add(arr.getJSONObject(i).getString("id"))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        val results = mutableListOf<GmailMessage>()
        for (id in ids) {
            val msgUrl = "https://www.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=Subject"
            val msgReq = Request.Builder()
                .url(msgUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            try {
                client.newCall(msgReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val snippet = root.optString("snippet")
                    val headers = root.optJSONObject("payload")?.optJSONArray("headers")
                    var from = ""
                    var subject = ""
                    if (headers != null) {
                        for (i in 0 until headers.length()) {
                            val h = headers.getJSONObject(i)
                            when (h.optString("name")) {
                                "From" -> from = h.optString("value")
                                "Subject" -> subject = h.optString("value")
                            }
                        }
                    }
                    results.add(GmailMessage(id, from, subject, snippet))
                }
            } catch (e: Exception) {
                // skip this message
            }
        }
        return results
    }

    fun createDraft(accessToken: String, toRaw: String, subject: String, body: String): Boolean {
        val toEmail = extractEmail(toRaw)
        val rawMessage = buildString {
            append("To: $toEmail\r\n")
            append("Subject: $subject\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
            append(body)
        }
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMessage.toByteArray(Charsets.UTF_8))

        val json = JSONObject()
        val messageObj = JSONObject()
        messageObj.put("raw", encoded)
        json.put("message", messageObj)

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://www.googleapis.com/gmail/v1/users/me/drafts")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { resp -> resp.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractEmail(raw: String): String {
        val match = Regex("<(.+?)>").find(raw)
        return match?.groupValues?.get(1) ?: raw.trim()
    }
}
