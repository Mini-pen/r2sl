package com.frombeyond.r2sl.ui.weeklymenu

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.frombeyond.r2sl.ui.BaseFragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.local.DishStorageManager
import com.frombeyond.r2sl.data.local.MenuStorageManager
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import com.frombeyond.r2sl.ui.recipes.RecipeViewerFragment
import com.frombeyond.r2sl.utils.ErrorLogger
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * * Fragment for managing weekly menus.
 * Allows users to create weekly menus by associating dishes to meals.
 */
class WeeklyMenuFragment : BaseFragment() {

    private lateinit var buttonWeekView: MaterialButton
    private lateinit var buttonThreeDaysView: MaterialButton
    private lateinit var daysContainer: LinearLayout
    private lateinit var daysColumnsContainer: LinearLayout
    private lateinit var threeDaysNav: LinearLayout
    private lateinit var threeDaysRange: TextView
    private lateinit var buttonPrevDays: MaterialButton
    private lateinit var buttonNextDays: MaterialButton
    private lateinit var buttonCalendar: MaterialButton
    private lateinit var menuIcon: ImageView
    private lateinit var startDateDisplay: TextView
    private lateinit var menuStorage: MenuStorageManager
    private lateinit var dishStorage: DishStorageManager
    private lateinit var recipesFileManager: RecipesLocalFileManager
    private lateinit var menuStartDate: LocalDate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_weekly_menu, container, false)
        buttonWeekView = root.findViewById(R.id.button_view_week)
        buttonThreeDaysView = root.findViewById(R.id.button_view_three_days)
        daysContainer = root.findViewById(R.id.weekly_menu_days_container)
        daysColumnsContainer = root.findViewById(R.id.weekly_menu_days_columns_container)
        threeDaysNav = root.findViewById(R.id.weekly_menu_three_days_nav)
        threeDaysRange = root.findViewById(R.id.weekly_menu_three_days_range)
        buttonPrevDays = root.findViewById(R.id.button_three_days_prev)
        buttonNextDays = root.findViewById(R.id.button_three_days_next)
        buttonCalendar = root.findViewById(R.id.button_weekly_menu_calendar)
        menuIcon = root.findViewById(R.id.iv_weekly_menu_icon)
        startDateDisplay = root.findViewById(R.id.weekly_menu_start_date_display)

        menuStorage = MenuStorageManager(requireContext())
        dishStorage = DishStorageManager(requireContext())
        recipesFileManager = RecipesLocalFileManager(requireContext())

        buttonWeekView.setOnClickListener {
            setSelectedView(isWeek = true)
        }
        buttonThreeDaysView.setOnClickListener {
            setSelectedView(isWeek = false)
        }

        buttonPrevDays.setOnClickListener {
            menuStartDate = menuStartDate.minusDays(1)
            persistStartDate()
            updateStartDateDisplay()
            renderCurrentView()
        }
        buttonNextDays.setOnClickListener {
            menuStartDate = menuStartDate.plusDays(1)
            persistStartDate()
            updateStartDateDisplay()
            renderCurrentView()
        }
        buttonCalendar.setOnClickListener {
            openDatePicker()
        }

        loadMenuIcon()
        initStartDate()
        setSelectedView(isWeek = true)
        return root
    }

    private fun setSelectedView(isWeek: Boolean) {
        buttonWeekView.isChecked = isWeek
        buttonThreeDaysView.isChecked = !isWeek
        renderCurrentView()
    }

    private fun renderCurrentView() {
        val isWeek = buttonWeekView.isChecked
        daysContainer.visibility = if (isWeek) View.VISIBLE else View.GONE
        daysColumnsContainer.visibility = if (isWeek) View.GONE else View.VISIBLE
        threeDaysNav.visibility = if (isWeek) View.GONE else View.VISIBLE
        if (isWeek) {
            renderWeekView()
        } else {
            renderThreeDaysView()
        }
    }

    private fun renderWeekView() {
        daysContainer.removeAllViews()
        val days = (0 until 7).map { menuStartDate.plusDays(it.toLong()) }
        days.forEach { date ->
            val card = buildDayCard(date)
            daysContainer.addView(card)
        }
    }

    private fun renderThreeDaysView() {
        daysColumnsContainer.removeAllViews()
        val days = (0 until 3).map { menuStartDate.plusDays(it.toLong()) }
        val rangeText = "${formatShortDate(days.first())} - ${formatShortDate(days.last())}"
        threeDaysRange.text = rangeText
        days.forEach { date ->
            val card = buildDayCard(date)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.text_spacing_small)
            card.layoutParams = params
            daysColumnsContainer.addView(card)
        }
    }

    private fun buildDayCard(date: LocalDate): View {
        val card = layoutInflater.inflate(R.layout.item_weekly_day, daysContainer, false)
        val title = card.findViewById<TextView>(R.id.weekly_day_title)
        val lunchValue = card.findViewById<TextView>(R.id.weekly_day_lunch_value)
        val dinnerValue = card.findViewById<TextView>(R.id.weekly_day_dinner_value)
        val addButton = card.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.button_weekly_add)
        val lunchContainer = card.findViewById<LinearLayout>(R.id.weekly_day_lunch_container)
        val dinnerContainer = card.findViewById<LinearLayout>(R.id.weekly_day_dinner_container)

        title.text = formatDayTitle(date)
        lunchValue.text = resolveMealLabel(date, MenuStorageManager.MEAL_LUNCH)
        dinnerValue.text = resolveMealLabel(date, MenuStorageManager.MEAL_DINNER)

        addButton.setOnClickListener {
            showMealChoiceDialog(date)
        }

        lunchContainer.setOnClickListener {
            showMealActionDialog(date, MenuStorageManager.MEAL_LUNCH)
        }

        dinnerContainer.setOnClickListener {
            showMealActionDialog(date, MenuStorageManager.MEAL_DINNER)
        }

        return card
    }

    private fun resolveMealLabel(date: LocalDate, mealType: String): String {
        val assignments = menuStorage.getAssignments(date, mealType)
        if (assignments.isEmpty()) {
            return getString(R.string.weekly_menu_placeholder_meal)
        }
        val dishNames = assignments.mapNotNull { assignment ->
            dishStorage.getDishById(assignment.dishId)?.name
        }
        return if (dishNames.isEmpty()) {
            getString(R.string.weekly_menu_placeholder_meal)
        } else {
            dishNames.joinToString(", ")
        }
    }

    private fun showMealChoiceDialog(date: LocalDate) {
        val meals = arrayOf(
            getString(R.string.weekly_menu_lunch),
            getString(R.string.weekly_menu_dinner)
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.weekly_menu_add_dish))
            .setItems(meals) { _, which ->
                val mealType = if (which == 0) {
                    MenuStorageManager.MEAL_LUNCH
                } else {
                    MenuStorageManager.MEAL_DINNER
                }
                showRecipesDialog(date, mealType)
            }
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .show()
    }

    private fun showRecipesDialog(date: LocalDate, mealType: String) {
        try {
            val entries = recipesFileManager.listRecipeEntries()
            val entriesWithRecipe = entries.mapNotNull { entry ->
                try {
                    val file = recipesFileManager.getRecipeFile(entry.fileName) ?: return@mapNotNull null
                    val format = recipesFileManager.readRecipeFile(file)
                    val recipe = format.recipes.firstOrNull() ?: return@mapNotNull null
                    RecipeChoiceWithMeta(entry.name, entry.fileName, recipe.metadata?.favorite ?: false, recipe.metadata?.rating ?: 0)
                } catch (e: IOException) {
                    ErrorLogger.getInstance().logError("Erreur lecture recette: ${entry.fileName}", e, "WeeklyMenuFragment")
                    RecipeChoiceWithMeta(entry.name, entry.fileName, false, 0)
                } catch (e: Exception) {
                    null
                }
            }
            if (entriesWithRecipe.isEmpty()) {
                Toast.makeText(requireContext(), R.string.menu_no_recipes, Toast.LENGTH_SHORT).show()
                return
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_selector, null)
            val searchInput = dialogView.findViewById<TextInputEditText>(R.id.recipe_search_input)
            val recipeList = dialogView.findViewById<ListView>(R.id.recipe_list)
            val filterFavoritesBtn = dialogView.findViewById<MaterialButton>(R.id.recipe_selector_filter_favorites)
            val filterRatingBtn = dialogView.findViewById<MaterialButton>(R.id.recipe_selector_filter_rating)
            val filterRatingLabel = dialogView.findViewById<TextView>(R.id.recipe_selector_filter_rating_label)

            var filterText = ""
            var filterFavoritesOnly = false
            var filterMinRating = 0

            fun applyFilters(): List<RecipeChoiceWithMeta> {
                return entriesWithRecipe.filter { r ->
                    val nameOk = r.name.lowercase(Locale.getDefault()).contains(filterText)
                    val favoriteOk = !filterFavoritesOnly || r.favorite
                    val ratingOk = r.rating >= filterMinRating
                    nameOk && favoriteOk && ratingOk
                }
            }

            fun updateList() {
                val filtered = applyFilters()
                val adapter = ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    filtered.map { it.name }
                )
                recipeList.adapter = adapter
            }

            fun updateFilterUi() {
                filterFavoritesBtn.text = if (filterFavoritesOnly) "⭐" else "☆"
                filterRatingBtn.text = when (filterMinRating) {
                    0 -> "🤍"
                    1 -> "❤️"
                    2 -> "❤️❤️"
                    3 -> "❤️❤️❤️"
                    else -> "🤍"
                }
                filterRatingLabel.text = "${filterMinRating}+"
            }

            filterFavoritesBtn.setOnClickListener {
                filterFavoritesOnly = !filterFavoritesOnly
                updateFilterUi()
                updateList()
            }
            filterRatingBtn.setOnClickListener {
                filterMinRating = (filterMinRating + 1) % 4
                updateFilterUi()
                updateList()
            }

            updateFilterUi()
            updateList()

            searchInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    filterText = s?.toString()?.lowercase(Locale.getDefault()) ?: ""
                    updateList()
                }
            })

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.menu_choose_recipe))
                .setView(dialogView)
                .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
                .create()

            recipeList.setOnItemClickListener { _, _, position, _ ->
                try {
                    val filtered = applyFilters()
                    if (position >= 0 && position < filtered.size) {
                        val choice = filtered[position]
                        val dish = dishStorage.addDishIfMissing(choice.name, choice.fileName)
                        menuStorage.addDish(date, mealType, dish.id)
                        dialog.dismiss()
                        renderCurrentView()
                    }
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur lors de la sélection de la recette", e, "WeeklyMenuFragment")
                    Toast.makeText(requireContext(), "Erreur lors de l'ajout de la recette", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de l'ouverture du dialog de sélection de recette", e, "WeeklyMenuFragment")
            Toast.makeText(requireContext(), "Erreur lors de l'ouverture de la liste des recettes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavedDishesDialog(date: LocalDate, mealType: String) {
        val dishes = dishStorage.loadDishes()
        if (dishes.isEmpty()) {
            Toast.makeText(requireContext(), R.string.menu_no_saved_dishes, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = dishes.map { it.name }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.menu_choose_dish))
            .setItems(labels) { _, which ->
                val dish = dishes[which]
                menuStorage.addDish(date, mealType, dish.id)
                renderCurrentView()
            }
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .show()
    }

    private fun showMealActionDialog(date: LocalDate, mealType: String, initialIndex: Int = 0) {
        val assignments = menuStorage.getAssignments(date, mealType)
        if (assignments.isEmpty()) {
            // Pas de repas assigné, on peut juste ajouter
            showMealChoiceDialog(date)
            return
        }

        val mealTypeLabel = if (mealType == MenuStorageManager.MEAL_LUNCH) {
            getString(R.string.weekly_menu_lunch)
        } else {
            getString(R.string.weekly_menu_dinner)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_meal_actions, null)
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_meal_title)
        val prevButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_prev)
        val nextButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_next)
        val deleteButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_delete)
        val changeButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_change)
        val viewRecipeButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_view_recipe)
        val addAnotherButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_add_another)
        val portionsPlusButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_portions_plus)
        val portionsMinusButton = dialogView.findViewById<MaterialButton>(R.id.button_meal_portions_minus)
        val portionsText = dialogView.findViewById<TextView>(R.id.dialog_meal_portions)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        var currentAssignments = assignments.toMutableList()
        var currentIndex = initialIndex.coerceIn(0, currentAssignments.size - 1)

        fun updateDialogContent(index: Int) {
            if (index < 0 || index >= currentAssignments.size) return

            val assignment = currentAssignments[index]
            val dish = dishStorage.getDishById(assignment.dishId)
            if (dish == null) {
                Toast.makeText(requireContext(), R.string.meal_dish_not_found, Toast.LENGTH_SHORT).show()
                return
            }

            titleText.text = "$mealTypeLabel - ${dish.name}"
            portionsText.text = assignment.portions.toString()

            portionsPlusButton.setOnClickListener {
                val a = currentAssignments.getOrNull(currentIndex) ?: return@setOnClickListener
                val newPortions = a.portions + 1
                menuStorage.setPortions(date, mealType, a.dishId, newPortions)
                currentAssignments[currentIndex] = a.copy(portions = newPortions)
                portionsText.text = newPortions.toString()
            }
            portionsMinusButton.setOnClickListener {
                val a = currentAssignments.getOrNull(currentIndex) ?: return@setOnClickListener
                if (a.portions <= 1) return@setOnClickListener
                val newPortions = a.portions - 1
                menuStorage.setPortions(date, mealType, a.dishId, newPortions)
                currentAssignments[currentIndex] = a.copy(portions = newPortions)
                portionsText.text = newPortions.toString()
            }

            val hasMultiple = currentAssignments.size > 1
            prevButton.alpha = if (hasMultiple && index > 0) 1.0f else 0.2f
            nextButton.alpha = if (hasMultiple && index < currentAssignments.size - 1) 1.0f else 0.2f
            prevButton.isEnabled = hasMultiple && index > 0
            nextButton.isEnabled = hasMultiple && index < currentAssignments.size - 1

            deleteButton.setOnClickListener {
                menuStorage.removeAssignment(date, mealType, assignment.dishId)
                dialog.dismiss()
                renderCurrentView()
            }

            changeButton.setOnClickListener {
                dialog.dismiss()
                menuStorage.removeAssignment(date, mealType, assignment.dishId)
                showRecipesDialog(date, mealType)
            }

            viewRecipeButton.setOnClickListener {
                dialog.dismiss()
                val recipeFileName = dish.recipeFileName
                if (recipeFileName != null) {
                    val bundle = Bundle().apply {
                        putString(RecipeViewerFragment.ARG_RECIPE_FILE_NAME, recipeFileName)
                    }
                    findNavController().navigate(R.id.recipe_viewer, bundle)
                } else {
                    Toast.makeText(requireContext(), R.string.meal_dish_no_recipe, Toast.LENGTH_SHORT).show()
                }
            }

            addAnotherButton.setOnClickListener {
                dialog.dismiss()
                showRecipesDialog(date, mealType)
            }
        }

        prevButton.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateDialogContent(currentIndex)
            }
        }

        nextButton.setOnClickListener {
            if (currentIndex < currentAssignments.size - 1) {
                currentIndex++
                updateDialogContent(currentIndex)
            }
        }

        // Initialiser avec le premier plat
        updateDialogContent(currentIndex)

        dialog.show()
    }

    private fun initStartDate() {
        val savedDate = menuStorage.loadMenu().startDate
        menuStartDate = savedDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
    }

    private fun persistStartDate() {
        menuStorage.saveStartDate(menuStartDate)
        updateStartDateDisplay()
    }

    private fun updateStartDateDisplay() {
        startDateDisplay.text = formatShortDate(menuStartDate)
    }

    private fun openDatePicker() {
        val initial = menuStartDate
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                menuStartDate = LocalDate.of(year, month + 1, day)
                persistStartDate()
                updateStartDateDisplay()
                renderCurrentView()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    private fun loadMenuIcon() {
        try {
            requireContext().assets.open("images/menu_icon.png").use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                menuIcon.setImageBitmap(bitmap)
            }
        } catch (_: Exception) {
            menuIcon.visibility = View.GONE
        }
    }

    private fun formatDayTitle(date: LocalDate): String {
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        val formatted = dayName.replaceFirstChar { it.uppercase() }
        return "$formatted ${formatShortDate(date)}"
    }

    private fun formatShortDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd/MM"))
    }

    private data class RecipeChoiceWithMeta(val name: String, val fileName: String, val favorite: Boolean, val rating: Int)
}
