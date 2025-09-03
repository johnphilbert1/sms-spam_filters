package com.example.smsspamfilterapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.SpamMessage
import com.example.smsspamfilterapp.data.SpamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpamViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SpamRepository
    private val _spamMessages = MutableStateFlow<List<SpamMessage>>(emptyList())
    val spamMessages: StateFlow<List<SpamMessage>> = _spamMessages.asStateFlow()

    private val _spamStats = MutableStateFlow(SpamStats())
    val spamStats: StateFlow<SpamStats> = _spamStats.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SpamRepository(database.spamMessageDao())
        
        viewModelScope.launch {
            repository.allSpamMessages.collect { messages ->
                _spamMessages.value = messages
                updateStats(messages)
            }
        }
    }

    private fun updateStats(messages: List<SpamMessage>) {
        val stats = SpamStats(
            totalSpam = messages.size,
            uniqueSenders = messages.map { it.sender }.distinct().size,
            mostCommonKeywords = messages
                .flatMap { it.matchedKeywords.split(",") }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .map { it.first }
        )
        _spamStats.value = stats
    }

    fun deleteSpamMessage(spamMessage: SpamMessage) {
        viewModelScope.launch {
            repository.deleteSpamMessage(spamMessage)
        }
    }

    fun clearAllSpam() {
        viewModelScope.launch {
            repository.deleteAllSpamMessages()
        }
    }
}

data class SpamStats(
    val totalSpam: Int = 0,
    val uniqueSenders: Int = 0,
    val mostCommonKeywords: List<String> = emptyList()
) 