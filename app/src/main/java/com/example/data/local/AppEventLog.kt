package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_events",
    indices = [
        Index(value = ["event_name"]),
        Index(value = ["timestamp_ms"])
    ]
)
data class AppEventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "event_name") val eventName: String,
    val category: String,
    @ColumnInfo(name = "metadata_json") val metadataJson: String = "{}",
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)
