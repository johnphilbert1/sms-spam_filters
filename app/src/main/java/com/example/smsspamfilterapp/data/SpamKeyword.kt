package com.example.smsspamfilterapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_keywords")
data class SpamKeyword(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val weight: Float = 1.0f
) 