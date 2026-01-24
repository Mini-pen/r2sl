package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents an item in a shopping list with its quantity and checked status.
 */
@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["shoppingListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shoppingListId", "ingredientName"])]
)
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shoppingListId: String, // ID of the shopping list this item belongs to
    val ingredientName: String, // Name of the ingredient
    val quantity: Double, // Total quantity needed
    val unit: String, // Unit of measurement
    val category: String?, // Category for sorting (e.g., "Vegetables", "Meat", "Dairy")
    val isChecked: Boolean = false, // Whether the item has been checked off
    val recipeIds: String?, // Comma-separated list of recipe IDs this ingredient is from
    val dayOfWeek: Int?, // Day of week (0-6) for this ingredient
    val mealType: String?, // Meal type (breakfast, lunch, dinner, snack)
    val notes: String? // Optional notes
)
