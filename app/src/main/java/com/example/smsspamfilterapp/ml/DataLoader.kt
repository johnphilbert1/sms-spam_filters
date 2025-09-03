package com.example.smsspamfilterapp.ml

import android.content.Context
import com.example.smsspamfilterapp.spam.BayesianFilter
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class DataLoader(private val context: Context) {
    companion object {
        private const val BAYESIAN_MODEL_FILE = "bayesian_model.json"
        private const val TFLITE_MODEL_FILE = "spam_model.tflite"
        private const val TOKENIZER_FILE = "tokenizer.json"
    }

    fun loadBayesianModel(): BayesianFilter {
        val bayesianFilter = BayesianFilter()
        try {
            context.assets.open(BAYESIAN_MODEL_FILE).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                val jsonObject = JSONObject(jsonString)

                // Load spam word counts
                val spamWordCounts = jsonObject.getJSONObject("spam_word_counts")
                spamWordCounts.keys().forEach { word ->
                    repeat(spamWordCounts.getInt(word)) {
                        bayesianFilter.trainSpam(word)
                    }
                }

                // Load ham word counts
                val hamWordCounts = jsonObject.getJSONObject("ham_word_counts")
                hamWordCounts.keys().forEach { word ->
                    repeat(hamWordCounts.getInt(word)) {
                        bayesianFilter.trainHam(word)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bayesianFilter
    }

    fun copyModelsToAssets() {
        try {
            // Copy models from training/data to assets
            context.assets.list("")?.forEach { fileName ->
                when (fileName) {
                    BAYESIAN_MODEL_FILE,
                    TFLITE_MODEL_FILE,
                    TOKENIZER_FILE -> {
                        context.assets.open(fileName).use { input ->
                            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
