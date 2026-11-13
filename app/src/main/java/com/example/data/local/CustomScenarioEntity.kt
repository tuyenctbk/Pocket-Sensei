package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_scenarios")
data class CustomScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    @ColumnInfo(name = "sensei_role") val senseiRole: String,
    @ColumnInfo(name = "user_role") val userRole: String,
    @ColumnInfo(name = "opening_line") val openingLine: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
