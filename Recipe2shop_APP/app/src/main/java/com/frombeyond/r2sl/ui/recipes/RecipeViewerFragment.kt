package com.frombeyond.r2sl.ui.recipes

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.TypedValue
import kotlin.math.abs
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.export.RecipeJson
import com.frombeyond.r2sl.data.export.RecipeJsonPdfGenerator
import com.frombeyond.r2sl.data.export.RecipeStepJson
import com.frombeyond.r2sl.data.export.StepIngredientJson
import com.frombeyond.r2sl.data.AppSettingsManager
import com.frombeyond.r2sl.utils.AccessibilityHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class RecipeViewerFragment : Fragment() {

    private lateinit var scrollView: NestedScrollView
    private lateinit var sectionsContainer: LinearLayout
    private lateinit var currentSectionText: TextView
    private lateinit var prevButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var pdfButton: MaterialButton
    private lateinit var moreInfoButton: MaterialButton
    private lateinit var metadataContainer: LinearLayout
    private lateinit var metadataContent: LinearLayout
    private lateinit var deleteButton: MaterialButton
    private lateinit var fileManager: RecipesLocalFileManager

    private val sections = mutableListOf<Section>()
    private var currentIndex = 0
    private var currentFileName: String? = null
    private var currentRecipe: RecipeJson? = null

    private val exportPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            exportPdfToUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_recipe_viewer, container, false)
        scrollView = root.findViewById(R.id.recipe_viewer_scroll)
        sectionsContainer = root.findViewById(R.id.recipe_viewer_sections)
        currentSectionText = root.findViewById(R.id.recipe_viewer_current_section)
        prevButton = root.findViewById(R.id.button_recipe_prev)
        nextButton = root.findViewById(R.id.button_recipe_next)
        backButton = root.findViewById(R.id.button_recipe_back)
        pdfButton = root.findViewById(R.id.button_recipe_pdf)
        moreInfoButton = root.findViewById(R.id.button_more_info)
        metadataContainer = root.findViewById(R.id.metadata_expandable_container)
        metadataContent = root.findViewById(R.id.metadata_content)
        deleteButton = root.findViewById(R.id.button_delete_recipe)
        fileManager = RecipesLocalFileManager(requireContext())

        backButton.setOnClickListener { findNavController().popBackStack() }
        prevButton.setOnClickListener { navigateToSection(currentIndex - 1) }
        nextButton.setOnClickListener { navigateToSection(currentIndex + 1) }
        pdfButton.setOnClickListener { exportRecipeToPdf() }
        moreInfoButton.setOnClickListener { toggleMetadataVisibility() }
        deleteButton.setOnClickListener { showDeleteConfirmation() }

        val fileName = arguments?.getString(ARG_RECIPE_FILE_NAME)
        if (fileName.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.recipes_viewer_missing_file, Toast.LENGTH_SHORT).show()
            return root
        }

        currentFileName = fileName
        loadRecipe(fileName)
        setupScrollTracking()
        return root
    }

    private fun loadRecipe(fileName: String) {
        val file = fileManager.getRecipeFile(fileName)
        if (file == null) {
            Toast.makeText(requireContext(), R.string.recipes_viewer_missing_file, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val format = fileManager.readRecipeFile(file)
            val recipe = format.recipes.firstOrNull()
            if (recipe == null) {
                Toast.makeText(requireContext(), R.string.recipes_viewer_invalid_file, Toast.LENGTH_SHORT).show()
                return
            }
            currentRecipe = recipe
            renderRecipe(recipe)
            renderMetadata(recipe)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.recipes_viewer_invalid_file, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderRecipe(recipe: RecipeJson) {
        sections.clear()
        sectionsContainer.removeAllViews()

        addSection(
            title = getString(R.string.recipe_viewer_section_presentation),
            contentBuilder = { container ->
                addKeyValue(container, getString(R.string.recipe_viewer_label_name), recipe.name)
                recipe.description?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_description), it)
                }
                addKeyValue(container, getString(R.string.recipe_viewer_label_servings), recipe.servings.toString())
                recipe.types?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_types), it.joinToString(", "))
                }
                recipe.tags?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_tags), it.joinToString(", "))
                }
                val times = buildList {
                    recipe.workTime?.let { add(getString(R.string.recipe_viewer_label_work_time, it)) }
                    recipe.prepTime?.let { add(getString(R.string.recipe_viewer_label_prep_time, it)) }
                    recipe.cookTime?.let { add(getString(R.string.recipe_viewer_label_cook_time, it)) }
                    recipe.totalTime?.let { add(getString(R.string.recipe_viewer_label_total_time, it)) }
                }
                if (times.isNotEmpty()) {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_times), times.joinToString(" | "))
                }
            }
        )

        addSection(
            title = getString(R.string.recipe_viewer_section_ingredients),
            contentBuilder = { container ->
                recipe.ingredients.forEach { ingredient ->
                    val alternatives = ingredient.quantity.joinToString(" OU ") { alt ->
                        "${formatQuantity(alt.nb)} ${normalizeUnit(alt.unit)}"
                    }
                    val value = buildString {
                        append(alternatives)
                        ingredient.notes?.let { append(" (").append(it).append(")") }
                    }
                    addKeyValue(container, "${ingredient.category} - ${ingredient.name}", value)
                }
            }
        )

        recipe.steps.sortedBy { it.stepOrder }.forEach { step ->
            val title = getString(R.string.recipe_viewer_section_step, step.stepOrder)
            addSection(title) { container ->
                step.title?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_step_title), it)
                }
                step.duration?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_step_duration), "$it min")
                }
                step.temperature?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_step_temperature), it)
                }
                step.notes?.let {
                    addKeyValue(container, getString(R.string.recipe_viewer_label_step_notes), it)
                }
                addStepIngredients(container, step.ingredients)
                addSubSteps(container, step)
            }
        }

        currentIndex = 0
        updateNavigationState()
    }

    private fun addStepIngredients(container: LinearLayout, ingredients: List<StepIngredientJson>?) {
        if (ingredients.isNullOrEmpty()) {
            return
        }
        val title = createText(getString(R.string.recipe_viewer_label_step_ingredients))
        setTextSizePx(title, R.dimen.text_size_subtitle)
        container.addView(title)
        ingredients.forEach { ingredient ->
            val text = "${ingredient.ingredientName}: ${formatQuantity(ingredient.quantity)} ${normalizeUnit(ingredient.unit)}"
            container.addView(createText(text))
        }
    }

    private fun addSubSteps(container: LinearLayout, step: RecipeStepJson) {
        val title = createText(getString(R.string.recipe_viewer_label_step_instructions))
        setTextSizePx(title, R.dimen.text_size_subtitle)
        container.addView(title)

        val falcEnabled = AccessibilityHelper.isFalcEnabled(requireContext())
        
        step.subSteps.sortedBy { it.subStepOrder }.forEach { subStep ->
            val instruction = if (falcEnabled && !subStep.instructionFalc.isNullOrBlank()) {
                // Use FALC version if available and FALC mode is enabled
                subStep.instructionFalc!!
            } else {
                // Use normal instruction
                subStep.instruction
            }
            val line = "${subStep.subStepOrder}. $instruction"
            container.addView(createText(line))
            
            // Show both versions if FALC is available but not enabled, or if FALC is enabled but normal is different
            if (!falcEnabled && !subStep.instructionFalc.isNullOrBlank()) {
                val falcText = createText(getString(R.string.recipe_viewer_label_falc, subStep.instructionFalc))
                falcText.setTextColor(requireContext().getColor(R.color.text_secondary))
                container.addView(falcText)
            }
        }
    }

    private fun addSection(title: String, contentBuilder: (LinearLayout) -> Unit) {
        val card = layoutInflater.inflate(R.layout.item_recipe_section, sectionsContainer, false) as MaterialCardView
        val titleText = card.findViewById<TextView>(R.id.recipe_section_title)
        val content = card.findViewById<LinearLayout>(R.id.recipe_section_content)
        titleText.text = title
        contentBuilder(content)
        sectionsContainer.addView(card)
        sections.add(Section(title, card))
    }

    private fun addKeyValue(container: LinearLayout, key: String, value: String) {
        container.addView(createText("$key : $value"))
    }

    private fun createText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextSizePx(this, R.dimen.text_size_medium)
        }
    }

    private fun setTextSizePx(view: TextView, dimenRes: Int) {
        val sizePx = resources.getDimension(dimenRes)
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
    }

    private fun formatQuantity(value: Double): String {
        return if (abs(value % 1.0) < 0.0001) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    private fun normalizeUnit(unit: String): String {
        return unit.ifBlank { "piece" }
    }

    private fun setupScrollTracking() {
        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            val position = scrollView.scrollY + 10
            val index = sections.indexOfLast { it.view.top <= position }.coerceAtLeast(0)
            if (index != currentIndex) {
                currentIndex = index
                updateNavigationState()
            }
        }
    }

    private fun navigateToSection(index: Int) {
        if (index < 0 || index >= sections.size) {
            return
        }
        currentIndex = index
        updateNavigationState()
        scrollView.post {
            scrollView.smoothScrollTo(0, sections[index].view.top)
        }
    }

    private fun updateNavigationState() {
        if (sections.isEmpty()) {
            currentSectionText.text = ""
            prevButton.isEnabled = false
            nextButton.isEnabled = false
            return
        }
        currentSectionText.text = sections[currentIndex].title
        prevButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex < sections.lastIndex
    }

    // * Affiche ou masque le bloc de métadonnées
    private fun toggleMetadataVisibility() {
        val isVisible = metadataContainer.visibility == View.VISIBLE
        metadataContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
        moreInfoButton.text = if (isVisible) "ℹ️ Plus d'info" else "ℹ️ Moins d'info"
    }
    
    // * Affiche les métadonnées dans le bloc dépliable
    private fun renderMetadata(recipe: RecipeJson) {
        metadataContent.removeAllViews()
        val metadata = recipe.metadata
        
        if (metadata != null) {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            
            addMetadataRow("Date de création", dateFormat.format(java.util.Date(metadata.createdAt)))
            addMetadataRow("Dernière modification", dateFormat.format(java.util.Date(metadata.updatedAt)))
            addMetadataRow("Source", metadata.source)
            addMetadataRow("Auteur", metadata.author)
            addMetadataRow("Favoris", if (metadata.favorite) "Oui" else "Non")
            addMetadataRow("Note", "${metadata.rating}/3")
            metadata.exportedAt?.let {
                addMetadataRow("Date d'export", dateFormat.format(java.util.Date(it)))
            }
        } else {
            val noMetadataText = createText("Aucune métadonnée disponible")
            noMetadataText.setTextColor(requireContext().getColor(R.color.text_secondary))
            metadataContent.addView(noMetadataText)
        }
    }
    
    // * Ajoute une ligne de métadonnée
    private fun addMetadataRow(key: String, value: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }
        
        val keyText = TextView(requireContext()).apply {
            text = "$key : "
            setTextSizePx(this, R.dimen.text_size_medium)
            setTextColor(requireContext().getColor(R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val valueText = TextView(requireContext()).apply {
            text = value
            setTextSizePx(this, R.dimen.text_size_medium)
            setTextColor(requireContext().getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        row.addView(keyText)
        row.addView(valueText)
        metadataContent.addView(row)
    }
    
    // * Convertit dp en pixels
    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
    
    // * Affiche un dialogue de confirmation avant de supprimer la recette
    private fun showDeleteConfirmation() {
        val fileName = currentFileName ?: return
        val recipeName = currentRecipe?.name ?: fileName
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Supprimer la recette")
            .setMessage("Êtes-vous sûr de vouloir supprimer la recette \"$recipeName\" ?\n\nCette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteRecipe(fileName)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    // * Supprime la recette
    private fun deleteRecipe(fileName: String) {
        try {
            if (fileManager.deleteRecipe(fileName)) {
                Toast.makeText(requireContext(), "Recette supprimée", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeViewerFragment", "Erreur lors de la suppression", e)
            Toast.makeText(requireContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
        }
    }
    
    // * Exporte la recette en PDF
    private fun exportRecipeToPdf() {
        val recipe = currentRecipe ?: return
        val fileName = "${recipe.name.replace(Regex("[^a-zA-Z0-9]"), "_")}.pdf"
        exportPdfLauncher.launch(fileName)
    }
    
    // * Génère le PDF et l'écrit dans l'URI
    private fun exportPdfToUri(uri: Uri) {
        val recipe = currentRecipe ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                val generator = RecipeJsonPdfGenerator(requireContext())
                generator.generatePdf(recipe, output)
            }
            Toast.makeText(requireContext(), "PDF généré avec succès", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("RecipeViewerFragment", "Erreur lors de la génération du PDF", e)
            Toast.makeText(requireContext(), "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private data class Section(val title: String, val view: View)

    companion object {
        const val ARG_RECIPE_FILE_NAME = "recipeFileName"
    }
}
