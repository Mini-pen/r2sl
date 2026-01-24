package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.Therapist
import kotlinx.coroutines.flow.Flow

@Dao
interface TherapistDao {
    
    @Query("SELECT * FROM therapists WHERE isActive = 1")
    fun getAllActiveTherapists(): Flow<List<Therapist>>
    
    @Query("SELECT * FROM therapists WHERE id = :therapistId")
    suspend fun getTherapistById(therapistId: String): Therapist?
    
    @Query("SELECT * FROM therapists WHERE email = :email")
    suspend fun getTherapistByEmail(email: String): Therapist?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTherapist(therapist: Therapist)
    
    @Update
    suspend fun updateTherapist(therapist: Therapist)
    
    @Delete
    suspend fun deleteTherapist(therapist: Therapist)
    
    @Query("UPDATE therapists SET isActive = 0 WHERE id = :therapistId")
    suspend fun deactivateTherapist(therapistId: String)
    
    @Query("UPDATE therapists SET lastLoginAt = :timestamp WHERE id = :therapistId")
    suspend fun updateLastLogin(therapistId: String, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM therapists WHERE isActive = 1")
    suspend fun getActiveTherapistCount(): Int
}
