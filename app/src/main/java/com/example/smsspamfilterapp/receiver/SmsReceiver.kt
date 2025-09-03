package com.example.smsspamfilterapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.Message
import com.example.smsspamfilterapp.data.MessageRepository

import com.example.smsspamfilterapp.notification.SpamNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var messageRepository: MessageRepository
    private lateinit var notificationManager: SpamNotificationManager

    
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val database = AppDatabase.getDatabase(context)
        messageRepository = MessageRepository(database.messageDao())
        notificationManager = SpamNotificationManager(context)


        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { smsMessage ->
            processMessage(context, smsMessage)
        }
    }

    private fun processMessage(context: Context, smsMessage: SmsMessage) {
        // Use the scoped coroutine with error handling
        scope.launch {
            try {
                val content = smsMessage.messageBody ?: ""
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val timestamp = smsMessage.timestampMillis
                
                // For now, use the content as-is. Multi-part SMS will be handled by the system
                val fullContent = content
                
                Log.d(TAG, "Processing message from $sender: '${fullContent.take(100)}...' (length: ${fullContent.length})")
                
                // Use simple keyword detection only (ML models disabled)
                val hasSpamKeywords = fullContent.lowercase().contains(Regex("(win|free|congrats|lottery|prize|winner|claim|urgent|limited time|act now|click here|unlimited|guaranteed|risk-free|no cost|no purchase|no obligation|no catch|no strings|no hidden|no fees)"))
                val isSpam = hasSpamKeywords
                val finalProbability = if (isSpam) 0.8f else 0.2f
                val mlConfidence = 0.5f  // Neutral
                val bayesianConfidence = 0.5f  // Neutral
                
                Log.d(TAG, "Using keyword-only detection: Keywords=$hasSpamKeywords, IsSpam=$isSpam")
                
                val matchedKeywords = if (hasSpamKeywords) "spam_keywords_detected" else ""
                
                val message = Message(
                    sender = sender,
                    content = fullContent,
                    timestamp = timestamp,
                    isSpam = isSpam,
                    mlConfidence = mlConfidence,
                    bayesianConfidence = bayesianConfidence,
                    matchedKeywords = matchedKeywords
                )
                
                // Enhanced logging for debugging
                Log.d(TAG, "Message from $sender: '${fullContent.take(50)}...'")
                Log.d(TAG, "Hybrid Classification: ML=$mlConfidence, Bayesian=$bayesianConfidence, Final=$finalProbability, IsSpam=$isSpam")
                
                if (isSpam) {
                    Log.d(TAG, "SPAM DETECTED from $sender (ML: $mlConfidence, Bayesian: $bayesianConfidence, Final: $finalProbability)")
                    notificationManager.showSpamNotification(message)
                } else {
                    Log.d(TAG, "Legitimate message from $sender (ML: $mlConfidence, Bayesian: $bayesianConfidence, Final: $finalProbability)")
                }
                
                try {
                    messageRepository.insertMessage(message)
                    Log.d(TAG, "Message saved successfully to database")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save message to database: ${e.message}", e)
                    // Try to save with minimal data
                    try {
                        val minimalMessage = Message(
                            sender = sender,
                            content = fullContent.take(100), // Limit content length
                            timestamp = timestamp,
                            isSpam = isSpam,
                            mlConfidence = mlConfidence,
                            bayesianConfidence = bayesianConfidence,
                            matchedKeywords = ""
                        )
                        messageRepository.insertMessage(minimalMessage)
                        Log.d(TAG, "Minimal message saved successfully")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to save even minimal message: ${e2.message}", e2)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message from ${smsMessage.originatingAddress}", e)
                // Still save the message even if spam detection fails
                val fallbackMessage = Message(
                    sender = smsMessage.originatingAddress ?: "Unknown",
                    content = smsMessage.messageBody ?: "",
                    timestamp = smsMessage.timestampMillis,
                    isSpam = false,
                    mlConfidence = 0f,
                    bayesianConfidence = 0f,
                    matchedKeywords = ""
                )
                messageRepository.insertMessage(fallbackMessage)
            }
        }
    }
} 