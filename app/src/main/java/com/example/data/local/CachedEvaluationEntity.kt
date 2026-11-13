package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_evaluations",
    indices = [
        Index(value = ["cache_key"], unique = true)
    ]
)
data class CachedEvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    val language: String,
    @ColumnInfo(name = "target_phrase") val targetPhrase: String,
    @ColumnInfo(name = "spoken_transcript") val spokenTranscript: String,
    val score: Int,
    @ColumnInfo(name = "phonetic_accuracy") val phoneticAccuracy: Int,
    @ColumnInfo(name = "intonation_pitch") val intonationPitch: Int,
    val fluency: Int,
    @ColumnInfo(name = "phonetic_breakdown") val phoneticBreakdown: String,
    @ColumnInfo(name = "mispronounced_json") val mispronouncedJson: String,
    @ColumnInfo(name = "coaching_tip") val coachingTip: String,
    @ColumnInfo(name = "native_phrasings_json") val nativePhrasingsJson: String,
    val encouragement: String,
    @ColumnInfo(name = "pitch_contour_type") val pitchContourType: String,
    @ColumnInfo(name = "target_pitch_json") val targetPitchJson: String,
    @ColumnInfo(name = "learner_pitch_json") val learnerPitchJson: String,
    @ColumnInfo(name = "pitch_guidance") val pitchGuidance: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
