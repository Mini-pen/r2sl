package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for Recipe entities.
 * 
 * Recipes contain a list of measured ingredients (via Ingredient entity).
 * When a recipe is created, a corresponding Dish should be automatically created.
 */
@Dao
interface RecipeDao {
    
    @Query("SELECT * FROM recipes WHERE userId = :userId ORDER BY name ASC")
    fun getAllRecipesByUser(userId: String): Flow<List<Recipe>>
    
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: String): Recipe?
    
    @Query("SELECT * FROM recipes WHERE userId = :userId AND isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteRecipes(userId: String): Flow<List<Recipe>>
    
    @Query("SELECT * FROM recipes WHERE userId = :userId AND name LIKE :searchQuery ORDER BY name ASC")
    fun searchRecipes(userId: String, searchQuery: String): Flow<List<Recipe>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)
    
    @Update
    suspend fun updateRecipe(recipe: Recipe)
    
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
    
    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)
}
