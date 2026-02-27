package com.frombeyond.r2sl.ui.shoppinglists

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.frombeyond.r2sl.ui.BaseFragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.local.DishStorageManager
import com.frombeyond.r2sl.data.local.MenuStorageManager
import com.frombeyond.r2sl.data.local.ShoppingListStorageManager
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import com.frombeyond.r2sl.utils.IngredientNormalizer
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * * Fragment for managing shopping lists.
 * Displays interactive shopping lists generated from weekly menus.
 */
class ShoppingListsFragment : BaseFragment() {

    private lateinit var listsContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var newListButton: MaterialButton
    private lateinit var storageManager: ShoppingListStorageManager
    private lateinit var menuStorage: MenuStorageManager
    private lateinit var dishStorage: DishStorageManager
    private lateinit var recipesFileManager: RecipesLocalFileManager
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_shopping_lists, container, false)
        listsContainer = root.findViewById(R.id.shopping_lists_container)
        emptyText = root.findViewById(R.id.shopping_lists_empty)
        newListButton = root.findViewById(R.id.button_new_shopping_list)

        storageManager = ShoppingListStorageManager(requireContext())
        menuStorage = MenuStorageManager(requireContext())
        dishStorage = DishStorageManager(requireContext())
        recipesFileManager = RecipesLocalFileManager(requireContext())

        newListButton.setOnClickListener { openDateRangePicker() }

        return root
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    private fun loadLists() {
        listsContainer.removeAllViews()
        val lists = storageManager.loadLists()
            .sortedByDescending { LocalDate.parse(it.startDate) }
        emptyText.isVisible = lists.isEmpty()
        lists.forEach { list ->
            val item = layoutInflater.inflate(R.layout.item_shopping_list_summary, listsContainer, false)
            val rangeText = item.findViewById<TextView>(R.id.shopping_list_range)
            val countText = item.findViewById<TextView>(R.id.shopping_list_count)
            val deleteButton = item.findViewById<MaterialButton>(R.id.button_delete_list)

            val start = LocalDate.parse(list.startDate)
            val end = LocalDate.parse(list.endDate)
            
            // Construire le texte avec les noms des jours
            val dayNames = buildList {
                var current = start
                while (!current.isAfter(end)) {
                    val dayName = current.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.FRENCH)
                    add(dayName.replaceFirstChar { it.uppercase() })
                    current = current.plusDays(1)
                }
            }
            val daysText = dayNames.joinToString(", ")
            rangeText.text = "${start.format(dateFormatter)} - ${end.format(dateFormatter)} ($daysText)"
            countText.text = "${list.items.size} articles"

            item.setOnClickListener {
                val bundle = Bundle().apply {
                    putString(ShoppingListDetailFragment.ARG_LIST_ID, list.id)
                }
                findNavController().navigate(R.id.shopping_list_detail, bundle)
            }

            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(list.id)
            }

            listsContainer.addView(item)
        }
    }

    private fun showDeleteConfirmationDialog(listId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.shopping_list_delete_title)
            .setMessage(R.string.shopping_list_delete_message)
            .setPositiveButton(R.string.shopping_list_delete_confirm) { _, _ ->
                storageManager.deleteList(listId)
                loadLists()
                Toast.makeText(requireContext(), R.string.shopping_list_delete_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .show()
    }

    private fun openDateRangePicker() {
        val today = LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val startDate = LocalDate.of(year, month + 1, day)
                openEndDatePicker(startDate)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        ).show()
    }

    private fun openEndDatePicker(startDate: LocalDate) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val endDate = LocalDate.of(year, month + 1, day)
                if (endDate.isBefore(startDate)) {
                    Toast.makeText(requireContext(), R.string.shopping_lists_select_end, Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                validateMenuAndCreateList(startDate, endDate)
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        ).show()
    }

    private fun validateMenuAndCreateList(startDate: LocalDate, endDate: LocalDate) {
        val missing = findMissingMeals(startDate, endDate)
        if (missing.isNotEmpty()) {
            val message = getString(
                R.string.shopping_lists_missing_meals_message,
                missing.joinToString("\n")
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.shopping_lists_missing_meals_title)
                .setMessage(message)
                .setPositiveButton(R.string.shopping_lists_confirm_create) { _, _ ->
                    createShoppingList(startDate, endDate)
                }
                .setNegativeButton(R.string.shopping_lists_cancel, null)
                .show()
        } else {
            createShoppingList(startDate, endDate)
        }
    }

    private fun findMissingMeals(startDate: LocalDate, endDate: LocalDate): List<String> {
        val missing = mutableListOf<String>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            if (menuStorage.getAssignment(current, MenuStorageManager.MEAL_LUNCH) == null) {
                missing.add("${formatDayTitle(current)} ${getString(R.string.weekly_menu_lunch)}")
            }
            if (menuStorage.getAssignment(current, MenuStorageManager.MEAL_DINNER) == null) {
                missing.add("${formatDayTitle(current)} ${getString(R.string.weekly_menu_dinner)}")
            }
            current = current.plusDays(1)
        }
        return missing
    }

    private fun createShoppingList(startDate: LocalDate, endDate: LocalDate) {
        val assignments = menuStorage.getAssignmentsBetween(startDate, endDate)
        val itemsMap = linkedMapOf<String, ShoppingListStorageManager.ShoppingListItem>()
        
        // Vérifier s'il existe déjà une liste avec les mêmes dates
        val existingList = storageManager.loadLists().firstOrNull { list ->
            val existingStart = LocalDate.parse(list.startDate)
            val existingEnd = LocalDate.parse(list.endDate)
            existingStart == startDate && existingEnd == endDate
        }
        
        // Si une liste existe, récupérer les items existants (notamment les items manuels)
        if (existingList != null) {
            existingList.items.forEach { item ->
                val key = "${IngredientNormalizer.normalizeName(item.name)}|${IngredientNormalizer.normalizeUnit(item.unit)}|${item.category.ifBlank { "Autres" }}"
                // Les items manuels (sans mealSources) doivent être préservés complètement
                if (item.mealSources.isEmpty()) {
                    itemsMap[key] = item
                } else {
                    // Pour les items avec mealSources, on garde l'état mais on va refusionner les quantités
                    // On garde l'état checked/canceled mais on va recalculer la quantité à partir des recettes
                    itemsMap[key] = item.copy(quantity = 0.0, mealSources = emptyList(), checked = item.checked, canceled = item.canceled)
                }
            }
        }
        
        assignments.forEach { assignment ->
            val dish = dishStorage.getDishById(assignment.dishId) ?: return@forEach
            val recipeFileName = dish.recipeFileName ?: return@forEach
            val recipeFile = recipesFileManager.getRecipeFile(recipeFileName) ?: return@forEach
            val recipe = try {
                val format = recipesFileManager.readRecipeFile(recipeFile)
                format.recipes.firstOrNull()
            } catch (_: Exception) {
                null
            } ?: return@forEach

            val recipeServings = recipe.servings.coerceAtLeast(1)
            val portionScale = assignment.portions.toDouble() / recipeServings

            recipe.ingredients.forEach { ingredient ->
                val firstAlt = ingredient.quantity.firstOrNull() ?: return@forEach
                val scaledQty = firstAlt.nb * portionScale
                val unit = IngredientNormalizer.normalizeUnit(firstAlt.unit)
                val category = ingredient.category.ifBlank { "Autres" }
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
                val checked = current?.checked ?: false
                val canceled = current?.canceled ?: false

                itemsMap[key] = ShoppingListStorageManager.ShoppingListItem(
                    name = ingredient.name,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    checked = checked,
                    canceled = canceled,
                    mealSources = mealSources
                )
            }
        }

        val list = if (existingList != null) {
            // Mettre à jour la liste existante
            val updated = existingList.copy(items = itemsMap.values.toList())
            storageManager.updateList(updated)
            updated
        } else {
            // Créer une nouvelle liste
            storageManager.createList(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                items = itemsMap.values.toList()
            )
        }
        
        loadLists()
        val bundle = Bundle().apply {
            putString(ShoppingListDetailFragment.ARG_LIST_ID, list.id)
        }
        findNavController().navigate(R.id.shopping_list_detail, bundle)
    }

    private fun formatDayTitle(date: LocalDate): String {
        val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH)
        val formatted = dayName.replaceFirstChar { it.uppercase() }
        return "$formatted ${date.format(dateFormatter)}"
    }
}
