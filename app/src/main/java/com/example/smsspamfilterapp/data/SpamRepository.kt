package com.example.smsspamfilterapp.data

import kotlinx.coroutines.flow.Flow

class SpamRepository(private val spamMessageDao: SpamMessageDao) {
    val allSpamMessages: Flow<List<SpamMessage>> = spamMessageDao.getAllSpamMessages()
    val spamCount: Flow<Int> = spamMessageDao.getSpamCount()

    suspend fun insertSpamMessage(spamMessage: SpamMessage): Long {
        return spamMessageDao.insertSpamMessage(spamMessage)
    }

    suspend fun deleteSpamMessage(spamMessage: SpamMessage) {
        spamMessageDao.deleteSpamMessage(spamMessage)
    }

    suspend fun deleteAllSpamMessages() {
        spamMessageDao.deleteAllSpamMessages()
    }

    suspend fun getSpamMessage(id: Long): SpamMessage? {
        return spamMessageDao.getSpamMessage(id)
    }
} 