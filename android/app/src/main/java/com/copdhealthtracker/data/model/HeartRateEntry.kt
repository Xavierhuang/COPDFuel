package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_entries")
data class HeartRateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bpm: Int,
    val date: Long = System.currentTimeMillis()
)
