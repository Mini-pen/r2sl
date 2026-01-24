package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents a sub-step within a recipe step.
 * Each step can contain multiple sub-steps, each with a standard instruction and a FALC version.
 */
@Entity(
    tableName = "sub_steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeStep::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stepId", "subStepOrder"], unique = true)]
)
data class SubStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stepId: Long, // ID of the recipe step this sub-step belongs to
    val subStepOrder: Int, // Order of the sub-step within the step (1, 2, 3, ...)
    val instruction: String, // Standard instruction text
    val instructionFalc: String? // FALC version of the instruction (simplified, easy to read)
)
