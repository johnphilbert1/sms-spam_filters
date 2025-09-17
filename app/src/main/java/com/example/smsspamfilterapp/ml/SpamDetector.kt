package com.example.smsspamfilterapp.ml

import android.content.Context
import com.example.smsspamfilterapp.spam.BayesianFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import android.util.Log

class SpamDetector private constructor(context: Context) {
    private val tfliteModel = TFLiteModel(context)
    private val bayesianFilter = BayesianFilter()
    private val preprocessor = TextPreprocessor()
    
    init {
        // Load pre-trained Bayesian data
        bayesianFilter.loadPreTrainedData(context)
    }
    
    companion object {
        private const val SPAM_THRESHOLD = 0.5f
        private var instance: SpamDetector? = null
        
        fun getInstance(context: Context): SpamDetector {
            return instance ?: synchronized(this) {
                instance ?: SpamDetector(context).also { instance = it }
            }
        }
    }

    suspend fun getMLSpamProbability(text: String): Float {
        return try {
            withContext(Dispatchers.Default) {
                try {
                    // Validate input
                    if (text.isBlank()) {
                        Log.w("SpamDetector", "Empty text provided for ML prediction")
                        return@withContext 0.5f
                    }
                    
                    val result = tfliteModel.predict(text)
                    Log.d("SpamDetector", "TFLite prediction for '$text': $result")
                    
                    // Validate result
                    if (result.isNaN() || result.isInfinite()) {
                        Log.w("SpamDetector", "Invalid ML prediction result: $result")
                        return@withContext 0.5f
                    }
                    
                    result.coerceIn(0f, 1f)
                } catch (e: Exception) {
                    Log.e("SpamDetector", "TFLite prediction failed: ${e.message}", e)
                    0.5f // Neutral on error
                }
            }
        } catch (e: Exception) {
            Log.e("SpamDetector", "ML prediction completely failed: ${e.message}", e)
            0.5f // Neutral on complete failure
        }
    }
    
    fun getBayesianSpamProbability(text: String): Float {
        return try {
            // Validate input
            if (text.isBlank()) {
                Log.w("SpamDetector", "Empty text provided for Bayesian prediction")
                return 0.5f
            }
            
            val result = bayesianFilter.getSpamProbability(text)
            Log.d("SpamDetector", "Bayesian prediction for '$text': $result")
            
            // Validate result
            if (result.isNaN() || result.isInfinite()) {
                Log.w("SpamDetector", "Invalid Bayesian prediction result: $result")
                return 0.5f
            }
            
            result.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e("SpamDetector", "Bayesian prediction failed: ${e.message}", e)
            0.5f // Neutral on error
        }
    }
    
    suspend fun isSpam(text: String): Boolean {
        // Preprocess text for consistent case handling
        val preprocessedText = preprocessor.preprocess(text)
        
        val mlConfidence = getMLSpamProbability(preprocessedText)
        val bayesianConfidence = getBayesianSpamProbability(preprocessedText)
        
        // More conservative hybrid decision logic
        val isSpam = when {
            // Only very high confidence from either model
            mlConfidence > 0.85f -> true      // Very high TFLite confidence
            bayesianConfidence > 0.9f -> true // Very high Bayesian confidence
            
            // Both models strongly agree
            mlConfidence > 0.6f && bayesianConfidence > 0.7f -> true
            
            // Default: not spam (more conservative)
            else -> false
        }
        
        Log.d("SpamDetector", "Conservative hybrid decision for '$text': ML=$mlConfidence, Bayesian=$bayesianConfidence, IsSpam=$isSpam")
        
        return isSpam
    }
    
    suspend fun getSpamProbability(text: String): Float {
        val mlConfidence = getMLSpamProbability(text)
        val bayesianConfidence = getBayesianSpamProbability(text)
        
        // Weighted combination: TFLite gets more weight when confident
        val finalProbability = when {
            mlConfidence > 0.7f || mlConfidence < 0.3f -> {
                // TFLite is confident, give it 80% weight
                0.8f * mlConfidence + 0.2f * bayesianConfidence
            }
            else -> {
                // TFLite is uncertain, use Bayesian more
                0.3f * mlConfidence + 0.7f * bayesianConfidence
            }
        }
        
        Log.d("SpamDetector", "Final probability for '$text': ML=$mlConfidence, Bayesian=$bayesianConfidence, Final=$finalProbability")
        
        return finalProbability.coerceIn(0f, 1f)
    }

    fun getMatchedKeywords(text: String): Set<String> {
        // Extract potential spam keywords from the text
        val spamKeywords = setOf(
            "win", "free", "congrats", "lottery", "prize", "winner", "claim", "urgent", 
            "limited time", "act now", "click here", "unlimited", "guaranteed", "risk-free", 
            "no cost", "no purchase", "no obligation", "no catch", "no strings", 
            "no hidden", "no fees", "cash", "money", "dollars", "million", "billion"
        )
        
        val lowerText = text.lowercase()
        return spamKeywords.filter { keyword -> 
            lowerText.contains(keyword.lowercase()) 
        }.toSet()
    }

    fun cleanup() {
        tfliteModel.close()
    }
} 