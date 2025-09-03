package com.example.smsspamfilterapp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smsspamfilterapp.data.BayesianRepository
import com.example.smsspamfilterapp.data.Message
import com.example.smsspamfilterapp.data.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessageViewModel(
    private val messageRepository: MessageRepository,
    private val bayesianRepository: BayesianRepository
) : ViewModel() {

    private val _inboxMessages = MutableStateFlow<List<Message>>(emptyList())
    val inboxMessages: StateFlow<List<Message>> = _inboxMessages.asStateFlow()

    private val _spamMessages = MutableStateFlow<List<Message>>(emptyList())
    val spamMessages: StateFlow<List<Message>> = _spamMessages.asStateFlow()

    init {
        loadMessages()
        loadTrainingData()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getInboxMessages().collect { messages ->
                _inboxMessages.value = messages
            }
        }
        viewModelScope.launch {
            messageRepository.getSpamMessages().collect { messages ->
                _spamMessages.value = messages
            }
        }
    }

    private fun loadTrainingData() {
        viewModelScope.launch {
            bayesianRepository.loadTrainingData()
        }
    }

    fun searchMessages(query: String) {
        viewModelScope.launch {
            messageRepository.searchInboxMessages(query).collect { messages ->
                _inboxMessages.value = messages
            }
        }
    }

    fun searchSpamMessages(query: String) {
        viewModelScope.launch {
            messageRepository.searchSpamMessages(query).collect { messages ->
                _spamMessages.value = messages
            }
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.deleteMessage(message)
        }
    }

    fun restoreMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.restoreMessage(message)
        }
    }

    fun markAsSpam(message: Message) {
        viewModelScope.launch {
            messageRepository.markAsSpam(message)
        }
    }

    fun trainSpam(message: Message) {
        viewModelScope.launch {
            bayesianRepository.trainSpam(message.content)
        }
    }

    fun trainHam(message: Message) {
        viewModelScope.launch {
            bayesianRepository.trainHam(message.content)
        }
    }

    fun calculateSpamProbability(message: String): Double {
        return bayesianRepository.calculateSpamProbability(message)
    }

    fun getMessageById(messageId: Long): LiveData<Message?> {
        val liveData = MutableLiveData<Message?>()
        viewModelScope.launch {
            val message = messageRepository.getMessageById(messageId)
            liveData.value = message
            _currentMessage.value = message
        }
        return liveData
    }

    fun getCurrentMessage(): Message? {
        return _currentMessage.value
    }

    private val _currentMessage = MutableLiveData<Message?>()

    class Factory(
        private val messageRepository: MessageRepository,
        private val bayesianRepository: BayesianRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
                return MessageViewModel(messageRepository, bayesianRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 