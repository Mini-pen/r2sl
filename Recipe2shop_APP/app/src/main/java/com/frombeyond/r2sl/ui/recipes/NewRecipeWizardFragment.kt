package com.frombeyond.r2sl.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.export.IngredientJson
import com.frombeyond.r2sl.data.export.QuantityAlternative
import com.frombeyond.r2sl.data.export.RecipeJson
import com.frombeyond.r2sl.data.export.RecipeJsonFormat
import com.frombeyond.r2sl.data.export.RecipeMetadataJson
import com.frombeyond.r2sl.data.local.IngredientEmojiManager
import com.frombeyond.r2sl.data.local.RayonsManager
import com.frombeyond.r2sl.ui.BaseFragment
import com.frombeyond.r2sl.utils.RayonPickerHelper
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

class NewRecipeWizardFragment : BaseFragment() {

    private lateinit var fileManager: RecipesLocalFileManager
    private lateinit var rayonsManager: RayonsManager
    private lateinit var emojiManager: IngredientEmojiManager

    private lateinit var stepTitle: TextView
    private lateinit var nameStepContainer: View
    private lateinit var ingredientsStepContainer: View
    private lateinit var nameInput: TextInputEditText
    private lateinit var typesInput: TextInputEditText
    private lateinit var ingredientsList: LinearLayout
    private lateinit var ingredientNameInput: TextInputEditText
    private lateinit var ingredientQtyInput: TextInputEditText
    private lateinit var ingredientCategoryInput: TextInputEditText
    private lateinit var backButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var addIngredientButton: MaterialButton

    private var wizardStep = 0
    private val wizardIngredients = mutableListOf<IngredientJson>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_new_recipe_wizard, container, false)
        fileManager = RecipesLocalFileManager(requireContext())
        rayonsManager = RayonsManager(requireContext())
        emojiManager = IngredientEmojiManager(requireContext())

        stepTitle = root.findViewById(R.id.wizard_step_title)
        nameStepContainer = root.findViewById(R.id.wizard_step_name_container)
        ingredientsStepContainer = root.findViewById(R.id.wizard_step_ingredients_container)
        nameInput = root.findViewById(R.id.wizard_recipe_name)
        typesInput = root.findViewById(R.id.wizard_recipe_types)
        ingredientsList = root.findViewById(R.id.wizard_ingredients_list)
        ingredientNameInput = root.findViewById(R.id.wizard_ingredient_name)
        ingredientQtyInput = root.findViewById(R.id.wizard_ingredient_quantity)
        ingredientCategoryInput = root.findViewById(R.id.wizard_ingredient_category)
        backButton = root.findViewById(R.id.wizard_back)
        nextButton = root.findViewById(R.id.wizard_next)
        addIngredientButton = root.findViewById(R.id.wizard_add_ingredient)

        ingredientCategoryInput.setText(RayonsManager.DEFAULT_CATEGORY)
        ingredientCategoryInput.setOnClickListener {
            RayonPickerHelper.show(requireContext(), rayonsManager, ingredientCategoryInput.text?.toString()) { selected ->
                ingredientCategoryInput.setText(selected)
            }
        }

        addIngredientButton.setOnClickListener { addIngredientFromForm() }
        backButton.setOnClickListener { goToPreviousStep() }
        nextButton.setOnClickListener { goToNextStep() }

        updateStepUi()
        return root
    }

    private fun goToNextStep() {
        if (wizardStep == 0) {
            if (nameInput.text?.toString()?.trim().isNullOrEmpty()) {
                Toast.makeText(requireContext(), R.string.recipes_edit_error_required, Toast.LENGTH_SHORT).show()
                return
            }
            if (typesInput.text?.toString()?.trim().isNullOrEmpty()) {
                Toast.makeText(requireContext(), R.string.recipes_edit_error_required, Toast.LENGTH_SHORT).show()
                return
            }
            wizardStep = 1
            updateStepUi()
            return
        }
        saveRecipeAndFinish()
    }

    private fun goToPreviousStep() {
        if (wizardStep > 0) {
            wizardStep -= 1
            updateStepUi()
        }
    }

    private fun updateStepUi() {
        val onNameStep = wizardStep == 0
        nameStepContainer.visibility = if (onNameStep) View.VISIBLE else View.GONE
        ingredientsStepContainer.visibility = if (onNameStep) View.GONE else View.VISIBLE
        backButton.visibility = if (onNameStep) View.GONE else View.VISIBLE
        stepTitle.text = getString(if (onNameStep) R.string.wizard_step_name_title else R.string.wizard_step_ingredients_title)
        nextButton.text = getString(if (onNameStep) R.string.wizard_next else R.string.wizard_finish)
        refreshIngredientsList()
    }

    private fun addIngredientFromForm() {
        val name = ingredientNameInput.text?.toString()?.trim().orEmpty()
        val qtyRaw = ingredientQtyInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || qtyRaw.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wizard_ingredient_incomplete, Toast.LENGTH_SHORT).show()
            return
        }
        val alternatives = parseQuantityAlternatives(qtyRaw) ?: run {
            Toast.makeText(requireContext(), R.string.recipes_edit_error_quantity, Toast.LENGTH_SHORT).show()
            return
        }
        val category = ingredientCategoryInput.text?.toString()?.trim().orEmpty()
            .ifBlank { RayonsManager.DEFAULT_CATEGORY }
        val emoji = emojiManager.getSuggestions(name).firstOrNull()?.takeIf { !it.isNullOrEmpty() }
        wizardIngredients.add(
            IngredientJson(
                name = name,
                category = category,
                quantity = alternatives,
                notes = null,
                emoji = emoji
            )
        )
        ingredientNameInput.setText("")
        ingredientQtyInput.setText("")
        refreshIngredientsList()
    }

    private fun refreshIngredientsList() {
        ingredientsList.removeAllViews()
        if (wizardIngredients.isEmpty()) {
            val hint = TextView(requireContext()).apply {
                text = getString(R.string.wizard_ingredients_empty_hint)
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }
            ingredientsList.addView(hint)
            return
        }
        wizardIngredients.forEachIndexed { index, ingredient ->
            val alt = ingredient.quantity.firstOrNull()
            val line = "${ingredient.name} — ${alt?.nb} ${alt?.unit} (${ingredient.category})"
            val row = TextView(requireContext()).apply {
                text = line
                setPadding(0, 8, 0, 8)
            }
            row.setOnLongClickListener {
                wizardIngredients.removeAt(index)
                refreshIngredientsList()
                true
            }
            ingredientsList.addView(row)
        }
    }

    private fun saveRecipeAndFinish() {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        val types = typesInput.text?.toString()?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("Plat")

        val recipe = RecipeJson(
            id = "r-${UUID.randomUUID()}",
            name = name,
            description = null,
            servings = 4,
            workTime = null,
            prepTime = null,
            cookTime = null,
            totalTime = null,
            types = types,
            tags = null,
            imageUrl = null,
            ingredients = wizardIngredients.toList(),
            steps = emptyList(),
            metadata = RecipeMetadataJson.createDefault()
        )

        val fileName = sanitizeFileName(name)
        try {
            fileManager.saveRecipeFile(fileName, RecipeJsonFormat(recipes = listOf(recipe)))
            fileManager.upsertRecipeIndexEntry(fileName, recipe.name)
            Toast.makeText(requireContext(), R.string.recipes_edit_saved, Toast.LENGTH_SHORT).show()

            val addToMealDate = arguments?.getString(EditRecipeFragment.ARG_ADD_TO_MEAL_DATE)
            val addToMealType = arguments?.getString(EditRecipeFragment.ARG_ADD_TO_MEAL_TYPE)
            if (!addToMealDate.isNullOrBlank() && !addToMealType.isNullOrBlank()) {
                val result = Bundle().apply {
                    putString(EditRecipeFragment.RESULT_RECIPE_NAME, recipe.name)
                    putString(EditRecipeFragment.RESULT_RECIPE_FILE_NAME, fileName)
                    putString(EditRecipeFragment.RESULT_ADD_TO_MEAL_DATE, addToMealDate)
                    putString(EditRecipeFragment.RESULT_ADD_TO_MEAL_TYPE, addToMealType)
                }
                parentFragmentManager.setFragmentResult(EditRecipeFragment.RESULT_KEY_ADD_RECIPE_TO_MEAL, result)
                findNavController().popBackStack()
                return
            }

            if (arguments?.getBoolean(ARG_OPEN_RECIPE_AFTER, false) == true) {
                val bundle = Bundle().apply {
                    putString(RecipeViewerFragment.ARG_RECIPE_FILE_NAME, fileName)
                }
                findNavController().navigate(R.id.recipe_viewer, bundle)
                findNavController().popBackStack(R.id.new_recipe_wizard, true)
                return
            }

            findNavController().popBackStack()
        } catch (_: IOException) {
            Toast.makeText(requireContext(), R.string.recipes_edit_save_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseQuantityAlternatives(raw: String): List<QuantityAlternative>? {
        return try {
            raw.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { part ->
                    val tokens = part.split(" ").filter { it.isNotEmpty() }
                    val quantity = tokens.firstOrNull()?.replace(",", ".")?.toDoubleOrNull()
                        ?: throw IllegalArgumentException()
                    val unit = tokens.drop(1).joinToString(" ").trim().ifEmpty { "pièce" }
                    QuantityAlternative(nb = quantity, unit = unit)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeFileName(name: String): String {
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{Mn}+".toRegex(), "")
        val slug = withoutAccents.replace("[^A-Za-z0-9]+".toRegex(), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
        val base = if (slug.isBlank()) "recette" else slug
        var candidate = "$base.json"
        var index = 2
        while (fileManager.getRecipeFile(candidate) != null) {
            candidate = "${base}_$index.json"
            index++
        }
        return candidate
    }

    companion object {
        const val ARG_OPEN_RECIPE_AFTER = "open_recipe_after"
    }
}
