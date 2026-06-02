package com.frombeyond.r2sl.ui.weeklymenu

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
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
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.frombeyond.r2sl.data.export.WeeklyMenuPdfGenerator
import com.frombeyond.r2sl.ui.recipes.EditRecipeFragment
import com.frombeyond.r2sl.ui.recipes.RecipeCreationLauncher
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import com.frombeyond.r2sl.ui.recipes.RecipeViewerFragment
import com.frombeyond.r2sl.utils.AccessibilityHelper
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
    private lateinit var exportMenuPdfButton: MaterialButton

    private val exportMenuPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) {
                exportMenuPdfToUri(uri)
            }
        }

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
        exportMenuPdfButton = root.findViewById(R.id.button_export_weekly_menu_pdf)

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

        exportMenuPdfButton.setOnClickListener {
            val fileName = "menu_${menuStartDate}.pdf"
            exportMenuPdfLauncher.launch(fileName)
        }

        parentFragmentManager.setFragmentResultListener(
            EditRecipeFragment.RESULT_KEY_ADD_RECIPE_TO_MEAL,
            viewLifecycleOwner
        ) { _, bundle ->
            val recipeName = bundle.getString(EditRecipeFragment.RESULT_RECIPE_NAME)
            val recipeFileName = bundle.getString(EditRecipeFragment.RESULT_RECIPE_FILE_NAME)
            val dateStr = bundle.getString(EditRecipeFragment.RESULT_ADD_TO_MEAL_DATE)
            val mealType = bundle.getString(EditRecipeFragment.RESULT_ADD_TO_MEAL_TYPE)
            if (!recipeName.isNullOrBlank() && !recipeFileName.isNullOrBlank() && !dateStr.isNullOrBlank() && !mealType.isNullOrBlank()) {
                try {
                    val date = LocalDate.parse(dateStr)
                    val dish = dishStorage.addDishIfMissing(recipeName, recipeFileName)
                    menuStorage.addDish(date, mealType, dish.id)
                    renderCurrentView()
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur ajout recette au menu après création", e, "WeeklyMenuFragment")
                }
            }
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

        title.setOnClickListener {
            showDayDetailDialog(date)
        }
        lunchContainer.setOnClickListener {
            showDayDetailDialog(date)
        }
        dinnerContainer.setOnClickListener {
            showDayDetailDialog(date)
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

            val newRecipeBtn = dialogView.findViewById<MaterialButton>(R.id.recipe_selector_new_recipe)
            newRecipeBtn.setOnClickListener {
                dialog.dismiss()
                RecipeCreationLauncher.show(
                    fragment = this,
                    navController = findNavController(),
                    addToMealDate = date.toString(),
                    addToMealType = mealType
                )
            }

            AccessibilityHelper.applyAccessibilitySettings(requireContext(), dialogView)
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
            // * Re-apply accessibility after ListView has created its item views
            dialogView.post {
                AccessibilityHelper.applyAccessibilitySettings(requireContext(), dialogView)
            }
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

    private fun showDayDetailDialog(date: LocalDate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_day_meals, null)
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_day_title)
        val lunchRows = dialogView.findViewById<LinearLayout>(R.id.dialog_day_lunch_rows)
        val dinnerRows = dialogView.findViewById<LinearLayout>(R.id.dialog_day_dinner_rows)
        val addLunchBtn = dialogView.findViewById<MaterialButton>(R.id.dialog_day_add_lunch)
        val addDinnerBtn = dialogView.findViewById<MaterialButton>(R.id.dialog_day_add_dinner)
        val copyMenuBtn = dialogView.findViewById<MaterialButton>(R.id.dialog_day_copy_menu)

        titleText.text = formatDayTitle(date)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        fun refreshDialog() {
            populateMealRows(lunchRows, date, MenuStorageManager.MEAL_LUNCH, dialog)
            populateMealRows(dinnerRows, date, MenuStorageManager.MEAL_DINNER, dialog)
            renderCurrentView()
        }

        addLunchBtn.setOnClickListener {
            dialog.dismiss()
            showRecipesDialog(date, MenuStorageManager.MEAL_LUNCH)
        }
        addDinnerBtn.setOnClickListener {
            dialog.dismiss()
            showRecipesDialog(date, MenuStorageManager.MEAL_DINNER)
        }
        copyMenuBtn.setOnClickListener {
            showCopyMenuDialog(date, dialog)
        }

        refreshDialog()

        dialog.show()
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
    }

    private fun populateMealRows(
        container: LinearLayout,
        date: LocalDate,
        mealType: String,
        parentDialog: androidx.appcompat.app.AlertDialog
    ) {
        container.removeAllViews()
        val assignments = menuStorage.getAssignments(date, mealType)
        if (assignments.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = getString(R.string.weekly_menu_placeholder_meal)
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }
            container.addView(empty)
            return
        }
        assignments.forEach { assignment ->
            val dish = dishStorage.getDishById(assignment.dishId) ?: return@forEach
            val row = layoutInflater.inflate(R.layout.item_day_meal_dish_row, container, false)
            row.findViewById<TextView>(R.id.day_meal_dish_name).text = dish.name
            val portionsLabel = row.findViewById<TextView>(R.id.day_meal_portions_label)
            portionsLabel.text = assignment.portions.toString()

            row.findViewById<MaterialButton>(R.id.day_meal_portions_plus).setOnClickListener {
                val newPortions = assignment.portions + 1
                menuStorage.setPortions(date, mealType, assignment.dishId, newPortions)
                parentDialog.dismiss()
                showDayDetailDialog(date)
            }
            row.findViewById<MaterialButton>(R.id.day_meal_portions_minus).setOnClickListener {
                if (assignment.portions <= 1) return@setOnClickListener
                val newPortions = assignment.portions - 1
                menuStorage.setPortions(date, mealType, assignment.dishId, newPortions)
                parentDialog.dismiss()
                showDayDetailDialog(date)
            }
            row.findViewById<MaterialButton>(R.id.day_meal_delete).setOnClickListener {
                menuStorage.removeAssignment(date, mealType, assignment.dishId)
                parentDialog.dismiss()
                showDayDetailDialog(date)
            }
            row.findViewById<MaterialButton>(R.id.day_meal_change).setOnClickListener {
                menuStorage.removeAssignment(date, mealType, assignment.dishId)
                parentDialog.dismiss()
                showRecipesDialog(date, mealType)
            }
            row.findViewById<MaterialButton>(R.id.day_meal_view_recipe).setOnClickListener {
                parentDialog.dismiss()
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
            row.findViewById<MaterialButton>(R.id.day_meal_add_another).setOnClickListener {
                parentDialog.dismiss()
                showRecipesDialog(date, mealType)
            }
            container.addView(row)
        }
    }

    private fun showCopyMenuDialog(sourceDate: LocalDate, parentDialog: androidx.appcompat.app.AlertDialog) {
        val input = TextInputEditText(requireContext()).apply {
            setText("7")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            val margin = (16 * resources.displayMetrics.density).toInt()
            setPadding(margin, margin, margin, margin)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.menu_copy_day_title)
            .setMessage(getString(R.string.menu_copy_day_message, formatDayTitle(sourceDate)))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val dayCount = input.text?.toString()?.trim()?.toIntOrNull() ?: 7
                when (val result = menuStorage.copyDayToFollowingDays(sourceDate, dayCount)) {
                    is MenuStorageManager.MenuCopyResult.Success -> {
                        parentDialog.dismiss()
                        renderCurrentView()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.menu_copy_day_success, result.daysCopied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is MenuStorageManager.MenuCopyResult.Overlap -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.menu_copy_day_overlap, formatDayTitle(result.date)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    MenuStorageManager.MenuCopyResult.SourceDayEmpty -> {
                        Toast.makeText(requireContext(), R.string.menu_copy_day_empty, Toast.LENGTH_SHORT).show()
                    }
                    MenuStorageManager.MenuCopyResult.InvalidDayCount -> {
                        Toast.makeText(requireContext(), R.string.menu_copy_day_invalid, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportMenuPdfToUri(uri: Uri) {
        try {
            val days = (0 until 7).map { menuStartDate.plusDays(it.toLong()) }
            requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                WeeklyMenuPdfGenerator(requireContext()).generatePdf(
                    days = days,
                    menuStorage = menuStorage,
                    dishStorage = dishStorage,
                    outputStream = output
                )
            }
            Toast.makeText(requireContext(), R.string.weekly_menu_export_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur export PDF menu", e, "WeeklyMenuFragment")
            Toast.makeText(requireContext(), R.string.weekly_menu_export_error, Toast.LENGTH_SHORT).show()
        }
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
