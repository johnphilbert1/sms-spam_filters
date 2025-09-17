package com.example.smsspamfilterapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smsspamfilterapp.R
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.BayesianRepository
import com.example.smsspamfilterapp.data.MessageRepository
import com.example.smsspamfilterapp.data.Message
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InboxFragment : Fragment() {
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    private val messageRepository by lazy { MessageRepository(database.messageDao()) }
    private val bayesianRepository by lazy { BayesianRepository(database.wordFrequencyDao()) }
    
    private val viewModel: MessageViewModel by viewModels {
        MessageViewModel.Factory(messageRepository, bayesianRepository)
    }
    
    private lateinit var adapter: SimpleMessageAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var loadingProgressBar: android.widget.ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView(view)
        observeData()
    }

    private fun setupViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        searchEditText.setOnEditorActionListener { _, _, _ ->
            loadingProgressBar.visibility = android.view.View.VISIBLE
            viewModel.searchMessages(searchEditText.text.toString())
            true
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.messagesRecyclerView)
        adapter = SimpleMessageAdapter(
            onMessageClick = { message ->
                // Open message details
                startActivity(MessageDetailsActivity.newIntent(requireContext(), message.id))
            },
            onLongPress = { message ->
                // Show context menu for quick actions
                showContextMenu(message)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun showContextMenu(message: Message) {
        val options = arrayOf("Mark as Spam", "Delete")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Message Actions")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> markAsSpam(message)
                    1 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun markAsSpam(message: Message) {
        AlertDialog.Builder(requireContext())
            .setTitle("Mark as Spam")
            .setMessage("Are you sure you want to mark this message as spam?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.markAsSpam(message)
                viewModel.trainSpam(message)
                android.widget.Toast.makeText(requireContext(), "Marked as spam", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMessage(message)
                android.widget.Toast.makeText(requireContext(), "Message deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inboxMessages.collectLatest { messages ->
                    adapter.submitList(messages)
                    loadingProgressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
} 