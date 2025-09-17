package com.example.smsspamfilterapp.util

import android.util.Log

/**
 * Utility class for handling logging throughout the app
 */
internal object LogUtils {
    private const val MAX_LOG_LENGTH = 100
    private const val ENABLED = true  // TODO: Make this configurable via settings
    
    fun d(tag: String, message: String) {
        if (ENABLED) {
            Log.d(tag, truncateMessage(message))
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLED) {
            Log.e(tag, truncateMessage(message), throwable)
        }
    }

    fun w(tag: String, message: String) {
        if (ENABLED) {
            Log.w(tag, truncateMessage(message))
        }
    }

    private fun truncateMessage(message: String): String {
        return if (message.length > MAX_LOG_LENGTH) {
            "${message.take(MAX_LOG_LENGTH)}..."
        } else {
            message
        }
    }
}
