package com.example.smsspamfilterapp.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

class TFLiteModel(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val preprocessor = TextPreprocessor()
    private val tokenizer = mutableMapOf<String, Int>()
    
    companion object {
        private const val MODEL_FILE = "spam_model.tflite"
        private const val TOKENIZER_FILE = "tokenizer.json"
        private const val VOCAB_SIZE = 10000
        private const val MAX_SEQUENCE_LENGTH = 100
        private const val INPUT_SIZE = MAX_SEQUENCE_LENGTH
        private const val OUTPUT_SIZE = 1
    }

    init {
        loadModel()
        loadTokenizer()
    }

    private fun loadModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelFile, options)
            android.util.Log.d("TFLiteModel", "Model loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("TFLiteModel", "Error loading model: ${e.message}", e)
            // Provide fallback behavior
            interpreter = null
        }
    }

    private fun loadTokenizer() {
        try {
            val inputStream = context.assets.open(TOKENIZER_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val config = jsonObject.getJSONObject("config")
            val wordCounts = JSONObject(config.getString("word_counts"))
            
            // Build tokenizer mapping (word -> index)
            val iterator = wordCounts.keys()
            var index = 1 // Start from 1, 0 is reserved for padding
            while (iterator.hasNext() && index < VOCAB_SIZE) {
                val word = iterator.next()
                tokenizer[word] = index
                index++
            }
            
            android.util.Log.d("TFLiteModel", "Tokenizer loaded with ${tokenizer.size} words")
        } catch (e: Exception) {
            android.util.Log.e("TFLiteModel", "Error loading tokenizer: ${e.message}", e)
        }
    }

    fun predict(text: String): Float {
        if (interpreter == null) {
            android.util.Log.w("TFLiteModel", "Interpreter not loaded, returning neutral prediction")
            return 0.5f
        }
        
        try {
            // Validate input
            if (text.isBlank()) {
                android.util.Log.w("TFLiteModel", "Empty input text, returning neutral prediction")
                return 0.5f
            }
            
            val preprocessedText = preprocessor.preprocess(text)
            android.util.Log.d("TFLiteModel", "Input text: '$text'")
            android.util.Log.d("TFLiteModel", "Preprocessed text: '$preprocessedText'")
            
            // Check if text is empty or too short
            if (preprocessedText.isBlank()) {
                android.util.Log.w("TFLiteModel", "Empty preprocessed text, returning neutral prediction")
                return 0.5f
            }
            
            val inputBuffer = preprocessInput(preprocessedText)
            android.util.Log.d("TFLiteModel", "Input buffer created successfully")
            
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, OUTPUT_SIZE), org.tensorflow.lite.DataType.FLOAT32)
            
                        // Add error handling for model inference
            try {
                interpreter?.run(inputBuffer.buffer, outputBuffer.buffer)
            } catch (e: Exception) {
                android.util.Log.e("TFLiteModel", "Model inference failed: ${e.message}", e)
                return 0.5f // Return neutral prediction on inference failure
            }
            
            val prediction = outputBuffer.floatArray[0]
            android.util.Log.d("TFLiteModel", "Raw prediction: $prediction for text: '$text'")
            
            // Check for invalid prediction values
            if (prediction.isNaN() || prediction.isInfinite()) {
                android.util.Log.w("TFLiteModel", "Invalid prediction value: $prediction, returning neutral")
                return 0.5f
            }
            
            // Ensure prediction is between 0 and 1
            val clampedPrediction = prediction.coerceIn(0f, 1f)
            android.util.Log.d("TFLiteModel", "Final prediction: $clampedPrediction for text: '$text'")
            
            return clampedPrediction
        } catch (e: Exception) {
            android.util.Log.e("TFLiteModel", "Error during prediction: ${e.message}", e)
            return 0.5f // Neutral prediction on error
        }
    }

    private fun preprocessInput(text: String): TensorBuffer {
        val tokens = preprocessor.tokenize(text)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, INPUT_SIZE), org.tensorflow.lite.DataType.FLOAT32)
        
        // Convert tokens to numerical features using proper tokenizer
        val features = FloatArray(INPUT_SIZE)
        tokens.take(MAX_SEQUENCE_LENGTH).forEachIndexed { index, token ->
            // Use tokenizer to get proper index, fallback to 0 for unknown words
            val tokenIndex = tokenizer[token] ?: 0
            features[index] = tokenIndex.toFloat()
        }
        
        // Log the features for debugging
        android.util.Log.d("TFLiteModel", "Input features: ${features.take(10).joinToString(",")}... (total: ${features.size})")
        
        inputBuffer.loadArray(features)
        return inputBuffer
    }

    fun close() {
        interpreter?.close()
    }
} 