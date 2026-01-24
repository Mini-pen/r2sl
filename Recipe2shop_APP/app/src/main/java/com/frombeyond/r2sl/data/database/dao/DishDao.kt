package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.Dish
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for Dish entities.
 * 
 * Dishes are the building blocks used in weekly menus.
 * A dish can be:
 * - Linked to a recipe (recipeId is not null): uses ingredients from that recipe
 * - Standalone (recipeId is null): uses direct ingredients via DishIngredient entity
 * - Composite: contains other dishes via DishComposition entity
 * 
 * A dish can combine these types (e.g., a composite dish containing recipe-linked dishes).
 */
@Dao
interface DishDao {
    
    @Query("SELECT * FROM dishes WHERE userId = :userId ORDER BY name ASC")
    fun getAllDishesByUser(userId: String): Flow<List<Dish>>
    
    @Query("SELECT * FROM dishes WHERE id = :dishId")
    suspend fun getDishById(dishId: String): Dish?
    
    @Query("SELECT * FROM dishes WHERE userId = :userId AND recipeId = :recipeId")
    fun getDishesByRecipe(userId: String, recipeId: String): Flow<List<Dish>>
    
    @Query("SELECT * FROM dishes WHERE userId = :userId AND recipeId IS NOT NULL ORDER BY name ASC")
    fun getDishesLinkedToRecipes(userId: String): Flow<List<Dish>>
    
    @Query("SELECT * FROM dishes WHERE userId = :userId AND recipeId IS NULL ORDER BY name ASC")
    fun getStandaloneDishes(userId: String): Flow<List<Dish>>
    
    /**
     * * Get all composite dishes (dishes that contain other dishes).
     * Note: This requires checking DishComposition table, so it's better to use DishCompositionDao.
     */
    @Query("SELECT DISTINCT d.* FROM dishes d INNER JOIN dish_compositions dc ON d.id = dc.parentDishId WHERE d.userId = :userId ORDER BY d.name ASC")
    fun getCompositeDishes(userId: String): Flow<List<Dish>>
    
    @Query("SELECT * FROM dishes WHERE userId = :userId AND name LIKE :searchQuery ORDER BY name ASC")
    fun searchDishes(userId: String, searchQuery: String): Flow<List<Dish>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDish(dish: Dish)
    
    @Update
    suspend fun updateDish(dish: Dish)
    
    @Delete
    suspend fun deleteDish(dish: Dish)
    
    @Query("DELETE FROM dishes WHERE id = :dishId")
    suspend fun deleteDishById(dishId: String)
}
