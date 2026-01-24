package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String,
    val therapistId: String, // ID du thérapeute propriétaire
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?, // Format YYYY-MM-DD
    val phoneNumber: String?,
    val email: String?,
    val address: String?,
    val emergencyContact: String?,
    val medicalHistory: String?, // Informations médicales importantes
    val currentMedications: String?, // Médicaments actuels
    val allergies: String?, // Allergies connues
    val diagnosis: String?, // Diagnostic principal
    val treatmentPlan: String?, // Plan de traitement
    val notes: String?, // Notes générales
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
