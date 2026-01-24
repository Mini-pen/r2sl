package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.WeeklyMenu
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for WeeklyMenu entities.
 */
@Dao
interface WeeklyMenuDao {
    
    @Query("SELECT * FROM weekly_menus WHERE userId = :userId AND weekStartDate = :weekStartDate ORDER BY dayOfWeek ASC, mealType ASC")
    fun getWeeklyMenu(userId: String, weekStartDate: Long): Flow<List<WeeklyMenu>>
    
    @Query("SELECT * FROM weekly_menus WHERE userId = :userId AND weekStartDate = :weekStartDate AND dayOfWeek = :dayOfWeek ORDER BY mealType ASC")
    fun getMealsForDay(userId: String, weekStartDate: Long, dayOfWeek: Int): Flow<List<WeeklyMenu>>
    
    @Query("SELECT * FROM weekly_menus WHERE userId = :userId ORDER BY weekStartDate DESC, dayOfWeek ASC")
    fun getAllWeeklyMenus(userId: String): Flow<List<WeeklyMenu>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyMenu(weeklyMenu: WeeklyMenu): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyMenus(weeklyMenus: List<WeeklyMenu>)
    
    @Update
    suspend fun updateWeeklyMenu(weeklyMenu: WeeklyMenu)
    
    @Delete
    suspend fun deleteWeeklyMenu(weeklyMenu: WeeklyMenu)
    
    @Query("DELETE FROM weekly_menus WHERE userId = :userId AND weekStartDate = :weekStartDate")
    suspend fun deleteWeeklyMenuByWeek(userId: String, weekStartDate: Long)
}
