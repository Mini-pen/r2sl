package com.frombeyond.r2sl.data.local

import android.content.Context
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import com.frombeyond.r2sl.utils.IngredientNormalizer
import java.time.LocalDate

/**
 * * Builds shopping list items from menu assignments for a date range.
 */
class ShoppingListGenerator(context: Context) {

    private val menuStorage = MenuStorageManager(context)
    private val dishStorage = DishStorageManager(context)
    private val recipesFileManager = RecipesLocalFileManager(context)

    fun buildItems(
        startDate: LocalDate,
        endDate: LocalDate,
        preserveManualFrom: ShoppingListStorageManager.ShoppingListEntry? = null
    ): List<ShoppingListStorageManager.ShoppingListItem> {
        val assignments = menuStorage.getAssignmentsBetween(startDate, endDate)
        val itemsMap = linkedMapOf<String, ShoppingListStorageManager.ShoppingListItem>()

        preserveManualFrom?.items?.forEach { item ->
            val key = itemKey(item)
            if (item.mealSources.isEmpty()) {
                itemsMap[key] = item
            } else {
                itemsMap[key] = item.copy(quantity = 0.0, mealSources = emptyList(), checked = item.checked, canceled = item.canceled)
            }
        }

        assignments.forEach { assignment ->
            val dish = dishStorage.getDishById(assignment.dishId) ?: return@forEach
            val recipeFileName = dish.recipeFileName ?: return@forEach
            val recipeFile = recipesFileManager.getRecipeFile(recipeFileName) ?: return@forEach
            val recipe = try {
                recipesFileManager.readRecipeFile(recipeFile).recipes.firstOrNull()
            } catch (_: Exception) {
                null
            } ?: return@forEach

            val recipeServings = recipe.servings.coerceAtLeast(1)
            val portionScale = assignment.portions.toDouble() / recipeServings

            recipe.ingredients.forEach { ingredient ->
                val firstAlt = ingredient.quantity.firstOrNull() ?: return@forEach
                val scaledQty = firstAlt.nb * portionScale
                val unit = IngredientNormalizer.normalizeUnit(firstAlt.unit)
                val category = ingredient.category.ifBlank { RayonsManager.DEFAULT_CATEGORY }
                val key = "${IngredientNormalizer.normalizeName(ingredient.name)}|$unit|$category"
                val current = itemsMap[key]

                if (current != null && current.mealSources.isEmpty()) {
                    return@forEach
                }

                val mealSource = ShoppingListStorageManager.MealSource(
                    date = assignment.date,
                    mealType = assignment.mealType,
                    recipeName = recipe.name,
                    quantityNeeded = scaledQty
                )

                val quantity = (current?.quantity ?: 0.0) + scaledQty
                val mealSources = (current?.mealSources ?: emptyList()) + mealSource

                itemsMap[key] = ShoppingListStorageManager.ShoppingListItem(
                    name = ingredient.name,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    checked = current?.checked ?: false,
                    canceled = current?.canceled ?: false,
                    mealSources = mealSources
                )
            }
        }

        return itemsMap.values.toList()
    }

    private fun itemKey(item: ShoppingListStorageManager.ShoppingListItem): String {
        return "${IngredientNormalizer.normalizeName(item.name)}|${IngredientNormalizer.normalizeUnit(item.unit)}|${item.category.ifBlank { RayonsManager.DEFAULT_CATEGORY }}"
    }
}
