package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY date DESC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE type = :type AND isDiscontinued = 0 ORDER BY date DESC")
    fun getMedicationsByType(type: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isDiscontinued = 1 ORDER BY discontinuedDate DESC")
    fun getDiscontinuedMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Long)
}
