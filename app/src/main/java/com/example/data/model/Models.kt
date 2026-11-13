package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val targetLanguage: String = "Japanese",
    val currentStreak: Long = 0L,
    val wordsMastered: Long = 0L,
    val totalMinutesPracticed: Long = 0L,
    val avgPronunciationScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VocabItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val word: String = "",
    val translation: String = "",
    val phonetic: String = "",
    val language: String = "Japanese",
    val masteryLevel: Long = 1L,
    val pronunciationScore: Long = 80L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PracticeSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scenarioTitle: String = "",
    val language: String = "Japanese",
    val turnsCount: Long = 0L,
    val avgScore: Long = 0L,
    val feedbackSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val targetLanguage: String = "Japanese",
    val leagueTier: String = "White Belt",
    val totalXp: Long = 0L,
    val streakDays: Long = 1L,
    val avgAccuracy: Long = 85L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

