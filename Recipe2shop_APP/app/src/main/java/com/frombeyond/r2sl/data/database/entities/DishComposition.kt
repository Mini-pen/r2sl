package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents a composition relationship between dishes.
 * A dish can contain other dishes as components.
 * 
 * This allows creating composite dishes (e.g., "Menu du jour" containing "Entrée", "Plat principal", "Dessert").
 * 
 * Example:
 * - Dish "Menu du jour" contains:
 *   - Dish "Salade César" (containedDishId)
 *   - Dish "Poulet rôti" (containedDishId)
 *   - Dish "Tarte aux pommes" (containedDishId)
 */
@Entity(
    tableName = "dish_compositions",
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["parentDishId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["containedDishId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentDishId"]),
        Index(value = ["containedDishId"]),
        Index(value = ["parentDishId", "containedDishId"], unique = true)
    ]
)
data class DishComposition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentDishId: String, // ID of the dish that contains other dishes
    val containedDishId: String, // ID of the dish that is contained in the parent dish
    val quantity: Int = 1, // Number of times this dish is included (e.g., 2 servings of "Salade")
    val order: Int = 0, // Order of this dish in the composition (for display purposes)
    val notes: String? // Optional notes about this composition
)
