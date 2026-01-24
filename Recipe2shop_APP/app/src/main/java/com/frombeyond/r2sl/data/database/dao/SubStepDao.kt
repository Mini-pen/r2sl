package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.SubStep
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for SubStep entities.
 */
@Dao
interface SubStepDao {
    
    @Query("SELECT * FROM sub_steps WHERE stepId = :stepId ORDER BY subStepOrder ASC")
    fun getSubStepsByStep(stepId: Long): Flow<List<SubStep>>
    
    @Query("SELECT * FROM sub_steps WHERE stepId = :stepId ORDER BY subStepOrder ASC")
    suspend fun getSubStepsByStepSync(stepId: Long): List<SubStep>
    
    @Query("SELECT * FROM sub_steps WHERE id = :subStepId")
    suspend fun getSubStepById(subStepId: Long): SubStep?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubStep(subStep: SubStep): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubSteps(subSteps: List<SubStep>)
    
    @Update
    suspend fun updateSubStep(subStep: SubStep)
    
    @Delete
    suspend fun deleteSubStep(subStep: SubStep)
    
    @Query("DELETE FROM sub_steps WHERE stepId = :stepId")
    suspend fun deleteSubStepsByStep(stepId: Long)
}
