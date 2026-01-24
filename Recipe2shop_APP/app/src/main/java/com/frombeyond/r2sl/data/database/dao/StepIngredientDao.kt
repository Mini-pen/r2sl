package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.StepIngredient
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for StepIngredient entities.
 * Manages ingredients used in specific recipe steps.
 */
@Dao
interface StepIngredientDao {
    
    @Query("SELECT * FROM step_ingredients WHERE stepId = :stepId ORDER BY id")
    fun getIngredientsByStep(stepId: Long): Flow<List<StepIngredient>>
    
    @Query("SELECT * FROM step_ingredients WHERE stepId = :stepId ORDER BY id")
    suspend fun getIngredientsByStepSync(stepId: Long): List<StepIngredient>
    
    @Query("SELECT * FROM step_ingredients WHERE ingredientId = :ingredientId")
    suspend fun getStepsByIngredient(ingredientId: Long): List<StepIngredient>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stepIngredient: StepIngredient): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stepIngredients: List<StepIngredient>)
    
    @Update
    suspend fun update(stepIngredient: StepIngredient)
    
    @Delete
    suspend fun delete(stepIngredient: StepIngredient)
    
    @Query("DELETE FROM step_ingredients WHERE stepId = :stepId")
    suspend fun deleteByStep(stepId: Long)
    
    @Query("DELETE FROM step_ingredients WHERE id = :id")
    suspend fun deleteById(id: Long)
}
