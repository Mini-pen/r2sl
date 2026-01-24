package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "therapists")
data class Therapist(
    @PrimaryKey
    val id: String,
    val email: String,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val specialization: String?, // Spécialisation thérapeutique
    val licenseNumber: String?, // Numéro de licence professionnelle
    val practiceAddress: String?,
    val yearsOfExperience: Int?,
    val photoUrl: String?,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
