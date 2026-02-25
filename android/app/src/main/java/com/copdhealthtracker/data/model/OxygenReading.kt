package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "oxygen_readings")
data class OxygenReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: Int,
    val date: Long = System.currentTimeMillis()
)
