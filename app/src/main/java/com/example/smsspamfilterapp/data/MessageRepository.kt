package com.example.smsspamfilterapp.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {

    fun getInboxMessages(): Flow<List<Message>> = messageDao.getInboxMessages()

    fun getSpamMessages(): Flow<List<Message>> = messageDao.getSpamMessages()

    fun searchInboxMessages(query: String): Flow<List<Message>> =
        messageDao.searchInboxMessages(query)

    fun searchSpamMessages(query: String): Flow<List<Message>> =
        messageDao.searchSpamMessages(query)

    suspend fun insertMessage(message: Message): Long =
        messageDao.insertMessage(message)

    suspend fun deleteMessage(message: Message) =
        messageDao.deleteMessage(message)

    suspend fun restoreMessage(message: Message) =
        messageDao.restoreMessage(message.id)

    suspend fun markAsSpam(message: Message) =
        messageDao.markAsSpam(message.id)

    suspend fun deleteOldSpamMessages(cutoffTime: Long) =
        messageDao.deleteOldSpamMessages(cutoffTime)

    suspend fun getMessageById(messageId: Long): Message? =
        messageDao.getMessageById(messageId)
} 