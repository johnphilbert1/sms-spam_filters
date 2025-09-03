package com.example.smsspamfilterapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamMessageDao {
    @Query("SELECT * FROM spam_messages ORDER BY timestamp DESC")
    fun getAllSpamMessages(): Flow<List<SpamMessage>>

    @Query("SELECT COUNT(*) FROM spam_messages")
    fun getSpamCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamMessage(spamMessage: SpamMessage): Long

    @Delete
    suspend fun deleteSpamMessage(spamMessage: SpamMessage)

    @Query("DELETE FROM spam_messages")
    suspend fun deleteAllSpamMessages()

    @Query("SELECT * FROM spam_messages WHERE id = :id")
    suspend fun getSpamMessage(id: Long): SpamMessage?
} 