package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.ShoppingList
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for ShoppingList entities.
 */
@Dao
interface ShoppingListDao {
    
    @Query("SELECT * FROM shopping_lists WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllShoppingListsByUser(userId: String): Flow<List<ShoppingList>>
    
    @Query("SELECT * FROM shopping_lists WHERE id = :shoppingListId")
    suspend fun getShoppingListById(shoppingListId: String): ShoppingList?
    
    @Query("SELECT * FROM shopping_lists WHERE userId = :userId AND weekStartDate = :weekStartDate")
    suspend fun getShoppingListByWeek(userId: String, weekStartDate: Long): ShoppingList?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList)
    
    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList)
    
    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList)
    
    @Query("DELETE FROM shopping_lists WHERE id = :shoppingListId")
    suspend fun deleteShoppingListById(shoppingListId: String)
}
