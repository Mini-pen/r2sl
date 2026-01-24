package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * * Represents a shopping list generated from weekly menus.
 */
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey
    val id: String,
    val userId: String, // ID of the user who owns this shopping list
    val name: String, // Name of the shopping list
    val weekStartDate: Long, // Timestamp of the Monday of the week this list is for
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
