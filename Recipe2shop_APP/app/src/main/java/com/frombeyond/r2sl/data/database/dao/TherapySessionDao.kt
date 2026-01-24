package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.TherapySession
import kotlinx.coroutines.flow.Flow

@Dao
interface TherapySessionDao {
    
    @Query("SELECT * FROM therapy_sessions WHERE therapistId = :therapistId ORDER BY sessionDate DESC")
    fun getSessionsByTherapist(therapistId: String): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE patientId = :patientId AND therapistId = :therapistId ORDER BY sessionDate DESC")
    fun getSessionsByPatient(patientId: String, therapistId: String): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE therapistId = :therapistId AND sessionDate >= :startDate ORDER BY sessionDate ASC")
    fun getUpcomingSessionsByTherapist(therapistId: String, startDate: Long): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE therapistId = :therapistId AND sessionStatus = 'planned' ORDER BY sessionDate ASC")
    fun getPlannedSessionsByTherapist(therapistId: String): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE therapistId = :therapistId AND sessionStatus = 'completed' ORDER BY sessionDate DESC")
    fun getCompletedSessionsByTherapist(therapistId: String): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE therapistId = :therapistId AND isUrgent = 1 ORDER BY sessionDate ASC")
    fun getUrgentSessionsByTherapist(therapistId: String): Flow<List<TherapySession>>
    
    @Query("SELECT * FROM therapy_sessions WHERE id = :sessionId AND therapistId = :therapistId")
    suspend fun getSessionById(sessionId: Long, therapistId: String): TherapySession?
    
    @Query("SELECT COUNT(*) FROM therapy_sessions WHERE therapistId = :therapistId AND sessionStatus = 'completed'")
    suspend fun getCompletedSessionsCountByTherapist(therapistId: String): Int
    
    @Query("SELECT COUNT(*) FROM therapy_sessions WHERE patientId = :patientId AND therapistId = :therapistId AND sessionStatus = 'completed'")
    suspend fun getCompletedSessionsCountByPatient(patientId: String, therapistId: String): Int
    
    @Insert
    suspend fun insertSession(session: TherapySession)
    
    @Update
    suspend fun updateSession(session: TherapySession)
    
    @Delete
    suspend fun deleteSession(session: TherapySession)
    
    @Query("UPDATE therapy_sessions SET sessionStatus = :status WHERE id = :sessionId AND therapistId = :therapistId")
    suspend fun updateSessionStatus(sessionId: Long, therapistId: String, status: String)
    
    @Query("UPDATE therapy_sessions SET updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: Long, timestamp: Long = System.currentTimeMillis())
}
