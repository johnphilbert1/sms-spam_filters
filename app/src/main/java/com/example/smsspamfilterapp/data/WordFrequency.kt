package com.example.smsspamfilterapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_frequencies")
data class WordFrequency(
    @PrimaryKey
    val word: String,
    val spamCount: Int = 0,
    val hamCount: Int = 0
) 