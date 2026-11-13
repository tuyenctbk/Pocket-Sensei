package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_phrases")
data class CustomPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val language: String,
    @ColumnInfo(name = "target_text") val targetText: String,
    val phonetic: String,
    val translation: String,
    @ColumnInfo(name = "accent_focus") val accentFocus: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
