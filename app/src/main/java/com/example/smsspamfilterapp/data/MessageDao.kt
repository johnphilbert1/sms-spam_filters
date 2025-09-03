package com.example.smsspamfilterapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE isSpam = 0 ORDER BY timestamp DESC")
    fun getInboxMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isSpam = 1 ORDER BY timestamp DESC")
    fun getSpamMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isSpam = 0 AND (content LIKE '%' || :query || '%' OR sender LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchInboxMessages(query: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isSpam = 1 AND (content LIKE '%' || :query || '%' OR sender LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchSpamMessages(query: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("UPDATE messages SET isSpam = 0 WHERE id = :messageId")
    suspend fun restoreMessage(messageId: Long)

    @Query("UPDATE messages SET isSpam = 1 WHERE id = :messageId")
    suspend fun markAsSpam(messageId: Long)

    @Query("DELETE FROM messages WHERE isSpam = 1 AND timestamp < :cutoffTime")
    suspend fun deleteOldSpamMessages(cutoffTime: Long)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?
} 