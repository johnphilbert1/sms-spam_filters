package com.example.smsspamfilterapp.receiver

import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages concatenation of multipart SMS messages.
 * Handles buffering, timeout, and completion of SMS message parts.
 */
class MessageConcatenationManager {
    
    companion object {
        private const val TAG = "MessageConcatenationManager"
        private const val CONCATENATION_TIMEOUT_MS = 30000L // 30 seconds
        private const val MAX_PENDING_MESSAGES = 100
        private const val MAX_MESSAGE_PARTS = 10
    }
    
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageIdCounter = AtomicLong(0)
    
    /**
     * Data class to hold pending message parts
     */
    private data class PendingMessage(
        val messageId: String,
        val sender: String,
        val timestamp: Long,
        val parts: MutableMap<Int, String> = mutableMapOf(),
        val totalParts: Int = 0,
        val createdTime: Long = System.currentTimeMillis(),
        val timeoutJob: Job? = null
    ) {
        fun isComplete(): Boolean = totalParts > 0 && parts.size == totalParts
        fun getConcatenatedContent(): String = parts.toSortedMap().values.joinToString("")
        
        fun copy(timeoutJob: Job? = this.timeoutJob): PendingMessage {
            return PendingMessage(messageId, sender, timestamp, parts, totalParts, createdTime, timeoutJob)
        }
    }
    
    /**
     * Process an incoming SMS message part
     * @param smsMessage The SMS message part
     * @param onComplete Callback when message is complete or timeout occurs
     */
    fun processMessage(
        smsMessage: SmsMessage,
        onComplete: (String, String, Long, Boolean) -> Unit
    ) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val content = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis
        
        Log.d(TAG, "Received message from $sender: '${content.take(50)}...' (length: ${content.length})")
        
        // Check if this is a concatenated SMS using User Data Header (UDH)
        val userData = smsMessage.userData
        val isConcatenated = userData != null && userData.size >= 6 && userData[0] == 0x00.toByte() && userData[1] == 0x03.toByte()
        
        Log.d(TAG, "UserData: ${userData?.let { it.joinToString(",") { b -> "0x${b.toString(16)}" } } ?: "null"}, isConcatenated: $isConcatenated")
        
        if (isConcatenated) {
            // This is a concatenated SMS part
            val referenceNumber = ((userData[2].toInt() and 0xFF) shl 8) or (userData[3].toInt() and 0xFF)
            val totalParts = userData[4].toInt() and 0xFF
            val partNumber = userData[5].toInt() and 0xFF
            
            Log.d(TAG, "Concatenated SMS detected: ref=$referenceNumber, part=$partNumber/$totalParts")
            
            val messageKey = "${sender}_${referenceNumber}"
            
            // Get or create pending message
            val pendingMessage = pendingMessages.getOrPut(messageKey) {
                PendingMessage(
                    messageId = messageKey,
                    sender = sender,
                    timestamp = timestamp,
                    totalParts = totalParts
                )
            }
            
            // Add this part
            pendingMessage.parts[partNumber] = content
            Log.d(TAG, "Added part $partNumber/$totalParts to message $messageKey")
            
            // Cancel existing timeout
            pendingMessage.timeoutJob?.cancel()
            
            if (pendingMessage.isComplete()) {
                // All parts received - process complete message
                val completeContent = pendingMessage.getConcatenatedContent()
                Log.d(TAG, "Complete concatenated message: '${completeContent.take(100)}...' (${completeContent.length} chars)")
                
                // Remove from pending
                pendingMessages.remove(messageKey)
                
                // Process complete message
                onComplete(completeContent, sender, timestamp, true)
            } else {
                // Still waiting for more parts - set timeout
                val timeoutJob = scope.launch {
                    delay(CONCATENATION_TIMEOUT_MS)
                    if (pendingMessages.containsKey(messageKey)) {
                        val incompleteContent = pendingMessage.getConcatenatedContent()
                        Log.w(TAG, "Timeout for concatenated message $messageKey, processing incomplete with ${pendingMessage.parts.size}/$totalParts parts")
                        
                        // Remove from pending
                        pendingMessages.remove(messageKey)
                        
                        // Process incomplete message
                        onComplete(incompleteContent, sender, timestamp, true)
                    }
                }
                
                // Update the pending message with new timeout job
                pendingMessages[messageKey] = pendingMessage.copy(timeoutJob = timeoutJob)
            }
        } else {
            // Check if this might be a split message without proper UDH (fallback detection)
            val mightBeSplit = content.length > 100 && (
                content.contains("...") ||
                content.endsWith("...") ||
                content.startsWith("...") ||
                content.contains("(1/2)") ||
                content.contains("(2/2)") ||
                content.contains("(1/3)") ||
                content.contains("(2/3)") ||
                content.contains("(3/3)") ||
                content.contains("Part 1") ||
                content.contains("Part 2") ||
                content.contains("Part 3")
            )
            
            if (mightBeSplit) {
                // Check if we have recent messages from the same sender that might be related
                val recentMessages = pendingMessages.filter { (key, pendingMessage) ->
                    key.startsWith("${sender}_") && 
                    Math.abs(timestamp - pendingMessage.timestamp) < 10000 && // Within 10 seconds
                    pendingMessage.totalParts == 0 // Fallback messages
                }
                
                val messageKey = if (recentMessages.isNotEmpty()) {
                    // Found existing fallback message from same sender
                    recentMessages.keys.first()
                } else {
                    // Create new fallback message
                    "${sender}_${timestamp}_fallback"
                }
                
                Log.d(TAG, "Potential split message detected (fallback): $sender, key: $messageKey")
                
                val pendingMessage = pendingMessages.getOrPut(messageKey) {
                    PendingMessage(
                        messageId = messageKey,
                        sender = sender,
                        timestamp = timestamp,
                        totalParts = 0 // Unknown total parts
                    )
                }
                
                // Add this part
                val partNumber = pendingMessage.parts.size + 1
                pendingMessage.parts[partNumber] = content
                Log.d(TAG, "Added fallback part $partNumber to message $messageKey")
                
                // Cancel existing timeout
                pendingMessage.timeoutJob?.cancel()
                
                // Set timeout for fallback detection
                val timeoutJob = scope.launch {
                    delay(5000) // Wait 5 seconds for more parts
                    if (pendingMessages.containsKey(messageKey)) {
                        val completeContent = pendingMessage.getConcatenatedContent()
                        Log.d(TAG, "Processing fallback concatenated message: '${completeContent.take(100)}...' (${completeContent.length} chars)")
                        
                        // Remove from pending
                        pendingMessages.remove(messageKey)
                        
                        // Process complete message
                        onComplete(completeContent, sender, timestamp, true)
                    }
                }
                
                // Update the pending message with new timeout job
                pendingMessages[messageKey] = pendingMessage.copy(timeoutJob = timeoutJob)
            } else {
                // Single message - process immediately
                Log.d(TAG, "Single message from $sender: '${content.take(50)}...' (length: ${content.length})")
                onComplete(content, sender, timestamp, false)
            }
        }
    }
    
    /**
     * Handle timeout for incomplete messages
     */
    private fun handleTimeout(messageKey: String) {
        val pendingMessage = pendingMessages.remove(messageKey)
        if (pendingMessage != null) {
            Log.w(TAG, "Timeout for message $messageKey, processing incomplete message with ${pendingMessage.parts.size}/${pendingMessage.totalParts} parts")
            
            // Process whatever parts we have
            val incompleteContent = pendingMessage.getConcatenatedContent()
            if (incompleteContent.isNotEmpty()) {
                // Note: We'll need to pass a callback here, but for timeout we'll just log
                Log.w(TAG, "Incomplete message content: '${incompleteContent.take(100)}...'")
            }
        }
    }
    
    /**
     * Clean up oldest pending messages when we have too many
     */
    private fun cleanupOldestMessages() {
        val sortedMessages = pendingMessages.values.sortedBy { it.createdTime }
        val toRemove = sortedMessages.take(pendingMessages.size - MAX_PENDING_MESSAGES + 10)
        
        toRemove.forEach { message ->
            message.timeoutJob?.cancel()
            pendingMessages.remove(message.messageId)
            Log.d(TAG, "Cleaned up old pending message: ${message.messageId}")
        }
    }
    
    /**
     * Get current statistics about pending messages
     */
    fun getStats(): String {
        val totalPending = pendingMessages.size
        val incompleteCount = pendingMessages.values.count { !it.isComplete() }
        return "Pending: $totalPending, Incomplete: $incompleteCount"
    }
    
    /**
     * Clean up all pending messages (useful for testing or app shutdown)
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up all pending messages")
        pendingMessages.values.forEach { it.timeoutJob?.cancel() }
        pendingMessages.clear()
    }
    
    /**
     * Test method to verify concatenation is working
     * This can be called from the app to test the concatenation logic
     */
    fun testConcatenation() {
        Log.d(TAG, "Testing concatenation logic...")
        Log.d(TAG, "Current pending messages: ${pendingMessages.size}")
        pendingMessages.forEach { (key, message) ->
            Log.d(TAG, "Pending: $key - Parts: ${message.parts.size}/${message.totalParts} - Complete: ${message.isComplete()}")
        }
    }
}
