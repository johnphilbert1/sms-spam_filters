package com.example.smsspamfilterapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.smsspamfilterapp.R
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.BayesianRepository
import com.example.smsspamfilterapp.data.Message
import com.example.smsspamfilterapp.data.MessageRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class MessageDetailsActivity : AppCompatActivity() {
    private lateinit var viewModel: MessageViewModel
    private var messageId: Long = 0

    companion object {
        private const val EXTRA_MESSAGE_ID = "message_id"
        
        fun newIntent(context: Context, messageId: Long): Intent {
            return Intent(context, MessageDetailsActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_details)

        messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, 0)
        if (messageId == 0L) {
            Toast.makeText(this, "Invalid message", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewModel()
        setupToolbar()
        observeMessage()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(application)
        val messageRepository = MessageRepository(database.messageDao())
        val bayesianRepository = BayesianRepository(database.wordFrequencyDao())
        
        viewModel = ViewModelProvider(this, MessageViewModel.Factory(messageRepository, bayesianRepository))[MessageViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Message Details"
    }

    private fun observeMessage() {
        viewModel.getMessageById(messageId).observe(this) { message ->
            message?.let { displayMessage(it) }
        }
    }

    private fun displayMessage(message: Message) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        findViewById<TextView>(R.id.senderTextView).text = message.sender
        findViewById<TextView>(R.id.timestampTextView).text = dateFormat.format(message.timestamp)
        findViewById<TextView>(R.id.messageTextView).text = message.content

        // Set ML confidence
        val mlConfidence = (message.mlConfidence * 100).roundToInt()
        findViewById<LinearProgressIndicator>(R.id.mlConfidenceIndicator).progress = mlConfidence
        findViewById<TextView>(R.id.mlConfidenceValue).text = "$mlConfidence%"

        // Set Bayesian confidence
        val bayesianConfidence = (message.bayesianConfidence * 100).roundToInt()
        findViewById<LinearProgressIndicator>(R.id.bayesianConfidenceIndicator).progress = bayesianConfidence
        findViewById<TextView>(R.id.bayesianConfidenceValue).text = "$bayesianConfidence%"

        // Set spam keywords
        val keywordsChipGroup = findViewById<ChipGroup>(R.id.keywordsChipGroup)
        keywordsChipGroup.removeAllViews()
        message.matchedKeywords.split(",").filter { it.isNotBlank() }.forEach { keyword ->
            val chip = Chip(this)
            chip.text = keyword
            chip.isCheckable = false
            keywordsChipGroup.addView(chip)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
