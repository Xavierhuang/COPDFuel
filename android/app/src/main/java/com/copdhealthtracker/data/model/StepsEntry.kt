package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps_entries")
data class StepsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val count: Int,
    val date: Long = System.currentTimeMillis()
)
