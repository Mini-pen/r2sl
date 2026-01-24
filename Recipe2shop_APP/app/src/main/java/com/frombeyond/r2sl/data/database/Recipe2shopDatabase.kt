package com.frombeyond.r2sl.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.frombeyond.r2sl.data.database.dao.*
import com.frombeyond.r2sl.data.database.entities.*
import com.frombeyond.r2sl.data.database.converters.DateConverters
import net.sqlcipher.database.SupportFactory

/**
 * * Main database for R2SL application.
 * Stores recipes, dishes, ingredients, weekly menus, and shopping lists.
 */
@Database(
    entities = [
        User::class,
        Recipe::class,
        Ingredient::class,
        RecipeStep::class,
        SubStep::class,
        StepIngredient::class,
        Dish::class,
        DishIngredient::class,
        DishComposition::class,
        WeeklyMenu::class,
        ShoppingList::class,
        ShoppingListItem::class,
        UserPreferences::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class Recipe2shopDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeStepDao(): RecipeStepDao
    abstract fun subStepDao(): SubStepDao
    abstract fun stepIngredientDao(): StepIngredientDao
    abstract fun dishDao(): DishDao
    abstract fun dishIngredientDao(): DishIngredientDao
    abstract fun dishCompositionDao(): DishCompositionDao
    abstract fun weeklyMenuDao(): WeeklyMenuDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    
    companion object {
        @Volatile
        private var INSTANCE: Recipe2shopDatabase? = null
        
        fun getDatabase(context: Context, passphrase: String): Recipe2shopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Recipe2shopDatabase::class.java,
                    "recipe2shoplist_database"
                )
                .openHelperFactory(SupportFactory(passphrase.toByteArray()))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
