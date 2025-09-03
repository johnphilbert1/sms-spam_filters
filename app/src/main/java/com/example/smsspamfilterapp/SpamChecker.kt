package com.example.smsspamfilterapp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.SpamMessage
import com.example.smsspamfilterapp.data.SpamRepository
import com.example.smsspamfilterapp.ml.DataLoader
import com.example.smsspamfilterapp.ml.SpamDetector
import com.example.smsspamfilterapp.spam.BayesianFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class SpamChecker private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val defaultKeywords = setOf(
        "win", "free", "congrats", "lottery", "prize", "winner",
        "claim", "urgent", "limited time", "act now", "click here",
        "unlimited", "guaranteed", "risk-free", "no cost", "no purchase",
        "no obligation", "no catch", "no strings", "no hidden", "no fees"
    )

    private val repository: SpamRepository
    private val scope = CoroutineScope(Dispatchers.IO)
    private val spamDetector: SpamDetector
    private val bayesianFilter: BayesianFilter
    private val dataLoader: DataLoader

    companion object {
        private const val TAG = "SpamChecker"
        private const val PREFS_NAME = "SpamFilterPrefs"
        private const val KEY_KEYWORDS = "spam_keywords"
        private var instance: SpamChecker? = null

        fun getInstance(context: Context): SpamChecker {
            return instance ?: synchronized(this) {
                instance ?: SpamChecker(context).also { instance = it }
            }
        }
    }

    init {
        // Initialize keywords if not already set
        if (!prefs.contains(KEY_KEYWORDS)) {
            saveKeywords(defaultKeywords)
        }
        
        // Initialize repository and ML components
        val database = AppDatabase.getDatabase(context)
        repository = SpamRepository(database.spamMessageDao())
        dataLoader = DataLoader(context)
        spamDetector = SpamDetector.getInstance(context)
        bayesianFilter = dataLoader.loadBayesianModel()
    }

    suspend fun isSpam(message: String): Boolean {
        // Check with TFLite model (deep learning approach)
        val tfliteProbability = spamDetector.getSpamProbability(message)
        
        // Check with Bayesian filter (probabilistic approach)
        val bayesianProbability = bayesianFilter.calculateSpamProbability(message)
        
        // Check for known spam keywords (rule-based approach)
        val hasSpamKeywords = getKeywords().any { keyword ->
            message.lowercase().contains(keyword.lowercase())
        }

        // Combine the results:
        // 1. If TFLite is very confident (>0.9), trust it
        // 2. If Bayesian is very confident (>0.9), trust it
        // 3. If both models are somewhat confident (>0.7) but not very confident, trust their agreement
        // 4. If we found spam keywords, increase suspicion
        return when {
            tfliteProbability > 0.9 -> true
            bayesianProbability > 0.9 -> true
            hasSpamKeywords && (tfliteProbability > 0.5 || bayesianProbability > 0.5) -> true
            tfliteProbability > 0.7 && bayesianProbability > 0.7 -> true
            else -> false
        }
    }

    fun getMatchedKeywords(message: String): Set<String> {
        val keywords = getKeywords()
        val messageLower = message.lowercase()
        
        return keywords.filter { keyword ->
            messageLower.contains(keyword.lowercase())
        }.toSet()
    }

    suspend fun saveSpamMessage(sender: String, body: String) = withContext(Dispatchers.IO) {
        try {
            val matchedKeywords = getMatchedKeywords(body)
            val spamMessage = SpamMessage(
                sender = sender,
                body = body,
                timestamp = Date(),
                matchedKeywords = matchedKeywords.joinToString(",")
            )
            repository.insertSpamMessage(spamMessage)
            Log.d(TAG, "Saved spam message from $sender")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving spam message", e)
        }
    }

    fun getKeywords(): Set<String> {
        return prefs.getStringSet(KEY_KEYWORDS, defaultKeywords) ?: defaultKeywords
    }

    fun addKeyword(keyword: String) {
        val currentKeywords = getKeywords().toMutableSet()
        currentKeywords.add(keyword.trim())
        saveKeywords(currentKeywords)
        Log.d(TAG, "Added keyword: $keyword")
    }

    fun removeKeyword(keyword: String) {
        val currentKeywords = getKeywords().toMutableSet()
        currentKeywords.remove(keyword.trim())
        saveKeywords(currentKeywords)
        Log.d(TAG, "Removed keyword: $keyword")
    }

    fun resetToDefaults() {
        saveKeywords(defaultKeywords)
        Log.d(TAG, "Reset keywords to defaults")
    }

    private fun saveKeywords(keywords: Set<String>) {
        prefs.edit {
            putStringSet(KEY_KEYWORDS, keywords)
        }
    }
}
