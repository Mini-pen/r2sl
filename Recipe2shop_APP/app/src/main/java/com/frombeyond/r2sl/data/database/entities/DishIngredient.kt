package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents a direct ingredient in a standalone dish (not linked to a recipe).
 * Used for dishes that have "ready-to-use" ingredients instead of being linked to a recipe.
 * 
 * If a dish has recipeId set, it uses ingredients from that recipe (via Ingredient entity).
 * If a dish has no recipeId, it uses direct ingredients via this DishIngredient entity.
 */
@Entity(
    tableName = "dish_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dishId"])]
)
data class DishIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dishId: String, // ID of the standalone dish this ingredient belongs to
    val name: String, // Name of the ingredient
    val quantity: Double, // Quantity (e.g., 2.5)
    val unit: String, // Unit of measurement (e.g., "cups", "grams", "tbsp", "pieces")
    val notes: String? // Optional notes (e.g., "chopped", "diced")
)
