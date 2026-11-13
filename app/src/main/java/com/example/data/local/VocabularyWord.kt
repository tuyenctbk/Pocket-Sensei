package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_words",
    indices = [
        Index(value = ["word", "language"], unique = true),
        Index(value = ["remote_id"])
    ]
)
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val phonetics: String,
    @ColumnInfo(name = "mastery_level") val masteryLevel: Int = 1,
    val language: String = "Japanese",
    @ColumnInfo(name = "pronunciation_score") val pronunciationScore: Int = 0,
    @ColumnInfo(name = "practice_count") val practiceCount: Int = 1,
    @ColumnInfo(name = "remote_id") val remoteId: String = "",
    @ColumnInfo(name = "last_practiced_at") val lastPracticedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "next_review_at") val nextReviewAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 1
)
