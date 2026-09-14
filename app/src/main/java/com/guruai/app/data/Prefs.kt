package com.guruai.app.data

import android.content.Context
import com.guruai.app.util.Constants

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)

    var geminiKey: String
        get() = sp.getString(Constants.KEY_GEMINI, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_GEMINI, v).apply()

    var grokKey: String
        get() = sp.getString(Constants.KEY_GROK, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_GROK, v).apply()

    var whatsappToken: String
        get() = sp.getString(Constants.KEY_WHATSAPP, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_WHATSAPP, v).apply()

    var mailToken: String
        get() = sp.getString(Constants.KEY_MAIL, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_MAIL, v).apply()

    var searchApiKey: String
        get() = sp.getString(Constants.KEY_SEARCH_API_KEY, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_SEARCH_API_KEY, v).apply()

    var searchCx: String
        get() = sp.getString(Constants.KEY_SEARCH_CX, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_SEARCH_CX, v).apply()

    var gmailConnectedEmail: String
        get() = sp.getString(Constants.KEY_GMAIL_EMAIL, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_GMAIL_EMAIL, v).apply()

    var screenMonitorEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_SCREEN_MONITOR, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_SCREEN_MONITOR, v).apply()

    var whatsappSyncEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_WHATSAPP_SYNC, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_WHATSAPP_SYNC, v).apply()

    var emailSyncEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_EMAIL_SYNC, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_EMAIL_SYNC, v).apply()

    var aiOnlineMode: Boolean
        get() = sp.getBoolean(Constants.KEY_AI_ONLINE, true)
        set(v) = sp.edit().putBoolean(Constants.KEY_AI_ONLINE, v).apply()

    var themeIndex: Int
        get() = sp.getInt(Constants.KEY_THEME, 0)
        set(v) = sp.edit().putInt(Constants.KEY_THEME, v).apply()

    var aiProvider: String
        get() = sp.getString(Constants.KEY_AI_PROVIDER, Constants.PROVIDER_GEMINI) ?: Constants.PROVIDER_GEMINI
        set(v) = sp.edit().putString(Constants.KEY_AI_PROVIDER, v).apply()

    var conversationSummary: String
        get() = sp.getString(Constants.KEY_SUMMARY, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_SUMMARY, v).apply()

    var summarizedUpToCount: Int
        get() = sp.getInt(Constants.KEY_SUMMARY_COUNT, 0)
        set(v) = sp.edit().putInt(Constants.KEY_SUMMARY_COUNT, v).apply()
}
