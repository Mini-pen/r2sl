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
import com.frombeyond.r2sl.data.local.MenuStorageManager
import com.frombeyond.r2sl.data.local.ShoppingListGenerator
import com.frombeyond.r2sl.data.local.ShoppingListStorageManager
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
    private lateinit var listGenerator: ShoppingListGenerator
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
        listGenerator = ShoppingListGenerator(requireContext())

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

            item.setOnLongClickListener {
                showRefreshListDialog(list)
                true
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
        ).apply { setTitle(R.string.shopping_lists_select_start) }.show()
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
        ).apply { setTitle(R.string.shopping_lists_select_end) }.show()
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

    private fun showRefreshListDialog(list: ShoppingListStorageManager.ShoppingListEntry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.shopping_list_refresh_title)
            .setMessage(R.string.shopping_list_refresh_message)
            .setPositiveButton(R.string.shopping_list_refresh_confirm) { _, _ ->
                refreshShoppingList(list)
            }
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .show()
    }

    private fun refreshShoppingList(list: ShoppingListStorageManager.ShoppingListEntry) {
        val startDate = LocalDate.parse(list.startDate)
        val endDate = LocalDate.parse(list.endDate)
        val items = listGenerator.buildItems(startDate, endDate, preserveManualFrom = list)
        val updated = list.copy(items = items)
        storageManager.updateList(updated)
        loadLists()
        Toast.makeText(requireContext(), R.string.shopping_list_refresh_done, Toast.LENGTH_SHORT).show()
    }

    private fun createShoppingList(startDate: LocalDate, endDate: LocalDate) {
        val existingList = storageManager.loadLists().firstOrNull { list ->
            val existingStart = LocalDate.parse(list.startDate)
            val existingEnd = LocalDate.parse(list.endDate)
            existingStart == startDate && existingEnd == endDate
        }

        val items = listGenerator.buildItems(startDate, endDate, preserveManualFrom = existingList)

        val list = if (existingList != null) {
            val updated = existingList.copy(items = items)
            storageManager.updateList(updated)
            updated
        } else {
            storageManager.createList(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                items = items
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
