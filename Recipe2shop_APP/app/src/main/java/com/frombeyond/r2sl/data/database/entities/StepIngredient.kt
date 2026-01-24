package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents an ingredient used in a specific recipe step.
 * Links a step to an ingredient from the recipe's ingredient list, with a specific quantity for that step.
 * The quantity should be less than or equal to the total quantity of the ingredient in the recipe.
 */
@Entity(
    tableName = "step_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeStep::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["stepId"]),
        Index(value = ["ingredientId"])
    ]
)
data class StepIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stepId: Long, // ID of the recipe step this ingredient is used in
    val ingredientId: Long, // ID of the ingredient from the recipe's ingredient list
    val quantity: Double, // Quantity used in this step (should be <= total ingredient quantity)
    val unit: String, // Unit of measurement (should match the ingredient's unit or be convertible)
    val notes: String? // Optional notes for this specific usage (e.g., "chopped", "diced")
)
