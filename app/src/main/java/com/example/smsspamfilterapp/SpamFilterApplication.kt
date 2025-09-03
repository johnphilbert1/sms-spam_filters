package com.example.smsspamfilterapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smsspamfilterapp.work.SpamCleanupWorker
import java.util.concurrent.TimeUnit

class SpamFilterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val cleanupWorkRequest = PeriodicWorkRequestBuilder<SpamCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SpamCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWorkRequest
        )
    }
} 