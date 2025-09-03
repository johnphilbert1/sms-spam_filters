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
        
        // Remove emojis and special characters
        cleaned = removeEmojis(cleaned)
        
        // Normalize text (lowercase but preserve some structure)
        cleaned = cleaned.lowercase()
        
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
        // Remove emoji characters (Unicode ranges for emojis)
        return text.replace(Regex("[\\u{1F600}-\\u{1F64F}]|[\\u{1F300}-\\u{1F5FF}]|[\\u{1F680}-\\u{1F6FF}]|[\\u{1F1E0}-\\u{1F1FF}]|[\\u{2600}-\\u{26FF}]|[\\u{2700}-\\u{27BF}]"), "")
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