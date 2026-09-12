package com.guruai.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentLinkedQueue

data class WhatsAppMessage(
    val sender: String,
    val text: String,
    val timestamp: Long
)

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != "com.whatsapp") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val message = WhatsAppMessage(sender = title, text = text, timestamp = sbn.postTime)
        recentMessages.add(message)

        while (recentMessages.size > MAX_STORED) {
            recentMessages.poll()
        }
    }

    companion object {
        private const val MAX_STORED = 50
        private val recentMessages = ConcurrentLinkedQueue<WhatsAppMessage>()

        fun getRecent(): List<WhatsAppMessage> = recentMessages.toList()

        fun getRecentFrom(name: String): List<WhatsAppMessage> =
            recentMessages.filter { it.sender.contains(name, ignoreCase = true) }

        fun isListenerConnected(context: android.content.Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }
    }
}
