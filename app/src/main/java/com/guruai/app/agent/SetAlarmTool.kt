package com.guruai.app.agent

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class SetAlarmTool(private val context: Context) : Tool {
    override val name = "set_alarm"

    override fun execute(args: String): String {
        // Expected args format: "HH:MM|Label" e.g. "07:30|Wake up"
        val parts = args.split("|")
        val timePart = parts.getOrNull(0)?.trim() ?: return "Could not understand the time."
        val label = parts.getOrNull(1)?.trim() ?: "Guru AI Alarm"

        val timeParts = timePart.split(":")
        if (timeParts.size != 2) return "Please use HH:MM format for the alarm time."

        val hour = timeParts[0].toIntOrNull() ?: return "Invalid hour."
        val minute = timeParts[1].toIntOrNull() ?: return "Invalid minute."

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Alarm set for $timePart ($label)."
        } catch (e: Exception) {
            "Could not set the alarm: ${e.message}"
        }
    }
}
