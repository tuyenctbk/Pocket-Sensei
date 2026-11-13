package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPhraseDao {

    @Query("SELECT * FROM custom_phrases ORDER BY created_at DESC")
    fun observeAllPhrases(): Flow<List<CustomPhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: CustomPhraseEntity): Long

    @Query("DELETE FROM custom_phrases WHERE id = :id")
    suspend fun deletePhraseById(id: Int)
}
