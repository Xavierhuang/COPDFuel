package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val type: String, // "daily" or "exacerbation"
    val date: Long = System.currentTimeMillis()
)
