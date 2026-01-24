package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val userId: String,
    val theme: String = "system", // "light", "dark", "system"
    val language: String = "fr",
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "09:00", // Format HH:mm
    val breathingExerciseDuration: Int = 300, // 5 minutes par défaut
    val meditationExerciseDuration: Int = 600, // 10 minutes par défaut
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
