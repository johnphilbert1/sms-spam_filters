package com.example.smsspamfilterapp.data

import com.example.smsspamfilterapp.spam.BayesianFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BayesianRepository(private val wordFrequencyDao: WordFrequencyDao) {
    private val bayesianFilter = BayesianFilter()

    suspend fun trainSpam(message: String) {
        val words = message.split(Regex("\\s+"))
        words.forEach { word ->
            val frequency = wordFrequencyDao.getWordFrequency(word) ?: WordFrequency(word, 0, 0)
            wordFrequencyDao.insertWordFrequency(
                frequency.copy(spamCount = frequency.spamCount + 1)
            )
        }
        bayesianFilter.trainSpam(message)
    }

    suspend fun trainHam(message: String) {
        val words = message.split(Regex("\\s+"))
        words.forEach { word ->
            val frequency = wordFrequencyDao.getWordFrequency(word) ?: WordFrequency(word, 0, 0)
            wordFrequencyDao.insertWordFrequency(
                frequency.copy(hamCount = frequency.hamCount + 1)
            )
        }
        bayesianFilter.trainHam(message)
    }

    fun calculateSpamProbability(message: String): Double {
        return bayesianFilter.calculateSpamProbability(message)
    }

    fun getWordStats(): Flow<Map<String, BayesianFilter.WordStats>> {
        return wordFrequencyDao.getAllWordFrequencies().map { frequencies ->
            frequencies.associate { frequency ->
                frequency.word to BayesianFilter.WordStats(
                    word = frequency.word,
                    spamCount = frequency.spamCount,
                    hamCount = frequency.hamCount,
                    spamProbability = if (frequency.spamCount + frequency.hamCount > 0) {
                        frequency.spamCount.toDouble() / (frequency.spamCount + frequency.hamCount)
                    } else {
                        0.5
                    }
                )
            }
        }
    }

    suspend fun loadTrainingData() {
        wordFrequencyDao.getAllWordFrequencies().collect { frequencies ->
            frequencies.forEach { frequency ->
                repeat(frequency.spamCount) {
                    bayesianFilter.trainSpam(frequency.word)
                }
                repeat(frequency.hamCount) {
                    bayesianFilter.trainHam(frequency.word)
                }
            }
        }
    }
} 