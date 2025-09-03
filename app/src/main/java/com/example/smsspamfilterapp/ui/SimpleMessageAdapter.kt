package com.example.smsspamfilterapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smsspamfilterapp.R
import com.example.smsspamfilterapp.data.Message
import java.text.SimpleDateFormat
import java.util.Locale

class SimpleMessageAdapter(
    private val onMessageClick: (Message) -> Unit,
    private val onLongPress: (Message) -> Unit
) : ListAdapter<Message, SimpleMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_simple, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderTextView: TextView = itemView.findViewById(R.id.senderTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val spamIndicator: ImageView = itemView.findViewById(R.id.spamIndicator)

        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(message: Message) {
            senderTextView.text = message.sender
            timestampTextView.text = dateFormat.format(message.timestamp)
            messageTextView.text = message.content

            // Show spam indicator if message has high spam confidence
            val isLikelySpam = message.mlConfidence > 0.7 || message.bayesianConfidence > 0.7
            spamIndicator.visibility = if (isLikelySpam) View.VISIBLE else View.GONE

            // Set click listeners
            itemView.setOnClickListener {
                onMessageClick(message)
            }

            itemView.setOnLongClickListener {
                onLongPress(message)
                true
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
