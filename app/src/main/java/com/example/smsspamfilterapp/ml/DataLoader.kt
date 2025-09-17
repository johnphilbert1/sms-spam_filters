package com.example.smsspamfilterapp.ml

import android.content.Context
import com.example.smsspamfilterapp.spam.BayesianFilter
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class DataLoader(private val context: Context) {
    companion object {
        private const val BAYESIAN_MODEL_FILE = "merged_bayesian_model.json"
        private const val TFLITE_MODEL_FILE = "spam_model.tflite"
        private const val TOKENIZER_FILE = "tokenizer.json"
    }

    fun loadBayesianModel(): BayesianFilter {
        val bayesianFilter = BayesianFilter()
        bayesianFilter.loadPreTrainedData(context)
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
