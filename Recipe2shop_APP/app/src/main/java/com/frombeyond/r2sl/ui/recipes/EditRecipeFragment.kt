package com.frombeyond.r2sl.ui.recipes

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.frombeyond.r2sl.ui.BaseFragment
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.export.QuantityAlternative
import com.frombeyond.r2sl.data.export.RecipeJson
import com.frombeyond.r2sl.data.export.RecipeJsonFormat
import com.frombeyond.r2sl.data.export.RecipeMetadataJson
import com.frombeyond.r2sl.data.export.RecipeStepJson
import com.frombeyond.r2sl.data.export.StepIngredientJson
import com.frombeyond.r2sl.data.export.SubStepJson
import com.frombeyond.r2sl.data.local.IngredientEmojiManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/**
 * * Fragment for creating or editing a recipe JSON file.
 */
class EditRecipeFragment : BaseFragment() {

    private lateinit var fileManager: RecipesLocalFileManager
    private var editingFileName: String? = null
    private var editingRecipeId: String? = null

    private lateinit var nameLayout: TextInputLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var typesLayout: TextInputLayout
    private lateinit var typesInput: TextInputEditText
    private lateinit var tagsInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var servingsInput: TextInputEditText
    private lateinit var workTimeInput: TextInputEditText
    private lateinit var prepTimeInput: TextInputEditText
    private lateinit var cookTimeInput: TextInputEditText
    private lateinit var totalTimeInput: TextInputEditText

    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var addIngredientButton: MaterialButton
    private lateinit var stepsContainer: LinearLayout
    private lateinit var addStepButton: MaterialButton

    private lateinit var saveTopButton: MaterialButton
    private lateinit var saveBottomButton: MaterialButton
    private lateinit var exportTopButton: MaterialButton
    private lateinit var exportBottomButton: MaterialButton

    private val exportRecipeLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            exportRecipeToUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_edit_recipe, container, false)
        fileManager = RecipesLocalFileManager(requireContext())

        nameLayout = root.findViewById(R.id.input_layout_recipe_name)
        nameInput = root.findViewById(R.id.input_recipe_name)
        typesLayout = root.findViewById(R.id.input_layout_recipe_types)
        typesInput = root.findViewById(R.id.input_recipe_types)
        tagsInput = root.findViewById(R.id.input_recipe_tags)
        descriptionInput = root.findViewById(R.id.input_recipe_description)
        servingsInput = root.findViewById(R.id.input_recipe_servings)
        workTimeInput = root.findViewById(R.id.input_recipe_work_time)
        prepTimeInput = root.findViewById(R.id.input_recipe_prep_time)
        cookTimeInput = root.findViewById(R.id.input_recipe_cook_time)
        totalTimeInput = root.findViewById(R.id.input_recipe_total_time)

        ingredientsContainer = root.findViewById(R.id.ingredients_container)
        addIngredientButton = root.findViewById(R.id.button_add_ingredient)
        stepsContainer = root.findViewById(R.id.steps_container)
        addStepButton = root.findViewById(R.id.button_add_step)

        saveTopButton = root.findViewById(R.id.button_save_recipe_top)
        saveBottomButton = root.findViewById(R.id.button_save_recipe_bottom)
        exportTopButton = root.findViewById(R.id.button_export_recipe_top)
        exportBottomButton = root.findViewById(R.id.button_export_recipe_bottom)

        addIngredientButton.setOnClickListener { addIngredientRow(null) }
        addStepButton.setOnClickListener { addStepRow(null) }

        saveTopButton.setOnClickListener { saveRecipe() }
        saveBottomButton.setOnClickListener { saveRecipe() }
        exportTopButton.setOnClickListener { exportRecipe() }
        exportBottomButton.setOnClickListener { exportRecipe() }

        editingFileName = arguments?.getString(ARG_RECIPE_FILE_NAME)
        if (editingFileName != null) {
            loadExistingRecipe(editingFileName!!)
        }

        return root
    }

    private fun loadExistingRecipe(fileName: String) {
        val file = fileManager.getRecipeFile(fileName)
        if (file == null) {
            Toast.makeText(requireContext(), R.string.recipes_edit_file_missing, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val format = fileManager.readRecipeFile(file)
            val recipe = format.recipes.firstOrNull()
            if (recipe == null) {
                Toast.makeText(requireContext(), R.string.recipes_edit_file_invalid, Toast.LENGTH_SHORT).show()
                return
            }
            editingRecipeId = recipe.id
            nameInput.setText(recipe.name)
            typesInput.setText(recipe.types?.joinToString(", ") ?: "")
            tagsInput.setText(recipe.tags?.joinToString(", ") ?: "")
            descriptionInput.setText(recipe.description ?: "")
            servingsInput.setText(recipe.servings.toString())
            workTimeInput.setText(recipe.workTime?.toString() ?: "")
            prepTimeInput.setText(recipe.prepTime?.toString() ?: "")
            cookTimeInput.setText(recipe.cookTime?.toString() ?: "")
            totalTimeInput.setText(recipe.totalTime?.toString() ?: "")

            ingredientsContainer.removeAllViews()
            recipe.ingredients.forEach { addIngredientRow(it) }

            stepsContainer.removeAllViews()
            recipe.steps.forEach { addStepRow(it) }
        } catch (_: IOException) {
            Toast.makeText(requireContext(), R.string.recipes_edit_file_invalid, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.recipes_edit_file_invalid, Toast.LENGTH_SHORT).show()
        }
    }

    private fun addIngredientRow(existing: com.frombeyond.r2sl.data.export.IngredientJson?) {
        val row = layoutInflater.inflate(R.layout.item_ingredient_input, ingredientsContainer, false)
        val nameInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_name)
        val quantityInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_quantities)
        val notesInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_notes)
        val categoryInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_category)
        val emojiDisplay = row.findViewById<android.widget.TextView>(R.id.ingredient_emoji_display)
        val removeButton = row.findViewById<MaterialButton>(R.id.button_remove_ingredient)

        val emojiManager = IngredientEmojiManager(requireContext())

        existing?.let {
            nameInput.setText(it.name)
            quantityInput.setText(it.quantity.joinToString("; ") { alt -> "${alt.nb} ${alt.unit}" })
            notesInput.setText(it.notes ?: "")
            categoryInput.setText(it.category)
            emojiDisplay.text = it.emoji?.takeIf { e -> e.isNotEmpty() } ?: emojiManager.getSuggestions(it.name).firstOrNull() ?: "📦"
        } ?: run {
            emojiDisplay.text = "📦"
        }

        nameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val suggestion = emojiManager.getSuggestions(name).firstOrNull()
                emojiDisplay.text = suggestion?.takeIf { it.isNotEmpty() } ?: "📦"
            }
        }

        emojiDisplay.setOnClickListener {
            showEmojiPickerForIngredient(nameInput, emojiDisplay, emojiManager)
        }

        removeButton.setOnClickListener {
            ingredientsContainer.removeView(row)
        }
        ingredientsContainer.addView(row)
    }

    private fun showEmojiPickerForIngredient(
        nameInput: TextInputEditText,
        emojiDisplay: android.widget.TextView,
        emojiManager: IngredientEmojiManager
    ) {
        val currentName = nameInput.text?.toString()?.trim().orEmpty()
        val allEntries = buildEmojiNameList(emojiManager, currentName)
        val adapter = object : ArrayAdapter<Pair<String, String>>(
            requireContext(),
            R.layout.item_emoji_picker_row,
            R.id.emoji_row_name,
            allEntries
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val row = convertView ?: layoutInflater.inflate(R.layout.item_emoji_picker_row, parent, false)
                val item = getItem(position) ?: return row
                row.findViewById<TextView>(R.id.emoji_row_emoji).text = item.first
                row.findViewById<TextView>(R.id.emoji_row_name).text = item.second
                return row
            }
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_emoji_picker, null)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.emoji_picker_search)
        val listView = dialogView.findViewById<ListView>(R.id.emoji_picker_list)
        listView.adapter = adapter
        var filteredEntries: List<Pair<String, String>> = allEntries
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase(java.util.Locale.getDefault()).orEmpty()
                filteredEntries = if (query.isEmpty()) allEntries
                else allEntries.filter { (emoji, name) ->
                    name.lowercase(java.util.Locale.getDefault()).contains(query) || emoji.contains(query)
                }
                adapter.clear()
                adapter.addAll(filteredEntries.toMutableList())
                adapter.notifyDataSetChanged()
            }
        })
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.recipes_edit_ingredient_emoji_title))
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        listView.setOnItemClickListener { _, _, position, _ ->
            (adapter.getItem(position))?.let { (emoji, _) ->
                emojiDisplay.text = emoji
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    /** Builds list of (emoji, name) from manager, suggestions first then rest, with fallback defaults. */
    private fun buildEmojiNameList(emojiManager: IngredientEmojiManager, ingredientName: String): MutableList<Pair<String, String>> {
        val all = emojiManager.getAll()
        val suggestions = emojiManager.getSuggestions(ingredientName).toSet()
        val entries = all.entries
            .filter { it.value.isNotEmpty() }
            .map { it.value to it.key }
            .sortedBy { (_, n) -> n.lowercase(java.util.Locale.getDefault()) }
            .toMutableList()
        val reordered = entries.partition { (e, _) -> e in suggestions }
        val result = (reordered.first + reordered.second).distinct().toMutableList()
        if (result.isEmpty()) {
            result.addAll(
                listOf(
                    "📦" to "colis", "🥕" to "carotte", "🥚" to "œuf", "🥛" to "lait",
                    "🧂" to "sel", "🍅" to "tomate", "🧅" to "oignon", "🌿" to "herbes"
                )
            )
        }
        return result
    }

    private fun addStepRow(existing: RecipeStepJson?) {
        val row = layoutInflater.inflate(R.layout.item_step_input, stepsContainer, false)
        val titleInput = row.findViewById<TextInputEditText>(R.id.input_step_title)
        val durationInput = row.findViewById<TextInputEditText>(R.id.input_step_duration)
        val temperatureInput = row.findViewById<TextInputEditText>(R.id.input_step_temperature)
        val notesInput = row.findViewById<TextInputEditText>(R.id.input_step_notes)
        val stepIngredientsInput = row.findViewById<TextInputEditText>(R.id.input_step_ingredients)
        val subStepsContainer = row.findViewById<LinearLayout>(R.id.substeps_container)
        val addSubStepButton = row.findViewById<MaterialButton>(R.id.button_add_substep)
        val removeStepButton = row.findViewById<MaterialButton>(R.id.button_remove_step)

        addSubStepButton.setOnClickListener { addSubStepRow(subStepsContainer, null) }
        removeStepButton.setOnClickListener { stepsContainer.removeView(row) }

        existing?.let { step ->
            titleInput.setText(step.title ?: "")
            durationInput.setText(step.duration?.toString() ?: "")
            temperatureInput.setText(step.temperature ?: "")
            notesInput.setText(step.notes ?: "")
            stepIngredientsInput.setText(formatStepIngredients(step.ingredients))
            step.subSteps.forEach { addSubStepRow(subStepsContainer, it) }
        } ?: addSubStepRow(subStepsContainer, null)

        stepsContainer.addView(row)
    }

    private fun addSubStepRow(container: LinearLayout, existing: SubStepJson?) {
        val row = layoutInflater.inflate(R.layout.item_substep_input, container, false)
        val instructionInput = row.findViewById<TextInputEditText>(R.id.input_substep_instruction)
        val falcInput = row.findViewById<TextInputEditText>(R.id.input_substep_instruction_falc)
        val removeButton = row.findViewById<MaterialButton>(R.id.button_remove_substep)

        existing?.let {
            instructionInput.setText(it.instruction)
            falcInput.setText(it.instructionFalc ?: "")
        }

        removeButton.setOnClickListener { container.removeView(row) }
        container.addView(row)
    }

    private fun saveRecipe() {
        val payload = buildRecipePayload() ?: return
        handleDuplicate(payload, shouldNavigateBack = true)
    }

    private fun exportRecipe() {
        val payload = buildRecipePayload() ?: return
        val fileName = payload.fileName
        exportRecipeLauncher.launch(fileName)
    }

    private fun exportRecipeToUri(uri: Uri) {
        val payload = buildRecipePayload() ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                val jsonText = RecipeJsonFormat(recipes = listOf(payload.recipe)).toJsonObject().toString(2)
                output.write(jsonText.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(requireContext(), R.string.recipes_edit_export_success, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), R.string.recipes_edit_export_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDuplicate(payload: RecipePayload, shouldNavigateBack: Boolean) {
        val existingFile = fileManager.getRecipeFile(payload.fileName)
        val isDuplicate = existingFile != null && existingFile.name != editingFileName
        if (!isDuplicate) {
            savePayload(payload, shouldNavigateBack)
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.recipes_edit_duplicate_title))
            .setMessage(getString(R.string.recipes_edit_duplicate_message))
            .setNegativeButton(getString(R.string.recipes_edit_duplicate_cancel), null)
            .setNeutralButton(getString(R.string.recipes_edit_duplicate_create_second)) { _, _ ->
                val uniqueFileName = generateUniqueFileName(payload.fileName)
                savePayload(payload.copy(fileName = uniqueFileName), shouldNavigateBack)
            }
            .setPositiveButton(getString(R.string.recipes_edit_duplicate_overwrite)) { _, _ ->
                savePayload(payload, shouldNavigateBack)
            }
            .show()
    }

    private fun savePayload(payload: RecipePayload, shouldNavigateBack: Boolean) {
        try {
            val previousFileName = editingFileName
            if (!previousFileName.isNullOrEmpty() && previousFileName != payload.fileName) {
                fileManager.getRecipeFile(previousFileName)?.delete()
                fileManager.removeRecipeIndexEntry(previousFileName)
            }
            fileManager.saveRecipeFile(payload.fileName, RecipeJsonFormat(recipes = listOf(payload.recipe)))
            fileManager.upsertRecipeIndexEntry(payload.fileName, payload.recipe.name)
            Toast.makeText(requireContext(), R.string.recipes_edit_saved, Toast.LENGTH_SHORT).show()
            if (shouldNavigateBack) {
                findNavController().popBackStack()
            }
        } catch (_: IOException) {
            Toast.makeText(requireContext(), R.string.recipes_edit_save_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateUniqueFileName(baseFileName: String): String {
        val base = baseFileName.removeSuffix(".json")
        var index = 2
        var candidate = "${base}_$index.json"
        while (fileManager.getRecipeFile(candidate) != null) {
            index += 1
            candidate = "${base}_$index.json"
        }
        return candidate
    }

    private fun buildRecipePayload(): RecipePayload? {
        nameLayout.error = null
        typesLayout.error = null

        val name = nameInput.text?.toString()?.trim().orEmpty()
        val typesRaw = typesInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            nameLayout.error = getString(R.string.recipes_edit_error_required)
            return null
        }
        if (typesRaw.isEmpty()) {
            typesLayout.error = getString(R.string.recipes_edit_error_required)
            return null
        }

        val types = typesRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (types.isEmpty()) {
            typesLayout.error = getString(R.string.recipes_edit_error_required)
            return null
        }

        val tags = tagsInput.text?.toString()?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        val servings = servingsInput.text?.toString()?.trim()?.toIntOrNull() ?: 1
        val workTime = workTimeInput.text?.toString()?.trim()?.toIntOrNull()
        val prepTime = prepTimeInput.text?.toString()?.trim()?.toIntOrNull()
        val cookTime = cookTimeInput.text?.toString()?.trim()?.toIntOrNull()
        val totalTime = totalTimeInput.text?.toString()?.trim()?.toIntOrNull()
            ?: listOfNotNull(workTime, prepTime, cookTime).sum().takeIf { it > 0 }

        val ingredients = try {
            parseIngredients()
        } catch (error: IllegalArgumentException) {
            // Les ingrédients sont optionnels, on continue avec une liste vide
            emptyList()
        }

        val steps = try {
            parseSteps()
        } catch (error: IllegalArgumentException) {
            // Les étapes sont optionnelles, on continue avec une liste vide
            emptyList()
        }

        // * Préserver les métadonnées existantes lors de l'édition, ou créer des métadonnées par défaut pour une nouvelle recette
        val existingMetadata = if (editingFileName != null) {
            try {
                val file = fileManager.getRecipeFile(editingFileName!!)
                if (file != null) {
                    val format = fileManager.readRecipeFile(file)
                    format.recipes.firstOrNull()?.metadata
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
        
        val metadata = existingMetadata?.copy(
            updatedAt = System.currentTimeMillis()
        ) ?: RecipeMetadataJson.createDefault()
        
        val recipe = RecipeJson(
            id = editingRecipeId ?: "r-${UUID.randomUUID()}",
            name = name,
            description = descriptionInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            servings = servings,
            workTime = workTime,
            prepTime = prepTime,
            cookTime = cookTime,
            totalTime = totalTime,
            types = types,
            tags = tags?.takeIf { it.isNotEmpty() },
            imageUrl = null,
            ingredients = ingredients,
            steps = steps,
            metadata = metadata
        )

        val fileName = sanitizeFileName(name)
        return RecipePayload(recipe, fileName)
    }

    private fun parseIngredients(): List<com.frombeyond.r2sl.data.export.IngredientJson> {
        val ingredients = mutableListOf<com.frombeyond.r2sl.data.export.IngredientJson>()
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)
            val nameInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_name)
            val quantityInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_quantities)
            val notesInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_notes)
            val categoryInput = row.findViewById<TextInputEditText>(R.id.input_ingredient_category)

            val name = nameInput.text?.toString()?.trim().orEmpty()
            val quantitiesRaw = quantityInput.text?.toString()?.trim().orEmpty()
            if (name.isEmpty() || quantitiesRaw.isEmpty()) {
                continue
            }
            try {
                val alternatives = parseQuantityAlternatives(quantitiesRaw)
                if (alternatives.isEmpty()) {
                    continue
                }
                val category = categoryInput.text?.toString()?.trim().orEmpty()
                val emojiDisplay = row.findViewById<android.widget.TextView>(R.id.ingredient_emoji_display)
                val emojiStr = emojiDisplay.text?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "📦" }
                ingredients.add(
                    com.frombeyond.r2sl.data.export.IngredientJson(
                        name = name,
                        category = if (category.isEmpty()) DEFAULT_CATEGORY else category,
                        quantity = alternatives,
                        notes = notesInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                        emoji = emojiStr
                    )
                )
            } catch (_: IllegalArgumentException) {
                // Ignorer les ingrédients avec des quantités invalides
                continue
            }
        }
        // Les ingrédients sont optionnels, on peut retourner une liste vide
        return ingredients
    }

    private fun parseQuantityAlternatives(raw: String): List<QuantityAlternative> {
        return raw.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { part ->
                val tokens = part.split(" ").filter { it.isNotEmpty() }
                val quantity = tokens.firstOrNull()?.replace(",", ".")?.toDoubleOrNull()
                    ?: throw IllegalArgumentException(getString(R.string.recipes_edit_error_quantity))
                val unit = tokens.drop(1).joinToString(" ").trim().ifEmpty { DEFAULT_UNIT }
                QuantityAlternative(quantity, unit)
            }
    }

    private fun parseSteps(): List<RecipeStepJson> {
        val steps = mutableListOf<RecipeStepJson>()
        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val titleInput = row.findViewById<TextInputEditText>(R.id.input_step_title)
            val durationInput = row.findViewById<TextInputEditText>(R.id.input_step_duration)
            val temperatureInput = row.findViewById<TextInputEditText>(R.id.input_step_temperature)
            val notesInput = row.findViewById<TextInputEditText>(R.id.input_step_notes)
            val stepIngredientsInput = row.findViewById<TextInputEditText>(R.id.input_step_ingredients)
            val subStepsContainer = row.findViewById<LinearLayout>(R.id.substeps_container)

            val subSteps = parseSubSteps(subStepsContainer)
            // Les sous-étapes sont optionnelles, on peut avoir une étape vide

            val stepIngredients = try {
                parseStepIngredients(stepIngredientsInput.text?.toString().orEmpty())
            } catch (_: IllegalArgumentException) {
                null
            }
            steps.add(
                RecipeStepJson(
                    stepOrder = steps.size + 1,
                    title = titleInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                    duration = durationInput.text?.toString()?.trim()?.toIntOrNull(),
                    temperature = temperatureInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                    notes = notesInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                    ingredients = stepIngredients,
                    subSteps = subSteps
                )
            )
        }
        // Les étapes sont optionnelles, on peut retourner une liste vide
        return steps
    }

    private fun parseSubSteps(container: LinearLayout): List<SubStepJson> {
        val subSteps = mutableListOf<SubStepJson>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            val instructionInput = row.findViewById<TextInputEditText>(R.id.input_substep_instruction)
            val falcInput = row.findViewById<TextInputEditText>(R.id.input_substep_instruction_falc)
            val instruction = instructionInput.text?.toString()?.trim().orEmpty()
            if (instruction.isEmpty()) {
                continue
            }
            subSteps.add(
                SubStepJson(
                    subStepOrder = subSteps.size + 1,
                    instruction = instruction,
                    instructionFalc = falcInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
        return subSteps
    }

    private fun parseStepIngredients(raw: String): List<StepIngredientJson>? {
        val items = raw.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (items.isEmpty()) {
            return null
        }
        return items.map { item ->
            val parts = item.split(":")
            if (parts.size < 2) {
                throw IllegalArgumentException(getString(R.string.recipes_edit_error_step_ingredient))
            }
            val name = parts[0].trim()
            val quantityTokens = parts[1].trim().split(" ").filter { it.isNotEmpty() }
            val quantity = quantityTokens.firstOrNull()?.replace(",", ".")?.toDoubleOrNull()
                ?: throw IllegalArgumentException(getString(R.string.recipes_edit_error_step_ingredient))
            val unit = quantityTokens.drop(1).joinToString(" ").trim().ifEmpty { DEFAULT_UNIT }
            if (name.isEmpty() || unit.isEmpty()) {
                throw IllegalArgumentException(getString(R.string.recipes_edit_error_step_ingredient))
            }
            StepIngredientJson(
                ingredientName = name,
                quantity = quantity,
                unit = unit,
                notes = null
            )
        }
    }

    private fun formatStepIngredients(ingredients: List<StepIngredientJson>?): String {
        if (ingredients.isNullOrEmpty()) {
            return ""
        }
        return ingredients.joinToString("; ") { ingredient ->
            "${ingredient.ingredientName}: ${ingredient.quantity} ${ingredient.unit}"
        }
    }

    private fun sanitizeFileName(name: String): String {
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{Mn}+".toRegex(), "")
        val slug = withoutAccents.replace("[^A-Za-z0-9]+".toRegex(), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
        return if (slug.isEmpty()) {
            "recette.json"
        } else {
            "$slug.json"
        }
    }

    companion object {
        const val ARG_RECIPE_FILE_NAME = "recipeFileName"
        private const val DEFAULT_CATEGORY = "Autres"
        private const val DEFAULT_UNIT = "piece"
    }

    private data class RecipePayload(
        val recipe: RecipeJson,
        val fileName: String
    )
}
