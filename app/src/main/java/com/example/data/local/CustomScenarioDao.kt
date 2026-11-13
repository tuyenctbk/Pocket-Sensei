package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomScenarioDao {

    @Query("SELECT * FROM custom_scenarios ORDER BY created_at DESC")
    fun observeAllScenarios(): Flow<List<CustomScenarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: CustomScenarioEntity): Long

    @Query("DELETE FROM custom_scenarios WHERE id = :id")
    suspend fun deleteScenarioById(id: Int)
}
