package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.Ingredient
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for Ingredient entities.
 */
@Dao
interface IngredientDao {
    
    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY name ASC")
    fun getIngredientsByRecipe(recipeId: String): Flow<List<Ingredient>>
    
    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId")
    suspend fun getIngredientsByRecipeSync(recipeId: String): List<Ingredient>
    
    @Query("SELECT * FROM ingredients WHERE id = :ingredientId")
    suspend fun getIngredientById(ingredientId: Long): Ingredient?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<Ingredient>)
    
    @Update
    suspend fun updateIngredient(ingredient: Ingredient)
    
    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)
    
    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipe(recipeId: String)
}
