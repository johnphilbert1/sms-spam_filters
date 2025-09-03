package com.example.smsspamfilterapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    @ColumnInfo(typeAffinity = androidx.room.ColumnInfo.TEXT)
    val content: String,
    val timestamp: Long,
    val isSpam: Boolean,
    val mlConfidence: Float = 0f,
    val bayesianConfidence: Float = 0f,
    @ColumnInfo(typeAffinity = androidx.room.ColumnInfo.TEXT)
    val matchedKeywords: String = ""
) 