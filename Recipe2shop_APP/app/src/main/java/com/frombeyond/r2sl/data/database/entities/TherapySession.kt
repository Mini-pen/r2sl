package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "therapy_sessions")
data class TherapySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: String,
    val therapistId: String,
    val sessionDate: Long, // Timestamp de la séance
    val sessionDuration: Int, // Durée en minutes
    val sessionType: String, // "consultation", "suivi", "urgence", etc.
    val sessionStatus: String, // "planned", "completed", "cancelled", "no_show"
    val sessionNotes: String?, // Notes détaillées de la séance (chiffrées)
    val objectives: String?, // Objectifs de la séance
    val progress: String?, // Progrès observé
    val nextSteps: String?, // Prochaines étapes
    val moodRating: Int?, // Évaluation de l'humeur 1-10
    val anxietyLevel: Int?, // Niveau d'anxiété 1-10
    val depressionLevel: Int?, // Niveau de dépression 1-10
    val isUrgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
