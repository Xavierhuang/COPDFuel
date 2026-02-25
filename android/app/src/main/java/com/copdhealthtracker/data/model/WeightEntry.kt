package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weight: Double,
    val isGoal: Boolean = false,
    val date: Long = System.currentTimeMillis()
)
