package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_entries")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val minutes: Int,
    val date: Long = System.currentTimeMillis()
)
