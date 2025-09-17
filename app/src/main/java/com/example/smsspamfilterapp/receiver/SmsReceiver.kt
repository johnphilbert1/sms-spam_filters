package com.example.smsspamfilterapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.Message
import com.example.smsspamfilterapp.data.MessageRepository
import com.example.smsspamfilterapp.ml.SpamDetector
import com.example.smsspamfilterapp.notification.SpamNotificationManager
import com.example.smsspamfilterapp.receiver.MessageConcatenationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var messageRepository: MessageRepository
    private lateinit var notificationManager: SpamNotificationManager
    private val concatenationManager = MessageConcatenationManager()

    
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Ensure we don't block the main thread/broadcast delivery. Do all work off-thread.
        val pendingResult = goAsync()
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                messageRepository = MessageRepository(database.messageDao())
                notificationManager = SpamNotificationManager(context)
                val spamDetector = SpamDetector.getInstance(context)

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { smsMessage ->
                    // Use concatenation manager to handle multipart messages
                    concatenationManager.processMessage(smsMessage) { content: String, sender: String, timestamp: Long, isMultipart: Boolean ->
                        Log.d(TAG, "Processing ${if (isMultipart) "concatenated" else "single"} message from $sender: '${content.take(50)}...' (length: ${content.length})")
                        scope.launch {
                            processCompleteMessage(context, content, sender, timestamp, isMultipart, spamDetector)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling SMS broadcast", e)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun processCompleteMessage(
        context: Context, 
        content: String, 
        sender: String, 
        timestamp: Long, 
        isMultipart: Boolean, 
        spamDetector: SpamDetector
    ) {
        try {
            val fullContent = content

            Log.d(TAG, "Processing ${if (isMultipart) "concatenated" else "single"} message from $sender: '${fullContent.take(100)}...' (length: ${fullContent.length})")
            Log.d(TAG, "Concatenation stats: ${concatenationManager.getStats()}")
            Log.d(TAG, "Context: ${context.javaClass.simpleName}")

            // ML operations are already on background thread, call suspend functions directly
            val mlConfidence = try {
                spamDetector.getMLSpamProbability(fullContent)
            } catch (e: Exception) {
                Log.e(TAG, "ML prediction failed: ${e.message}", e)
                0.5f
            }

            val bayesianConfidence = try {
                spamDetector.getBayesianSpamProbability(fullContent)
            } catch (e: Exception) {
                Log.e(TAG, "Bayesian prediction failed: ${e.message}", e)
                0.5f
            }

            val isSpam = try {
                spamDetector.isSpam(fullContent)
            } catch (e: Exception) {
                Log.e(TAG, "Spam detection failed: ${e.message}", e)
                false
            }
            
            val finalProbability = try {
                spamDetector.getSpamProbability(fullContent)
            } catch (e: Exception) {
                Log.e(TAG, "Final probability calculation failed: ${e.message}", e)
                0.5f
            }

            Log.d(TAG, "Hybrid detection: ML=$mlConfidence, Bayesian=$bayesianConfidence, Final=$finalProbability, IsSpam=$isSpam")

            val matchedKeywords = spamDetector.getMatchedKeywords(fullContent).joinToString(",")

            val message = Message(
                sender = sender,
                content = fullContent,
                timestamp = timestamp,
                isSpam = isSpam,
                mlConfidence = mlConfidence,
                bayesianConfidence = bayesianConfidence,
                matchedKeywords = matchedKeywords
            )

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
                try {
                    val minimalMessage = Message(
                        sender = sender,
                        content = fullContent.take(100),
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
            Log.e(TAG, "Error processing message from $sender", e)
            val fallbackMessage = Message(
                sender = sender,
                content = content,
                timestamp = timestamp,
                isSpam = false,
                mlConfidence = 0f,
                bayesianConfidence = 0f,
                matchedKeywords = ""
            )
            try {
                messageRepository.insertMessage(fallbackMessage)
                Log.d(TAG, "Fallback message saved successfully")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to save fallback message: ${e2.message}", e2)
            }
        }
    }
    
    /**
     * Clean up resources when the receiver is being destroyed
     */
    fun cleanup() {
        concatenationManager.cleanup()
    }
} 