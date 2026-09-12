package com.guruai.app.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface KnowledgeDao {

    @Insert
    suspend fun insert(entry: KnowledgeEntity)

    @Query("SELECT * FROM knowledge ORDER BY timestamp DESC")
    suspend fun getAll(): List<KnowledgeEntity>

    @Query("DELETE FROM knowledge WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM knowledge")
    suspend fun clear()
}
