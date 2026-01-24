package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents an ingredient in a recipe with its quantity and unit.
 * Ingredients belong to recipes (not to dishes directly).
 * For standalone dishes, use DishIngredient entity instead.
 */
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: String, // ID of the recipe this ingredient belongs to
    val name: String, // Name of the ingredient
    val quantity: Double, // Quantity (e.g., 2.5)
    val unit: String, // Unit of measurement (e.g., "cups", "grams", "tbsp", "pieces")
    val category: String, // Category / rayon (e.g., "fruits et legumes frais")
    val notes: String? // Optional notes (e.g., "chopped", "diced")
)
