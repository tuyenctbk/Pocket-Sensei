package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedEvaluationDao {

    @Query("SELECT * FROM cached_evaluations WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun findByKey(cacheKey: String): CachedEvaluationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: CachedEvaluationEntity): Long

    @Query("SELECT COUNT(*) FROM cached_evaluations")
    fun observeCacheCount(): Flow<Int>

    @Query("DELETE FROM cached_evaluations")
    suspend fun clearAll()
}
