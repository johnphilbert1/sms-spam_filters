package com.example.smsspamfilterapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.MessageRepository
import java.util.concurrent.TimeUnit

class SpamCleanupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val messageRepository by lazy {
        MessageRepository(AppDatabase.getDatabase(context).messageDao())
    }

    override suspend fun doWork(): Result {
        return try {
            // Get retention period from preferences (default: 30 days)
            val retentionPeriod = TimeUnit.DAYS.toMillis(30)
            val cutoffTime = System.currentTimeMillis() - retentionPeriod

            // Delete old spam messages
            messageRepository.deleteOldSpamMessages(cutoffTime)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "spam_cleanup_work"
    }
} 