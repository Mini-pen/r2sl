package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getUserPreferences(userId: String): UserPreferences?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferences)
    
    @Update
    suspend fun updateUserPreferences(preferences: UserPreferences)
    
    @Delete
    suspend fun deleteUserPreferences(preferences: UserPreferences)
    
    @Query("UPDATE user_preferences SET theme = :theme WHERE userId = :userId")
    suspend fun updateTheme(userId: String, theme: String)
    
    @Query("UPDATE user_preferences SET language = :language WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, language: String)
    
    @Query("UPDATE user_preferences SET notificationsEnabled = :enabled WHERE userId = :userId")
    suspend fun updateNotificationsEnabled(userId: String, enabled: Boolean)
    
    @Query("UPDATE user_preferences SET updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateTimestamp(userId: String, timestamp: Long = System.currentTimeMillis())
}
