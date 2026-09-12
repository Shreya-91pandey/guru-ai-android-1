package com.guruai.app.memory

import android.content.Context

class KnowledgeStore(context: Context) {
    private val dao = AppDatabase.getDatabase(context).knowledgeDao()

    suspend fun save(title: String, content: String) {
        dao.insert(KnowledgeEntity(title = title, content = content))
    }

    suspend fun getAll(): List<KnowledgeEntity> {
        return dao.getAll()
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clear()
    }

    /**
     * Simple keyword-based search: finds entries whose title or content
     * shares words with the query. Not true semantic search, but useful
     * for finding relevant saved notes.
     */
    suspend fun findRelevant(query: String, maxResults: Int = 3): List<KnowledgeEntity> {
        val all = dao.getAll()
        if (all.isEmpty()) return emptyList()

        val queryWords = query.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 }
            .toSet()

        if (queryWords.isEmpty()) return emptyList()

        return all
            .map { entry ->
                val text = (entry.title + " " + entry.content).lowercase()
                val score = queryWords.count { word -> text.contains(word) }
                entry to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }
    }
}
