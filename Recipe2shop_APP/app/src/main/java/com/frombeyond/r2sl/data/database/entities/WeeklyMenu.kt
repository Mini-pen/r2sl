package com.frombeyond.r2sl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * * Represents a weekly menu with meals assigned to specific days and meal types.
 * Weekly menus manipulate dishes (not recipes directly).
 * Each meal is associated with a dish via dishId.
 */
@Entity(
    tableName = "weekly_menus",
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["weekStartDate", "dayOfWeek", "mealType"]),
        Index(value = ["dishId"])
    ]
)
data class WeeklyMenu(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID of the user who owns this menu
    val weekStartDate: Long, // Timestamp of the Monday of the week
    val dayOfWeek: Int, // 0 = Monday, 1 = Tuesday, ..., 6 = Sunday
    val mealType: String, // "breakfast", "lunch", "dinner", "snack"
    val dishId: String, // ID of the dish for this meal
    val notes: String?, // Optional notes for this meal
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
