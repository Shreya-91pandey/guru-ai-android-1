package com.guruai.app.util

object Constants {
    const val APP_NAME = "Guru AI"
    const val DEVICE_MODEL = "Nothing Phone (3a) Lite"
    const val DEVICE_OS = "Nothing OS"

    const val MASTER_PASSWORD = "Nikesh@12345"

    const val PREFS = "guru_ai_prefs"
    const val KEY_GEMINI = "gemini_api_key"
    const val KEY_GROK = "grok_api_key"
    const val KEY_OPENROUTER = "openrouter_api_key"
    const val KEY_WHATSAPP = "whatsapp_token"
    const val KEY_MAIL = "mail_token"

    const val KEY_SCREEN_MONITOR = "screen_monitor_enabled"
    const val KEY_WHATSAPP_SYNC = "whatsapp_sync_enabled"
    const val KEY_EMAIL_SYNC = "email_sync_enabled"
    const val KEY_AI_ONLINE = "ai_online_mode"
    const val KEY_THEME = "selected_theme"
    const val KEY_AI_PROVIDER = "ai_provider"
    const val KEY_SUMMARY = "conversation_summary"
    const val KEY_SUMMARY_COUNT = "summarized_up_to_count"

    const val PROVIDER_GEMINI = "gemini"
    const val PROVIDER_GROK = "grok"
    const val PROVIDER_OPENROUTER = "openrouter"

    val SYSTEM_PROMPT = """
You are Guru AI – a warm, witty personal companion on a $DEVICE_MODEL ($DEVICE_OS).
Speak naturally like a close friend. Be professional for work tasks.
You know Nothing OS: Glyph, Nothing X, settings paths, permissions, battery, camera.
When Accessibility is enabled you may read on-screen text the user points you to and help draft/type after confirmation.
Never send WhatsApp/email without explicit user confirmation.
For bulk business messages, prepare drafts and confirm recipients first.
Never guess or hallucinate facts — if unsure, say so clearly instead of making something up.
IMPORTANT: Only write your own single reply. Never write the user's next message, never continue the conversation on their behalf, and never include lines like "User:" in your response.
""".trimIndent()

    data class ThemeColors(
        val name: String,
        val background: String,
        val surface: String,
        val accent: String,
        val textPrimary: String,
        val textSecondary: String
    )

    val THEMES = listOf(
        ThemeColors(
            name = "Dark Gold",
            background = "#0A0A0A",
            surface = "#121212",
            accent = "#F5C518",
            textPrimary = "#F5F5F5",
            textSecondary = "#666666"
        ),
        ThemeColors(
            name = "Cloud Black",
            background = "#000000",
            surface = "#1A1A1A",
            accent = "#FFFFFF",
            textPrimary = "#FFFFFF",
            textSecondary = "#888888"
        ),
        ThemeColors(
            name = "Ocean Blue",
            background = "#071426",
            surface = "#0F2440",
            accent = "#38BDF8",
            textPrimary = "#E5F6FF",
            textSecondary = "#7FA8C9"
        ),
        ThemeColors(
            name = "Midnight Purple",
            background = "#160C24",
            surface = "#241536",
            accent = "#A78BFA",
            textPrimary = "#F3ECFF",
            textSecondary = "#9A87B8"
        ),
        ThemeColors(
            name = "Cream Light",
            background = "#F5F1E8",
            surface = "#EAE3D3",
            accent = "#C2703D",
            textPrimary = "#2B2620",
            textSecondary = "#8A8171"
        )
    )
}
