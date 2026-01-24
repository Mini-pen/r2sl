package com.frombeyond.r2sl.data.database.dao

import androidx.room.*
import com.frombeyond.r2sl.data.database.entities.ShoppingListItem
import kotlinx.coroutines.flow.Flow

/**
 * * Data Access Object for ShoppingListItem entities.
 */
@Dao
interface ShoppingListItemDao {
    
    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :shoppingListId ORDER BY category ASC, ingredientName ASC")
    fun getShoppingListItems(shoppingListId: String): Flow<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :shoppingListId AND category = :category ORDER BY ingredientName ASC")
    fun getShoppingListItemsByCategory(shoppingListId: String, category: String): Flow<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :shoppingListId AND isChecked = 0 ORDER BY category ASC, ingredientName ASC")
    fun getUncheckedItems(shoppingListId: String): Flow<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE id = :itemId")
    suspend fun getShoppingListItemById(itemId: Long): ShoppingListItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItems(items: List<ShoppingListItem>)
    
    @Update
    suspend fun updateShoppingListItem(item: ShoppingListItem)
    
    @Query("UPDATE shopping_list_items SET isChecked = :isChecked WHERE id = :itemId")
    suspend fun updateItemCheckedStatus(itemId: Long, isChecked: Boolean)
    
    @Delete
    suspend fun deleteShoppingListItem(item: ShoppingListItem)
    
    @Query("DELETE FROM shopping_list_items WHERE shoppingListId = :shoppingListId")
    suspend fun deleteShoppingListItemsByList(shoppingListId: String)
}
