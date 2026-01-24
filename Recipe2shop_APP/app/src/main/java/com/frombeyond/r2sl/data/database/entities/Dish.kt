package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * * Represents a dish that can be associated with a meal in a weekly menu.
 * A dish is the building block used in weekly menus.
 * 
 * A dish can be:
 * - Linked to a recipe (via recipeId): created automatically when a recipe is created
 *   → Uses ingredients from that recipe (via Ingredient entity)
 * - Standalone with "ready-to-use" ingredients: has direct ingredients via DishIngredient entity
 * - Composite dish: contains other dishes via DishComposition entity
 * 
 * A dish can combine these types:
 * - A composite dish can contain dishes linked to recipes
 * - A composite dish can contain standalone dishes
 * - A composite dish can even contain other composite dishes (nested composition)
 * 
 * In weekly menus, dishes are manipulated (not recipes directly).
 */
@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey
    val id: String,
    val userId: String, // ID of the user who owns this dish
    val name: String,
    val recipeId: String?, // Optional: link to a recipe (if null, dish has direct ingredients via DishIngredient)
    val description: String?,
    val category: String?, // e.g., "Breakfast", "Lunch", "Dinner", "Snack"
    val servings: Int, // Number of servings for this dish
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
