package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.ExerciseSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {
    
    @Query("SELECT * FROM exercise_sessions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSessionsByUser(userId: String): Flow<List<ExerciseSession>>
    
    @Query("SELECT * FROM exercise_sessions WHERE userId = :userId AND exerciseType = :exerciseType ORDER BY createdAt DESC")
    fun getSessionsByUserAndType(userId: String, exerciseType: String): Flow<List<ExerciseSession>>
    
    @Query("SELECT * FROM exercise_sessions WHERE userId = :userId AND completed = 1 ORDER BY createdAt DESC")
    fun getCompletedSessionsByUser(userId: String): Flow<List<ExerciseSession>>
    
    @Query("SELECT COUNT(*) FROM exercise_sessions WHERE userId = :userId AND completed = 1")
    suspend fun getCompletedSessionsCount(userId: String): Int
    
    @Query("SELECT SUM(duration) FROM exercise_sessions WHERE userId = :userId AND completed = 1")
    suspend fun getTotalExerciseTime(userId: String): Int?
    
    @Query("SELECT * FROM exercise_sessions WHERE userId = :userId AND createdAt >= :startDate ORDER BY createdAt DESC")
    fun getSessionsFromDate(userId: String, startDate: Long): Flow<List<ExerciseSession>>
    
    @Insert
    suspend fun insertSession(session: ExerciseSession)
    
    @Update
    suspend fun updateSession(session: ExerciseSession)
    
    @Delete
    suspend fun deleteSession(session: ExerciseSession)
    
    @Query("DELETE FROM exercise_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: String)
}
