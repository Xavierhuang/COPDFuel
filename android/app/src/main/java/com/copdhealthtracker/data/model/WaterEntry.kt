package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int, // in ounces
    val date: Long = System.currentTimeMillis()
)
