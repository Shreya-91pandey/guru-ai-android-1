package com.guruai.app.memory

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

class MemoryStore(context: Context) {
    private val dao = AppDatabase.getDatabase(context).memoryDao()

    suspend fun saveMessage(role: String, content: String) {
        dao.insert(MessageEntity(role = role, content = content))
    }

    suspend fun getAllMessages(): List<MessageEntity> {
        return dao.getAll()
    }

    suspend fun getMessagesGroupedByDate(): Map<String, List<MessageEntity>> {
        val all = dao.getAll()
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return all.groupBy { formatter.format(it.timestamp) }
    }

    suspend fun clearAll() {
        dao.clear()
    }
}
