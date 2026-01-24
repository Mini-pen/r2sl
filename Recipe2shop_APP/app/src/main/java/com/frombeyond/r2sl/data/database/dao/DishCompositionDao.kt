package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.DishComposition
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for DishComposition entities.
 * Manages the composition relationships between dishes (dishes containing other dishes).
 */
@Dao
interface DishCompositionDao {
    
    /**
     * * Get all dishes contained in a parent dish.
     */
    @Query("SELECT * FROM dish_compositions WHERE parentDishId = :parentDishId ORDER BY `order` ASC")
    fun getContainedDishes(parentDishId: String): Flow<List<DishComposition>>
    
    /**
     * * Get all dishes contained in a parent dish (synchronous version).
     */
    @Query("SELECT * FROM dish_compositions WHERE parentDishId = :parentDishId ORDER BY `order` ASC")
    suspend fun getContainedDishesSync(parentDishId: String): List<DishComposition>
    
    /**
     * * Get all parent dishes that contain a specific dish.
     */
    @Query("SELECT * FROM dish_compositions WHERE containedDishId = :containedDishId")
    fun getParentDishes(containedDishId: String): Flow<List<DishComposition>>
    
    /**
     * * Get all parent dishes that contain a specific dish (synchronous version).
     */
    @Query("SELECT * FROM dish_compositions WHERE containedDishId = :containedDishId")
    suspend fun getParentDishesSync(containedDishId: String): List<DishComposition>
    
    /**
     * * Check if a dish is contained in another dish.
     */
    @Query("SELECT COUNT(*) > 0 FROM dish_compositions WHERE parentDishId = :parentDishId AND containedDishId = :containedDishId")
    suspend fun isDishContained(parentDishId: String, containedDishId: String): Boolean
    
    /**
     * * Get a specific composition relationship.
     */
    @Query("SELECT * FROM dish_compositions WHERE id = :id")
    suspend fun getCompositionById(id: Long): DishComposition?
    
    /**
     * * Insert a composition relationship.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComposition(composition: DishComposition): Long
    
    /**
     * * Insert multiple composition relationships.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompositions(compositions: List<DishComposition>)
    
    /**
     * * Update a composition relationship.
     */
    @Update
    suspend fun updateComposition(composition: DishComposition)
    
    /**
     * * Delete a composition relationship.
     */
    @Delete
    suspend fun deleteComposition(composition: DishComposition)
    
    /**
     * * Delete all compositions for a parent dish.
     */
    @Query("DELETE FROM dish_compositions WHERE parentDishId = :parentDishId")
    suspend fun deleteCompositionsByParent(parentDishId: String)
    
    /**
     * * Delete all compositions where a dish is contained.
     */
    @Query("DELETE FROM dish_compositions WHERE containedDishId = :containedDishId")
    suspend fun deleteCompositionsByContained(containedDishId: String)
}
