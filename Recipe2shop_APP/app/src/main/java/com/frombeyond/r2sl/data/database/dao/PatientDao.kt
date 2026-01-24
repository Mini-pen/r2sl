package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    
    @Query("SELECT * FROM patients WHERE therapistId = :therapistId AND isActive = 1 ORDER BY lastName, firstName")
    fun getPatientsByTherapist(therapistId: String): Flow<List<Patient>>
    
    @Query("SELECT * FROM patients WHERE therapistId = :therapistId AND isActive = 1 AND (firstName LIKE '%' || :searchQuery || '%' OR lastName LIKE '%' || :searchQuery || '%') ORDER BY lastName, firstName")
    fun searchPatientsByTherapist(therapistId: String, searchQuery: String): Flow<List<Patient>>
    
    @Query("SELECT * FROM patients WHERE id = :patientId AND therapistId = :therapistId")
    suspend fun getPatientById(patientId: String, therapistId: String): Patient?
    
    @Query("SELECT COUNT(*) FROM patients WHERE therapistId = :therapistId AND isActive = 1")
    suspend fun getPatientCountByTherapist(therapistId: String): Int
    
    @Query("SELECT * FROM patients WHERE therapistId = :therapistId AND isActive = 1 ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentPatientsByTherapist(therapistId: String): Flow<List<Patient>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)
    
    @Update
    suspend fun updatePatient(patient: Patient)
    
    @Delete
    suspend fun deletePatient(patient: Patient)
    
    @Query("UPDATE patients SET isActive = :isActive WHERE id = :patientId AND therapistId = :therapistId")
    suspend fun updatePatientStatus(patientId: String, therapistId: String, isActive: Boolean)
    
    @Query("UPDATE patients SET updatedAt = :timestamp WHERE id = :patientId")
    suspend fun updatePatientTimestamp(patientId: String, timestamp: Long = System.currentTimeMillis())
}
