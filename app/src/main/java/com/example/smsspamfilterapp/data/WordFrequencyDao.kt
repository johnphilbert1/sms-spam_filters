package com.example.smsspamfilterapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordFrequencyDao {
    @Query("SELECT * FROM word_frequencies")
    fun getAllWordFrequencies(): Flow<List<WordFrequency>>

    @Query("SELECT * FROM word_frequencies WHERE word = :word")
    suspend fun getWordFrequency(word: String): WordFrequency?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordFrequency(wordFrequency: WordFrequency)

    @Update
    suspend fun updateWordFrequency(wordFrequency: WordFrequency)

    @Query("UPDATE word_frequencies SET spamCount = spamCount + 1 WHERE word = :word")
    suspend fun incrementSpamCount(word: String)

    @Query("UPDATE word_frequencies SET hamCount = hamCount + 1 WHERE word = :word")
    suspend fun incrementHamCount(word: String)

    @Query("SELECT * FROM word_frequencies ORDER BY (spamCount + hamCount) DESC LIMIT :limit")
    fun getMostFrequentWords(limit: Int): Flow<List<WordFrequency>>

    @Delete
    suspend fun deleteWordFrequency(wordFrequency: WordFrequency)

    @Query("DELETE FROM word_frequencies")
    suspend fun deleteAllWordFrequencies()
} 