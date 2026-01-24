package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * * Represents a cooking recipe with a list of measured ingredients and instructions.
 * A recipe is linked to ingredients via the Ingredient entity (recipeId).
 * When a recipe is created, a corresponding Dish is automatically created.
 * 
 * Recipe types: "entrée", "apéritif", "plat principal", "dessert", "encas"
 * Tags are free-form strings for categorization.
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    val id: String,
    val userId: String, // ID of the user who owns this recipe
    val name: String,
    val description: String?, // Short description of the recipe
    val servings: Int, // Number of servings
    val workTime: Int?, // Work/preparation time in minutes (travail préparatoire)
    val prepTime: Int?, // Additional preparation time in minutes
    val cookTime: Int?, // Cooking time in minutes
    val totalTime: Int?, // Total time = workTime + prepTime + cookTime (calculated)
    val instructions: String?, // Legacy: full cooking instructions (deprecated, use RecipeStep instead)
    val types: String?, // Comma-separated list of types: "entrée,plat principal" (from: entrée, apéritif, plat principal, dessert, encas)
    val tags: String?, // Comma-separated list of free-form tags
    val imageUrl: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
