package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.data.database.Recipe2shopDatabase
import com.frombeyond.r2sl.data.database.entities.Ingredient
import com.frombeyond.r2sl.data.database.entities.Recipe
import com.frombeyond.r2sl.data.database.entities.RecipeStep
import com.frombeyond.r2sl.data.database.entities.StepIngredient
import com.frombeyond.r2sl.data.database.entities.SubStep
import com.frombeyond.r2sl.data.export.QuantityAlternative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * * Service for exporting and importing recipes in JSON format.
 * Handles the conversion between database entities and the formal JSON format.
 */
class RecipeExportImportService(
    private val context: Context,
    private val database: Recipe2shopDatabase
) {
    private companion object {
        const val DEFAULT_CATEGORY = "Autres"
    }
    
    /**
     * * Exports one or more recipes to a JSON file.
     * @param recipeIds List of recipe IDs to export (must not be empty)
     * @param outputFile File to write the JSON to
     * @return true if export was successful, false otherwise
     */
    suspend fun exportRecipesToJson(
        recipeIds: List<String>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (recipeIds.isEmpty()) {
            android.util.Log.e("RecipeExportImportService", "Cannot export: recipeIds list is empty")
            return@withContext false
        }
        try {
            val recipeDao = database.recipeDao()
            val ingredientDao = database.ingredientDao()
            val stepDao = database.recipeStepDao()
            val subStepDao = database.subStepDao()
            val stepIngredientDao = database.stepIngredientDao()
            
            val recipesToExport = if (recipeIds.isEmpty()) {
                // Cannot export all without userId - return empty
                android.util.Log.w("RecipeExportImportService", "Cannot export all recipes without userId")
                emptyList<Recipe>()
            } else {
                recipeIds.mapNotNull { recipeDao.getRecipeById(it) }
            }
            
            val recipeJsons = recipesToExport.map { recipe ->
                val ingredients = ingredientDao.getIngredientsByRecipeSync(recipe.id)
                val steps = stepDao.getStepsByRecipeSync(recipe.id)
                
                // Parse types and tags from comma-separated strings
                val types = recipe.types?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                val tags = recipe.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                
                // Group ingredients by name to create quantity alternatives
                val groupedIngredients = ingredients.groupBy { it.name }
                val ingredientJsons = groupedIngredients.map { (ingredientName, ingredientList) ->
                    val quantityAlternatives = ingredientList.map { ing ->
                        QuantityAlternative(
                            nb = ing.quantity,
                            unit = ing.unit
                        )
                    }
                    // Use the notes from the first ingredient (or combine if needed)
                    val combinedNotes = ingredientList.mapNotNull { it.notes }
                        .distinct()
                        .joinToString("; ")
                        .takeIf { it.isNotEmpty() }

                    val category = ingredientList.map { it.category }
                        .firstOrNull { it.isNotBlank() }
                        ?: DEFAULT_CATEGORY
                    
                    IngredientJson(
                        name = ingredientName,
                        category = category,
                        quantity = quantityAlternatives,
                        notes = combinedNotes ?: ingredientList.firstOrNull()?.notes
                    )
                }
                
                RecipeJson(
                    id = recipe.id,
                    name = recipe.name,
                    description = recipe.description,
                    servings = recipe.servings,
                    workTime = recipe.workTime,
                    prepTime = recipe.prepTime,
                    cookTime = recipe.cookTime,
                    totalTime = recipe.totalTime,
                    types = types,
                    tags = tags,
                    imageUrl = recipe.imageUrl,
                    ingredients = ingredientJsons,
                    steps = steps.map { step ->
                        // Get ingredients for this step
                        val stepIngredients = stepIngredientDao.getIngredientsByStepSync(step.id)
                        val stepIngredientJsons = stepIngredients.mapNotNull { stepIngredient ->
                            // Find the ingredient name from the ingredient ID
                            val ingredient = ingredients.find { it.id == stepIngredient.ingredientId }
                            ingredient?.let {
                                StepIngredientJson(
                                    ingredientName = it.name,
                                    quantity = stepIngredient.quantity,
                                    unit = stepIngredient.unit,
                                    notes = stepIngredient.notes
                                )
                            }
                        }
                        
                        // Get sub-steps for this step
                        val subSteps = subStepDao.getSubStepsByStepSync(step.id)
                        val subStepJsons = subSteps.map { subStep ->
                            SubStepJson(
                                subStepOrder = subStep.subStepOrder,
                                instruction = subStep.instruction,
                                instructionFalc = subStep.instructionFalc
                            )
                        }
                        
                        RecipeStepJson(
                            stepOrder = step.stepOrder,
                            title = step.title,
                            duration = step.duration,
                            temperature = step.temperature,
                            notes = step.notes,
                            ingredients = stepIngredientJsons.takeIf { it.isNotEmpty() },
                            subSteps = subStepJsons
                        )
                    },
                    metadata = RecipeMetadataJson(
                        createdAt = recipe.createdAt,
                        updatedAt = recipe.updatedAt,
                        exportedAt = System.currentTimeMillis()
                    )
                )
            }
            
            val jsonFormat = RecipeJsonFormat(
                version = "1.0",
                recipes = recipeJsons
            )
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            FileWriter(outputFile).use { writer ->
                writer.write(jsonFormat.toJsonObject().toString(2)) // Pretty print with 2 spaces
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("RecipeExportImportService", "Error exporting recipes", e)
            false
        }
    }
    
    /**
     * * Imports recipes from a JSON file.
     * @param inputFile JSON file to import from
     * @param userId User ID to assign to imported recipes
     * @return List of imported recipe IDs, or empty list if import failed
     */
    suspend fun importRecipesFromJson(
        inputFile: File,
        userId: String
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val recipeDao = database.recipeDao()
            val ingredientDao = database.ingredientDao()
            val stepDao = database.recipeStepDao()
            val subStepDao = database.subStepDao()
            val stepIngredientDao = database.stepIngredientDao()
            
            val jsonContent = FileReader(inputFile).use { it.readText() }
            val jsonObject = JSONObject(jsonContent)
            val jsonFormat = RecipeJsonFormat.fromJsonObject(jsonObject)
            
            val importedRecipeIds = mutableListOf<String>()
            
            jsonFormat.recipes.forEach { recipeJson ->
                // Convert types and tags lists to comma-separated strings
                val typesString = recipeJson.types?.joinToString(",")
                val tagsString = recipeJson.tags?.joinToString(",")
                
                // Create recipe entity
                val recipe = Recipe(
                    id = recipeJson.id,
                    userId = userId,
                    name = recipeJson.name,
                    description = recipeJson.description,
                    servings = recipeJson.servings,
                    workTime = recipeJson.workTime,
                    prepTime = recipeJson.prepTime,
                    cookTime = recipeJson.cookTime,
                    totalTime = recipeJson.totalTime,
                    instructions = null, // Instructions are now in steps
                    types = typesString,
                    tags = tagsString,
                    imageUrl = recipeJson.imageUrl,
                    isFavorite = false,
                    createdAt = recipeJson.metadata?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = recipeJson.metadata?.updatedAt ?: System.currentTimeMillis()
                )
                
                recipeDao.insertRecipe(recipe)
                
                // Insert ingredients: expand quantity alternatives into separate ingredient entries
                val ingredientsToInsert = mutableListOf<Ingredient>()
                recipeJson.ingredients.forEach { ingredientJson ->
                    ingredientJson.quantity.forEach { alt ->
                        ingredientsToInsert.add(
                            Ingredient(
                                id = 0, // Auto-generated
                                recipeId = recipe.id,
                                name = ingredientJson.name,
                                quantity = alt.nb,
                                unit = alt.unit,
                                category = ingredientJson.category,
                                notes = ingredientJson.notes
                            )
                        )
                    }
                }
                ingredientDao.insertIngredients(ingredientsToInsert)
                
                // Get inserted ingredients with their IDs
                val insertedIngredients = ingredientDao.getIngredientsByRecipeSync(recipe.id)
                
                // Create a mapping of ingredient name to ID (handle duplicates by taking first match)
                val ingredientNameToId = insertedIngredients.groupBy { it.name }
                    .mapValues { it.value.first().id }
                
                // Insert steps
                recipeJson.steps.forEach { stepJson ->
                    val step = RecipeStep(
                        id = 0, // Auto-generated
                        recipeId = recipe.id,
                        stepOrder = stepJson.stepOrder,
                        title = stepJson.title,
                        duration = stepJson.duration,
                        temperature = stepJson.temperature,
                        notes = stepJson.notes
                    )
                    val stepId = stepDao.insertStep(step)
                    
                    // Insert sub-steps
                    stepJson.subSteps.forEach { subStepJson ->
                        val subStep = SubStep(
                            id = 0, // Auto-generated
                            stepId = stepId,
                            subStepOrder = subStepJson.subStepOrder,
                            instruction = subStepJson.instruction,
                            instructionFalc = subStepJson.instructionFalc
                        )
                        subStepDao.insertSubStep(subStep)
                    }
                    
                    // Insert step ingredients if any
                    stepJson.ingredients?.forEach { stepIngredientJson ->
                        val ingredientId = ingredientNameToId[stepIngredientJson.ingredientName]
                        ingredientId?.let {
                            val stepIngredient = StepIngredient(
                                id = 0,
                                stepId = stepId,
                                ingredientId = it,
                                quantity = stepIngredientJson.quantity,
                                unit = stepIngredientJson.unit,
                                notes = stepIngredientJson.notes
                            )
                            stepIngredientDao.insert(stepIngredient)
                        }
                    }
                }
                
                importedRecipeIds.add(recipe.id)
            }
            
            importedRecipeIds
        } catch (e: Exception) {
            android.util.Log.e("RecipeExportImportService", "Error importing recipes", e)
            emptyList()
        }
    }
}
