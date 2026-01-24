package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.DishIngredient
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for DishIngredient entities.
 * Used for standalone dishes that have direct ingredients (not linked to a recipe).
 */
@Dao
interface DishIngredientDao {
    
    @Query("SELECT * FROM dish_ingredients WHERE dishId = :dishId ORDER BY name ASC")
    fun getIngredientsByDish(dishId: String): Flow<List<DishIngredient>>
    
    @Query("SELECT * FROM dish_ingredients WHERE dishId = :dishId")
    suspend fun getIngredientsByDishSync(dishId: String): List<DishIngredient>
    
    @Query("SELECT * FROM dish_ingredients WHERE id = :ingredientId")
    suspend fun getDishIngredientById(ingredientId: Long): DishIngredient?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDishIngredient(ingredient: DishIngredient): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDishIngredients(ingredients: List<DishIngredient>)
    
    @Update
    suspend fun updateDishIngredient(ingredient: DishIngredient)
    
    @Delete
    suspend fun deleteDishIngredient(ingredient: DishIngredient)
    
    @Query("DELETE FROM dish_ingredients WHERE dishId = :dishId")
    suspend fun deleteIngredientsByDish(dishId: String)
}
