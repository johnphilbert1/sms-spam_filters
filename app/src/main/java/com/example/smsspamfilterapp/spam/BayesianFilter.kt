package com.example.smsspamfilterapp.spam

import kotlin.math.ln

class BayesianFilter {
    private val spamWordCounts = mutableMapOf<String, Int>()
    private val hamWordCounts = mutableMapOf<String, Int>()
    private var totalSpamMessages = 0
    private var totalHamMessages = 0
    
    fun loadPreTrainedData(context: android.content.Context) {
        try {
            // Load the merged Bayesian model
            val inputStream = context.assets.open("merged_bayesian_model.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            
            // Load word frequencies from the merged model
            val wordFrequencies = jsonObject.getJSONObject("word_frequencies")
            val wordIterator = wordFrequencies.keys()
            
            while (wordIterator.hasNext()) {
                val word = wordIterator.next()
                val wordData = wordFrequencies.getJSONObject(word)
                val spamCount = wordData.getInt("spam_count")
                val hamCount = wordData.getInt("ham_count")
                
                spamWordCounts[word] = spamCount
                hamWordCounts[word] = hamCount
                totalSpamMessages += spamCount
                totalHamMessages += hamCount
            }
            
            val version = jsonObject.optString("version", "unknown")
            val totalWords = jsonObject.optInt("total_words", 0)
            val modelType = jsonObject.optString("model_type", "unknown")
            
            android.util.Log.d("BayesianFilter", "Loaded merged model v$version ($modelType): $totalWords words, ${totalSpamMessages} spam, ${totalHamMessages} ham")
            
        } catch (e: Exception) {
            android.util.Log.e("BayesianFilter", "Error loading merged Bayesian model: ${e.message}", e)
            android.util.Log.w("BayesianFilter", "Falling back to empty model - spam detection will be less accurate")
        }
    }

    fun trainSpam(message: String) {
        totalSpamMessages++
        message.split(Regex("\\s+")).forEach { word ->
            spamWordCounts[word] = (spamWordCounts[word] ?: 0) + 1
        }
    }

    fun trainHam(message: String) {
        totalHamMessages++
        message.split(Regex("\\s+")).forEach { word ->
            hamWordCounts[word] = (hamWordCounts[word] ?: 0) + 1
        }
    }

    fun calculateSpamProbability(message: String): Double {
        // Handle case with no training data
        if (totalSpamMessages == 0 && totalHamMessages == 0) {
            android.util.Log.w("BayesianFilter", "No training data available, returning neutral probability")
            return 0.5
        }
        
        val words = message.split(Regex("\\s+"))
        var spamProbability = 0.0
        var hamProbability = 0.0

        words.forEach { word ->
            val spamCount = spamWordCounts[word] ?: 0
            val hamCount = hamWordCounts[word] ?: 0

            // Calculate word probabilities using Laplace smoothing
            val spamProb = (spamCount + 1.0) / (totalSpamMessages + 2.0)
            val hamProb = (hamCount + 1.0) / (totalHamMessages + 2.0)

            // Add log probabilities to avoid underflow
            spamProbability += ln(spamProb)
            hamProbability += ln(hamProb)
        }

        // Calculate final probability using Bayes' theorem
        val spamPrior = totalSpamMessages.toDouble() / (totalSpamMessages + totalHamMessages)
        val hamPrior = totalHamMessages.toDouble() / (totalSpamMessages + totalHamMessages)

        val spamScore = spamProbability + ln(spamPrior)
        val hamScore = hamProbability + ln(hamPrior)

        val result = 1.0 / (1.0 + Math.exp(hamScore - spamScore))
        android.util.Log.d("BayesianFilter", "Spam probability for '$message': $result")
        return result
    }

    fun getWordStats(): Map<String, WordStats> {
        val allWords = (spamWordCounts.keys + hamWordCounts.keys).toSet()
        return allWords.associateWith { word ->
            WordStats(
                word = word,
                spamCount = spamWordCounts[word] ?: 0,
                hamCount = hamWordCounts[word] ?: 0,
                spamProbability = calculateWordSpamProbability(word)
            )
        }
    }

    private fun calculateWordSpamProbability(word: String): Double {
        val spamCount = spamWordCounts[word] ?: 0
        val hamCount = hamWordCounts[word] ?: 0
        val totalCount = spamCount + hamCount

        return if (totalCount > 0) {
            spamCount.toDouble() / totalCount
        } else {
            0.5 // Neutral probability for unknown words
        }
    }

    fun getSpamProbability(text: String): Float {
        if (spamWordCounts.isEmpty() || hamWordCounts.isEmpty()) {
            android.util.Log.w("BayesianFilter", "No training data available, returning neutral probability")
            return 0.5f
        }
        
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 0.5f
        
        var spamScore = 0.0
        var hamScore = 0.0
        
        words.forEach { word ->
            val spamCount = spamWordCounts[word] ?: 0
            val hamCount = hamWordCounts[word] ?: 0
            
            if (spamCount > 0 || hamCount > 0) {
                // Calculate log probabilities
                val spamProb = (spamCount + 1.0) / (totalSpamMessages + spamWordCounts.size)
                val hamProb = (hamCount + 1.0) / (totalHamMessages + hamWordCounts.size)
                
                spamScore += Math.log(spamProb)
                hamScore += Math.log(hamProb)
            }
        }
        
        // Apply whitelist protection
        val hasHamWords = text.lowercase().contains(Regex("(meeting|thanks|family|friend|hello|hi|good|morning|afternoon|evening|night|work|home|office|school|university|college|study|exam|test|project|assignment|dinner|lunch|breakfast|coffee|tea|water|food|eat|drink|sleep|rest|exercise|gym|run|walk|drive|car|bus|train|plane|travel|vacation|holiday|birthday|party|wedding|anniversary|congratulations|happy|sad|tired|busy|free|available|sorry|please|help|support|service|customer)"))
        
        if (hasHamWords) {
            // Reduce spam probability for messages with ham words
            spamScore -= 2.0 // Significant reduction
            android.util.Log.d("BayesianFilter", "Ham words detected, reducing spam score")
        }
        
        val spamProbability = 1.0 / (1.0 + Math.exp(hamScore - spamScore))
        
        android.util.Log.d("BayesianFilter", "Bayesian calculation for '$text': spamScore=$spamScore, hamScore=$hamScore, hasHamWords=$hasHamWords, finalProbability=$spamProbability")
        
        return spamProbability.toFloat()
    }

    data class WordStats(
        val word: String,
        val spamCount: Int,
        val hamCount: Int,
        val spamProbability: Double
    )
} 