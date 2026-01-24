package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents a step in a recipe.
 * Each step contains a title, duration, temperature, notes, ingredients, and a list of sub-steps.
 * Sub-steps contain the actual instructions (both standard and FALC versions).
 */
@Entity(
    tableName = "recipe_steps",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId", "stepOrder"])]
)
data class RecipeStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: String, // ID of the recipe this step belongs to
    val stepOrder: Int, // Order of the step in the recipe (1, 2, 3, ...)
    val title: String?, // Title of the step (e.g., "Préparation de la pâte")
    val duration: Int?, // Duration in minutes for this step (optional)
    val temperature: String?, // Temperature for this step (e.g., "180°C", "medium heat")
    val notes: String? // Optional notes for this step
)
