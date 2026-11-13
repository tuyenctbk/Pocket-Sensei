package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AppEventLog): Long

    @Query("SELECT * FROM app_events ORDER BY timestamp_ms DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 25): Flow<List<AppEventLog>>

    @Query("SELECT COUNT(*) FROM app_events")
    fun observeTotalEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_events WHERE event_name = :eventName")
    suspend fun countEventsByName(eventName: String): Int

    @Query("SELECT COUNT(*) FROM app_events WHERE event_name = :eventName AND timestamp_ms >= :sinceMs")
    suspend fun countEventsSince(eventName: String, sinceMs: Long): Int

    @Query("DELETE FROM app_events WHERE timestamp_ms < :olderThanMs")
    suspend fun pruneOldEvents(olderThanMs: Long)
}
