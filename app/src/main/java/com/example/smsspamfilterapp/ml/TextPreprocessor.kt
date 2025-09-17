package com.example.smsspamfilterapp.ml

import android.util.Log

class TextPreprocessor {
    
    companion object {
        private const val TAG = "TextPreprocessor"
        
        // Common ham words that should reduce spam probability
        private val HAM_WHITELIST = setOf(
            "meeting", "thanks", "family", "friend", "hello", "hi", "good", "morning", 
            "afternoon", "evening", "night", "work", "home", "office", "school", 
            "university", "college", "study", "exam", "test", "project", "assignment",
            "dinner", "lunch", "breakfast", "coffee", "tea", "water", "food", "eat",
            "drink", "sleep", "rest", "exercise", "gym", "run", "walk", "drive", "car",
            "bus", "train", "plane", "travel", "vacation", "holiday", "birthday", "party",
            "wedding", "anniversary", "congratulations", "happy", "sad", "tired", "busy",
            "free", "available", "sorry", "please", "help", "support", "service", "customer"
        )
    }
    
    fun preprocess(text: String): String {
        if (text.isBlank()) return text
        
        var cleaned = text
        
        // First, convert to lowercase for consistent processing
        cleaned = cleaned.lowercase()
        
        // Remove emojis and special characters
        cleaned = removeEmojis(cleaned)
        
        // Clean punctuation and special characters
        cleaned = cleanPunctuation(cleaned)
        
        // Normalize whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
        
        Log.d(TAG, "Original: '$text' -> Cleaned: '$cleaned'")
        
        return cleaned
    }
    
    fun tokenize(text: String): List<String> {
        return text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
    
    fun hasHamWords(text: String): Boolean {
        val lowerText = text.lowercase()
        return HAM_WHITELIST.any { hamWord -> 
            lowerText.contains(hamWord.lowercase()) 
        }
    }
    
    private fun removeEmojis(text: String): String {
        // Avoid unsupported regex escapes on Android: filter by Unicode code points instead
        val builder = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val isEmoji = isEmojiCodePoint(codePoint)
            if (!isEmoji) {
                builder.appendCodePoint(codePoint)
            }
            i += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    private fun isEmojiCodePoint(cp: Int): Boolean {
        // Basic Emoji ranges (non-exhaustive but covers common sets)
        return (
            // Emoticons
            (cp in 0x1F600..0x1F64F) ||
            // Misc Symbols and Pictographs
            (cp in 0x1F300..0x1F5FF) ||
            // Transport and Map Symbols
            (cp in 0x1F680..0x1F6FF) ||
            // Regional Indicator Symbols
            (cp in 0x1F1E0..0x1F1FF) ||
            // Misc symbols
            (cp in 0x2600..0x26FF) ||
            // Dingbats
            (cp in 0x2700..0x27BF)
        )
    }
    
    private fun cleanPunctuation(text: String): String {
        // Replace multiple punctuation with single
        var cleaned = text.replace(Regex("!{2,}"), "!")
        cleaned = cleaned.replace(Regex("\\?{2,}"), "?")
        cleaned = cleaned.replace(Regex("\\.{2,}"), ".")
        
        // Remove excessive punctuation but keep basic structure
        cleaned = cleaned.replace(Regex("[^a-z0-9\\s!?.,]"), " ")
        
        return cleaned
    }
} 