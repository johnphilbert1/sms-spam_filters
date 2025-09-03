package com.example.smsspamfilterapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "spam_messages")
data class SpamMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Date,
    val matchedKeywords: String // Comma-separated list of matched keywords
) 