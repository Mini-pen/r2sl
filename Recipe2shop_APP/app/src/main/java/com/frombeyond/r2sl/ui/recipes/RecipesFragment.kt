package com.frombeyond.r2sl.ui.recipes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.export.RecipeJsonFormat
import com.frombeyond.r2sl.data.export.RecipeJson
import com.frombeyond.r2sl.data.export.RecipeMetadataJson
import com.frombeyond.r2sl.utils.ErrorLogger
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.IOException

/**
 * * Fragment for viewing local JSON recipes.
 */
class RecipesFragment : Fragment() {

    private lateinit var filesContainer: LinearLayout
    private lateinit var emptyFilesText: TextView
    private lateinit var newRecipeButton: FloatingActionButton
    private lateinit var filterButton: MaterialButton
    private lateinit var favoriteFilterButton: MaterialButton
    private lateinit var ratingFilterButton: MaterialButton
    private lateinit var searchButton: MaterialButton
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchInput: TextInputEditText
    private lateinit var fileManager: RecipesLocalFileManager

    // Filter state
    private var allEntries: List<RecipesLocalFileManager.RecipeIndexEntry> = emptyList()
    private var filteredEntries: List<RecipesLocalFileManager.RecipeIndexEntry> = emptyList()
    private var selectedTypes: Set<String> = emptySet()
    private var selectedTags: Set<String> = emptySet()
    private var nameSearchQuery: String = ""
    private var filterFavoritesOnly: Boolean = false
    private var filterMinRating: Int = 0 // 0 = pas de filtre, 1-3 = note minimum requise

    // Indexed data
    private val allTypes: MutableSet<String> = mutableSetOf()
    private val allTags: MutableSet<String> = mutableSetOf()
    private val entryToRecipe: MutableMap<String, RecipeJson> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_recipes, container, false)
        filesContainer = root.findViewById(R.id.recipes_files_container)
        emptyFilesText = root.findViewById(R.id.recipes_files_empty)
        newRecipeButton = root.findViewById(R.id.button_new_recipe)
        filterButton = root.findViewById(R.id.button_filter_recipe)
        favoriteFilterButton = root.findViewById(R.id.button_filter_favorites)
        ratingFilterButton = root.findViewById(R.id.button_filter_rating)
        searchButton = root.findViewById(R.id.button_search_recipe)
        searchInputLayout = root.findViewById(R.id.search_input_layout)
        searchInput = root.findViewById(R.id.search_input)
        fileManager = RecipesLocalFileManager(requireContext())

        setupListeners()
        loadLocalFiles()
        return root
    }

    override fun onResume() {
        super.onResume()
        loadLocalFiles()
    }

    private fun setupListeners() {
        newRecipeButton.setOnClickListener {
            findNavController().navigate(R.id.edit_recipe)
        }

        filterButton.setOnClickListener {
            showFilterDialog()
        }

        favoriteFilterButton.setOnClickListener {
            filterFavoritesOnly = !filterFavoritesOnly
            updateFavoriteFilterButton()
            applyFilters()
        }
        updateFavoriteFilterButton()

        ratingFilterButton.setOnClickListener {
            showRatingFilterDialog()
        }
        updateRatingFilterButton()

        searchButton.setOnClickListener {
            val isVisible = searchInputLayout.isVisible
            searchInputLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) {
                searchInput.requestFocus()
            } else {
                searchInput.setText("")
                nameSearchQuery = ""
                applyFilters()
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                nameSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    private fun loadLocalFiles() {
        try {
            allEntries = fileManager.listRecipeEntries()
            if (allEntries.isNotEmpty()) {
                indexRecipes()
            }
            applyFilters()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors du chargement des fichiers de recettes", e, "RecipesFragment")
            android.util.Log.e("RecipesFragment", "Erreur lors du chargement des fichiers", e)
            // Afficher un message d'erreur à l'utilisateur
            emptyFilesText.text = "Erreur lors du chargement des recettes"
            emptyFilesText.isVisible = true
        }
    }

    private fun indexRecipes() {
        try {
            allTypes.clear()
            allTags.clear()
            entryToRecipe.clear()

            allEntries.forEach { entry ->
                try {
                    val file = fileManager.getRecipeFile(entry.fileName) ?: return@forEach
                    val format = fileManager.readRecipeFile(file)
                    val recipe = format.recipes.firstOrNull() ?: return@forEach
                    
                    entryToRecipe[entry.fileName] = recipe
                    
                    recipe.types?.forEach { type ->
                        allTypes.add(type)
                    }
                    
                    recipe.tags?.forEach { tag ->
                        allTags.add(tag)
                    }
                } catch (e: IOException) {
                    ErrorLogger.getInstance().logError("Erreur lors de l'indexation de la recette: ${entry.fileName}", e, "RecipesFragment")
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur inattendue lors de l'indexation: ${entry.fileName}", e, "RecipesFragment")
                }
            }
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de l'indexation des recettes", e, "RecipesFragment")
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_filter, null)
        val typesContainer = dialogView.findViewById<LinearLayout>(R.id.filter_types_container)
        val tagsContainer = dialogView.findViewById<LinearLayout>(R.id.filter_tags_container)
        val tagSearchInput = dialogView.findViewById<TextInputEditText>(R.id.filter_tag_search)
        val selectedTagsContainer = dialogView.findViewById<LinearLayout>(R.id.filter_selected_tags)
        val showAllButton = dialogView.findViewById<MaterialButton>(R.id.filter_show_all)

        // Create type checkboxes
        val typeCheckboxes = mutableMapOf<String, CheckBox>()
        allTypes.sorted().forEach { type ->
            val checkBox = CheckBox(requireContext()).apply {
                text = type
                isChecked = selectedTypes.isEmpty() || selectedTypes.contains(type)
            }
            typeCheckboxes[type] = checkBox
            typesContainer.addView(checkBox)
        }

        // Create tag search with autocomplete
        val availableTags = allTags.sorted().toMutableList()
        val selectedTagsList = selectedTags.toMutableSet()
        
        fun updateSelectedTagsDisplay() {
            selectedTagsContainer.removeAllViews()
            selectedTagsList.forEach { tag ->
                val tagChip = layoutInflater.inflate(R.layout.item_filter_tag, selectedTagsContainer, false)
                val tagText = tagChip.findViewById<TextView>(R.id.tag_text)
                val tagRemove = tagChip.findViewById<MaterialButton>(R.id.tag_remove)
                tagText.text = tag
                tagRemove.setOnClickListener {
                    selectedTagsList.remove(tag)
                    updateSelectedTagsDisplay()
                }
                selectedTagsContainer.addView(tagChip)
            }
        }
        updateSelectedTagsDisplay()

        tagSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    val query = s?.toString()?.trim()?.lowercase() ?: ""
                    if (query.isEmpty()) {
                        tagsContainer.removeAllViews()
                        return
                    }
                    
                    val matchingTags = availableTags.filter { 
                        it.lowercase().contains(query) && !selectedTagsList.contains(it)
                    }
                    
                    tagsContainer.removeAllViews()
                    matchingTags.take(5).forEach { tag ->
                        val tagView = TextView(requireContext()).apply {
                            text = tag
                            setPadding(16, 16, 16, 16)
                            setOnClickListener {
                                selectedTagsList.add(tag)
                                tagSearchInput.setText("")
                                updateSelectedTagsDisplay()
                            }
                        }
                        tagsContainer.addView(tagView)
                    }
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur lors de la recherche de tags", e, "RecipesFragment")
                }
            }
        })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recipes_filter)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selectedTypes = typeCheckboxes.filter { it.value.isChecked }.keys.toSet()
                selectedTags = selectedTagsList
                applyFilters()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        // Auto-close when "Tout afficher" is clicked
        showAllButton.setOnClickListener {
            selectedTypes = emptySet()
            selectedTags = emptySet()
            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    // * Met à jour l'apparence du bouton de filtre favoris
    private fun updateFavoriteFilterButton() {
        favoriteFilterButton.text = if (filterFavoritesOnly) "⭐" else "☆"
        favoriteFilterButton.alpha = 1.0f
    }

    // * Affiche le dialogue de sélection de note minimum
    private fun showRatingFilterDialog() {
        val options = arrayOf(
            "Toutes (0+)",
            "1 cœur minimum (❤️)",
            "2 cœurs minimum (❤️❤️)",
            "3 cœurs minimum (❤️❤️❤️)"
        )
        val selectedIndex = filterMinRating

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Note minimum")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                filterMinRating = which
                updateRatingFilterButton()
                applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // * Met à jour l'apparence du bouton de filtre de note
    private fun updateRatingFilterButton() {
        ratingFilterButton.text = when (filterMinRating) {
            0 -> "🤍"
            1 -> "❤️"
            2 -> "❤️❤️"
            3 -> "❤️❤️❤️"
            else -> "🤍"
        }
        ratingFilterButton.alpha = if (filterMinRating > 0) 1.0f else 0.6f
    }

    private fun applyFilters() {
        try {
            if (allEntries.isEmpty()) {
                filteredEntries = emptyList()
                displayRecipes()
                return
            }
            
            filteredEntries = allEntries.filter { entry ->
                try {
                    // Filter by name first (works even without recipe loaded)
                    if (nameSearchQuery.isNotEmpty()) {
                        if (!entry.name.lowercase().contains(nameSearchQuery.lowercase())) {
                            return@filter false
                        }
                    }

                    val recipe = entryToRecipe[entry.fileName]
                    
                    // If recipe is not loaded yet, only filter by name
                    if (recipe == null) {
                        return@filter nameSearchQuery.isBlank()
                    }

                    // Filter by types
                    if (selectedTypes.isNotEmpty()) {
                        val recipeTypes = recipe.types?.toSet() ?: emptySet()
                        if (recipeTypes.intersect(selectedTypes).isEmpty()) {
                            return@filter false
                        }
                    }

                    // Filter by tags
                    if (selectedTags.isNotEmpty()) {
                        val recipeTags = recipe.tags?.toSet() ?: emptySet()
                        if (recipeTags.intersect(selectedTags).isEmpty()) {
                            return@filter false
                        }
                    }

                    // Filter by favorites
                    if (filterFavoritesOnly) {
                        val isFavorite = recipe.metadata?.favorite ?: false
                        if (!isFavorite) {
                            return@filter false
                        }
                    }

                    // Filter by minimum rating
                    if (filterMinRating > 0) {
                        val recipeRating = recipe.metadata?.rating ?: 0
                        if (recipeRating < filterMinRating) {
                            return@filter false
                        }
                    }

                    true
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur lors du filtrage de la recette: ${entry.fileName}", e, "RecipesFragment")
                    false
                }
            }

            displayRecipes()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de l'application des filtres", e, "RecipesFragment")
            filteredEntries = emptyList()
            displayRecipes()
        }
    }

    private fun displayRecipes() {
        try {
            filesContainer.removeAllViews()
            emptyFilesText.isVisible = filteredEntries.isEmpty()
            filteredEntries.forEach { entry ->
                try {
                    filesContainer.addView(createFileButton(entry))
                } catch (e: Exception) {
                    ErrorLogger.getInstance().logError("Erreur lors de l'affichage de la recette: ${entry.fileName}", e, "RecipesFragment")
                }
            }
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de l'affichage des recettes", e, "RecipesFragment")
        }
    }

    private fun createFileButton(entry: RecipesLocalFileManager.RecipeIndexEntry): View {
        val row = layoutInflater.inflate(R.layout.item_recipe_file, filesContainer, false)
        val nameText = row.findViewById<TextView>(R.id.recipe_file_name)
        val favoriteStar = row.findViewById<TextView>(R.id.recipe_favorite_star)
        val ratingHearts = row.findViewById<TextView>(R.id.recipe_rating_hearts)
        val editButton = row.findViewById<MaterialButton>(R.id.button_edit_recipe)

        nameText.text = entry.name
        
        // Charger les métadonnées de la recette
        val recipe = entryToRecipe[entry.fileName]
        val metadata = recipe?.metadata
        
        // Afficher l'étoile favoris
        val isFavorite = metadata?.favorite ?: false
        favoriteStar.text = if (isFavorite) "⭐" else "☆"
        favoriteStar.alpha = 1.0f
        
        // Afficher les cœurs de note
        val rating = metadata?.rating ?: 0
        ratingHearts.text = when (rating) {
            0 -> "🤍"
            1 -> "❤️"
            2 -> "❤️❤️"
            3 -> "❤️❤️❤️"
            else -> "🤍"
        }
        
        // Gérer le clic sur l'étoile (toggle favoris)
        favoriteStar.setOnClickListener {
            toggleFavorite(entry.fileName, recipe)
        }
        
        // Gérer le clic sur les cœurs (ouvrir dialogue de sélection de note)
        ratingHearts.setOnClickListener {
            showRatingDialog(entry.fileName, recipe, rating)
        }
        
        row.setOnClickListener { openRecipeViewer(entry.fileName) }
        editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString(EditRecipeFragment.ARG_RECIPE_FILE_NAME, entry.fileName)
            }
            findNavController().navigate(R.id.edit_recipe, bundle)
        }
        return row
    }
    
    // * Toggle le statut favoris d'une recette
    private fun toggleFavorite(fileName: String, recipe: RecipeJson?) {
        try {
            val file = fileManager.getRecipeFile(fileName) ?: return
            val format = fileManager.readRecipeFile(file)
            val currentRecipe = format.recipes.firstOrNull() ?: return
            
            val currentMetadata = currentRecipe.metadata ?: RecipeMetadataJson.createDefault()
            val newMetadata = currentMetadata.copy(
                favorite = !currentMetadata.favorite,
                updatedAt = System.currentTimeMillis()
            )
            
            val updatedRecipe = currentRecipe.copy(metadata = newMetadata)
            val updatedFormat = format.copy(recipes = listOf(updatedRecipe))
            
            fileManager.saveRecipeFile(fileName, updatedFormat)
            
            // Mettre à jour le cache
            entryToRecipe[fileName] = updatedRecipe
            
            // Rafraîchir l'affichage
            loadLocalFiles()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de la modification du favoris", e, "RecipesFragment")
            android.util.Log.e("RecipesFragment", "Erreur lors du toggle favoris", e)
        }
    }
    
    // * Affiche un dialogue pour sélectionner la note (0-3)
    private fun showRatingDialog(fileName: String, recipe: RecipeJson?, currentRating: Int) {
        val options = arrayOf("0 - 🤍 Aucune note", "1 - ❤️", "2 - ❤️❤️", "3 - ❤️❤️❤️")
        val selectedIndex = currentRating
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Note de la recette")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                updateRating(fileName, recipe, which)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    // * Met à jour la note d'une recette
    private fun updateRating(fileName: String, recipe: RecipeJson?, newRating: Int) {
        try {
            val file = fileManager.getRecipeFile(fileName) ?: return
            val format = fileManager.readRecipeFile(file)
            val currentRecipe = format.recipes.firstOrNull() ?: return
            
            val currentMetadata = currentRecipe.metadata ?: RecipeMetadataJson.createDefault()
            val newMetadata = currentMetadata.copy(
                rating = newRating.coerceIn(0, 3),
                updatedAt = System.currentTimeMillis()
            )
            
            val updatedRecipe = currentRecipe.copy(metadata = newMetadata)
            val updatedFormat = format.copy(recipes = listOf(updatedRecipe))
            
            fileManager.saveRecipeFile(fileName, updatedFormat)
            
            // Mettre à jour le cache
            entryToRecipe[fileName] = updatedRecipe
            
            // Rafraîchir l'affichage
            loadLocalFiles()
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors de la modification de la note", e, "RecipesFragment")
            android.util.Log.e("RecipesFragment", "Erreur lors de la mise à jour de la note", e)
        }
    }

    private fun openRecipeViewer(fileName: String) {
        val bundle = Bundle().apply {
            putString(RecipeViewerFragment.ARG_RECIPE_FILE_NAME, fileName)
        }
        findNavController().navigate(R.id.recipe_viewer, bundle)
    }

}