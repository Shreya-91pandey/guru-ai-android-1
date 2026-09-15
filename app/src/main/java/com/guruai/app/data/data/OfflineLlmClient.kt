package com.guruai.app.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference

object OfflineLlmClient {
    @Volatile private var inference: LlmInference? = null
    @Volatile private var loadedPath: String? = null

    @Synchronized
    fun generate(context: Context, modelPath: String, prompt: String): String {
        return try {
            if (inference == null || loadedPath != modelPath) {
                inference?.close()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)
                    .build()
                inference = LlmInference.createFromOptions(context, options)
                loadedPath = modelPath
            }
            inference?.generateResponse(prompt) ?: "(Offline model error: failed to load)"
        } catch (e: Exception) {
            "(Offline model error: ${e.message})"
        }
    }
}
