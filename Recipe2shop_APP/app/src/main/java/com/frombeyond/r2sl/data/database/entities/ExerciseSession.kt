package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_sessions")
data class ExerciseSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val exerciseType: String, // "breathing", "meditation", "relaxation"
    val duration: Int, // en secondes
    val completed: Boolean,
    val rating: Int? = null, // 1-5 étoiles
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
