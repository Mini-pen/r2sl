package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.RecipeStep
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for RecipeStep entities.
 */
@Dao
interface RecipeStepDao {
    
    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY stepOrder ASC")
    fun getStepsByRecipe(recipeId: String): Flow<List<RecipeStep>>
    
    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY stepOrder ASC")
    suspend fun getStepsByRecipeSync(recipeId: String): List<RecipeStep>
    
    @Query("SELECT * FROM recipe_steps WHERE id = :stepId")
    suspend fun getStepById(stepId: Long): RecipeStep?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: RecipeStep): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RecipeStep>)
    
    @Update
    suspend fun updateStep(step: RecipeStep)
    
    @Delete
    suspend fun deleteStep(step: RecipeStep)
    
    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsByRecipe(recipeId: String)
}
