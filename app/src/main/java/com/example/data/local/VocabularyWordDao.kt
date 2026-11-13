package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyWordDao {

    @Query("SELECT * FROM vocabulary_words ORDER BY last_practiced_at DESC")
    fun observeAllWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE LOWER(language) = LOWER(:language) ORDER BY last_practiced_at DESC")
    fun observeWordsByLanguage(language: String): Flow<List<VocabularyWord>>

    @Query("SELECT COUNT(*) FROM vocabulary_words WHERE next_review_at <= :nowMs")
    fun observeDueCount(nowMs: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT * FROM vocabulary_words WHERE word = :word AND LOWER(language) = LOWER(:language) LIMIT 1")
    suspend fun findByWordAndLanguage(word: String, language: String): VocabularyWord?

    @Query("SELECT * FROM vocabulary_words WHERE remote_id = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): VocabularyWord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(word: VocabularyWord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(words: List<VocabularyWord>)

    @Update
    suspend fun updateWord(word: VocabularyWord)

    @Query(
        "UPDATE vocabulary_words " +
            "SET mastery_level = :masteryLevel, " +
            "pronunciation_score = :pronunciationScore, " +
            "practice_count = practice_count + 1, " +
            "last_practiced_at = :practicedAt, " +
            "next_review_at = :nextReviewAt, " +
            "interval_days = :intervalDays " +
            "WHERE id = :id"
    )
    suspend fun recordPracticeAttempt(
        id: Int,
        masteryLevel: Int,
        pronunciationScore: Int,
        practicedAt: Long = System.currentTimeMillis(),
        nextReviewAt: Long = practicedAt,
        intervalDays: Int = 1
    )

    @Delete
    suspend fun deleteWord(word: VocabularyWord)

    @Query("DELETE FROM vocabulary_words WHERE id = :id")
    suspend fun deleteWordById(id: Int)

    @Query("DELETE FROM vocabulary_words")
    suspend fun clearAll()
}
