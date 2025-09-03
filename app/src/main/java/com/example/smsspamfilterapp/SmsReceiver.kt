package com.example.smsspamfilterapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w(TAG, "Received unexpected intent action: ${intent.action}")
            return
        }

        // Get a wake lock to ensure processing completes
        val pendingResult = goAsync()

        // Using GlobalScope is not ideal but acceptable for BroadcastReceiver
        // since its lifecycle is very short and controlled by the system
        GlobalScope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val spamChecker = SpamChecker.getInstance(context)

                messages?.forEach { message ->
                    val sender = message.displayOriginatingAddress ?: "Unknown"
                    val body = message.messageBody

                    try {
                        if (spamChecker.isSpam(body)) {
                            Log.i(TAG, "Spam blocked from $sender: $body")
                            spamChecker.saveSpamMessage(sender, body)
                            pendingResult.abortBroadcast()
                            showSpamNotification(context, sender)
                        } else {
                            Log.d(TAG, "Legitimate message from $sender")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message from $sender", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showSpamNotification(context: Context, sender: String) {
        Toast.makeText(
            context,
            "Spam message blocked from $sender",
            Toast.LENGTH_SHORT
        ).show()
    }
}