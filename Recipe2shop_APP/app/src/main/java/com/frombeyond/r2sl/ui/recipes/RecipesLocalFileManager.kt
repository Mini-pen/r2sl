package com.frombeyond.r2sl.ui.recipes

import android.content.Context
import com.frombeyond.r2sl.data.export.RecipeJsonFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.json.JSONObject
import org.json.JSONArray

/**
 * * Handles local JSON recipe files stored in the app files directory.
 */
class RecipesLocalFileManager(private val context: Context) {

    private val recipesDir: File by lazy {
        File(context.filesDir, RECIPES_DIR_NAME)
    }
    private val indexFile: File by lazy {
        File(recipesDir, INDEX_FILE_NAME)
    }

    fun ensureSamplesSeeded(): Int {
        if (!recipesDir.exists() && !recipesDir.mkdirs()) {
            return 0
        }

        val seeded = seedFromIndexIfNeeded()
        if (seeded >= 0) {
            rebuildIndexFromFiles()
            return seeded
        }

        val assets = context.assets.list(ASSETS_RECIPES_DIR) ?: return 0
        var seededCount = 0
        for (assetName in assets) {
            if (assetName.equals(INDEX_FILE_NAME, ignoreCase = true)) {
                continue
            }
            val targetFile = File(recipesDir, assetName)
            if (targetFile.exists()) {
                continue
            }
            val assetPath = "$ASSETS_RECIPES_DIR/$assetName"
            if (copyAssetToFile(assetPath, targetFile)) {
                seededCount += 1
            }
        }
        rebuildIndexFromFiles()
        return seededCount
    }

    fun listRecipeFiles(): List<File> {
        if (!recipesDir.exists()) {
            return emptyList()
        }
        return recipesDir.listFiles { file ->
            file.isFile &&
                file.extension.equals("json", ignoreCase = true) &&
                !file.name.equals(INDEX_FILE_NAME, ignoreCase = true)
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    fun listRecipeEntries(): List<RecipeIndexEntry> {
        if (!recipesDir.exists()) {
            return emptyList()
        }
        val entries = loadIndexEntries().toMutableList()
        val updated = syncIndexWithFiles(entries)
        if (updated) {
            saveIndexEntries(entries)
        }
        return entries.sortedBy { it.name.lowercase() }
    }

    fun getRecipeFile(fileName: String): File? {
        val file = File(recipesDir, fileName)
        return if (file.exists() && file.isFile) {
            file
        } else {
            null
        }
    }

    @Throws(IOException::class)
    fun readRecipeFile(file: File): RecipeJsonFormat {
        val jsonText = file.readText(Charsets.UTF_8)
        val jsonObject = JSONObject(jsonText)
        val updated = ensureIngredientCategories(jsonObject)
        if (updated) {
            file.writeText(jsonObject.toString(2), Charsets.UTF_8)
        }
        return RecipeJsonFormat.fromJsonObject(jsonObject)
    }

    @Throws(IOException::class)
    fun saveRecipeFile(fileName: String, format: RecipeJsonFormat) {
        if (!recipesDir.exists()) {
            recipesDir.mkdirs()
        }
        val targetFile = File(recipesDir, fileName)
        val jsonText = format.toJsonObject().toString(2)
        targetFile.writeText(jsonText, Charsets.UTF_8)
    }

    fun upsertRecipeIndexEntry(fileName: String, recipeName: String) {
        if (!recipesDir.exists()) {
            recipesDir.mkdirs()
        }
        val entries = loadIndexEntries().toMutableList()
        val existingIndex = entries.indexOfFirst { it.fileName == fileName }
        if (existingIndex >= 0) {
            entries[existingIndex] = entries[existingIndex].copy(name = recipeName)
        } else {
            entries.add(RecipeIndexEntry(fileName, recipeName))
        }
        saveIndexEntries(entries)
    }

    fun removeRecipeIndexEntry(fileName: String) {
        if (!indexFile.exists()) {
            return
        }
        val entries = loadIndexEntries().filterNot { it.fileName == fileName }
        saveIndexEntries(entries)
    }

    fun refreshIndex() {
        rebuildIndexFromFiles()
    }
    
    // * Met à jour les métadonnées de toutes les recettes existantes
    fun updateAllRecipesMetadata(source: String, author: String): Int {
        if (!recipesDir.exists()) {
            return 0
        }
        val files = listRecipeFiles()
        var updatedCount = 0
        val now = System.currentTimeMillis()
        
        files.forEach { file ->
            try {
                val format = readRecipeFile(file)
                val recipe = format.recipes.firstOrNull() ?: return@forEach
                
                val currentMetadata = recipe.metadata
                val newMetadata = if (currentMetadata != null) {
                    currentMetadata.copy(
                        createdAt = now,
                        updatedAt = now,
                        source = source,
                        author = author
                    )
                } else {
                    com.frombeyond.r2sl.data.export.RecipeMetadataJson(
                        createdAt = now,
                        updatedAt = now,
                        exportedAt = null,
                        source = source,
                        author = author,
                        favorite = false,
                        rating = 0
                    )
                }
                
                val updatedRecipe = recipe.copy(metadata = newMetadata)
                val updatedFormat = format.copy(recipes = listOf(updatedRecipe))
                saveRecipeFile(file.name, updatedFormat)
                updatedCount++
            } catch (e: Exception) {
                android.util.Log.e("RecipesLocalFileManager", "Erreur lors de la mise à jour de ${file.name}", e)
            }
        }
        
        return updatedCount
    }
    
    // * Supprime une recette et son entrée dans l'index
    fun deleteRecipe(fileName: String): Boolean {
        return try {
            val file = getRecipeFile(fileName)
            if (file != null && file.delete()) {
                removeRecipeIndexEntry(fileName)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipesLocalFileManager", "Erreur lors de la suppression de $fileName", e)
            false
        }
    }
    
    // * Corrige les types de source et vérifie la cohérence des ingrédients de toutes les recettes
    fun fixRecipesSourcesAndIngredients(): Pair<Int, Int> {
        if (!recipesDir.exists()) {
            return Pair(0, 0)
        }
        val files = listRecipeFiles()
        var sourceFixedCount = 0
        var ingredientsFixedCount = 0
        
        files.forEach { file ->
            try {
                val format = readRecipeFile(file)
                val recipe = format.recipes.firstOrNull() ?: return@forEach
                
                var needsUpdate = false
                var updatedRecipe = recipe
                
                // Vérifier et corriger la source
                val currentSource = recipe.metadata?.source ?: "manual_entry"
                val validSources = setOf("manual_entry", "website", "book", "r2sl_recipes_pack")
                val correctedSource = if (currentSource in validSources) {
                    currentSource
                } else {
                    // Si la source n'est pas valide, déterminer le bon type
                    // Si c'est "debug_pack", on le garde (c'est une valeur acceptée même si pas dans la liste officielle)
                    if (currentSource == "debug_pack") {
                        currentSource
                    } else {
                        needsUpdate = true
                        sourceFixedCount++
                        "manual_entry"
                    }
                }
                
                // Vérifier et corriger les ingrédients
                val fixedIngredients = recipe.ingredients.map { ingredient ->
                    var fixedIngredient = ingredient
                    var ingredientFixed = false
                    
                    // Vérifier la catégorie
                    if (ingredient.category.isBlank()) {
                        fixedIngredient = ingredient.copy(category = DEFAULT_CATEGORY)
                        ingredientFixed = true
                    }
                    
                    // Vérifier les quantités alternatives
                    val fixedQuantities = fixedIngredient.quantity.mapNotNull { qty ->
                        if (qty.nb < 0) {
                            ingredientFixed = true
                            null // Supprimer les quantités négatives
                        } else if (qty.unit.isBlank()) {
                            ingredientFixed = true
                            qty.copy(unit = "unité") // Unité par défaut si vide
                        } else {
                            qty
                        }
                    }
                    
                    // Si toutes les quantités ont été supprimées, ajouter une quantité par défaut
                    val finalQuantities = if (fixedQuantities.isEmpty()) {
                        ingredientFixed = true
                        listOf(com.frombeyond.r2sl.data.export.QuantityAlternative(1.0, "unité"))
                    } else {
                        fixedQuantities
                    }
                    
                    // Retourner l'ingrédient corrigé si nécessaire
                    if (ingredientFixed || finalQuantities != ingredient.quantity) {
                        fixedIngredient.copy(quantity = finalQuantities)
                    } else {
                        ingredient
                    }
                }
                
                if (fixedIngredients != recipe.ingredients) {
                    needsUpdate = true
                    ingredientsFixedCount++
                    updatedRecipe = recipe.copy(ingredients = fixedIngredients)
                }
                
                // Mettre à jour les métadonnées si nécessaire
                if (correctedSource != currentSource || needsUpdate) {
                    val updatedMetadata = (recipe.metadata ?: com.frombeyond.r2sl.data.export.RecipeMetadataJson.createDefault()).copy(
                        source = correctedSource,
                        updatedAt = System.currentTimeMillis()
                    )
                    updatedRecipe = updatedRecipe.copy(metadata = updatedMetadata)
                }
                
                // Sauvegarder si des modifications ont été apportées
                if (needsUpdate || correctedSource != currentSource) {
                    val updatedFormat = format.copy(recipes = listOf(updatedRecipe))
                    saveRecipeFile(file.name, updatedFormat)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipesLocalFileManager", "Erreur lors de la correction de ${file.name}", e)
            }
        }
        
        return Pair(sourceFixedCount, ingredientsFixedCount)
    }

    private fun copyAssetToFile(assetPath: String, targetFile: File): Boolean {
        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun seedFromIndexIfNeeded(): Int {
        return try {
            val assetPath = "$ASSETS_RECIPES_DIR/$INDEX_FILE_NAME"
            val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonText)
            val recipesArray = jsonObject.optJSONArray("recipes") ?: return 0
            var seededCount = 0
            for (i in 0 until recipesArray.length()) {
                val entry = recipesArray.getJSONObject(i)
                val fileName = entry.optString("fileName").takeIf { it.isNotBlank() } ?: continue
                val targetFile = File(recipesDir, fileName)
                if (targetFile.exists()) {
                    continue
                }
                val assetFilePath = "$ASSETS_RECIPES_DIR/$fileName"
                if (copyAssetToFile(assetFilePath, targetFile)) {
                    seededCount += 1
                }
            }
            seededCount
        } catch (_: Exception) {
            -1
        }
    }

    private fun rebuildIndexFromFiles() {
        val entries = listRecipeFiles().mapNotNull { file ->
            val name = try {
                val format = readRecipeFile(file)
                format.recipes.firstOrNull()?.name
            } catch (_: Exception) {
                null
            } ?: return@mapNotNull null
            RecipeIndexEntry(file.name, name)
        }
        saveIndexEntries(entries)
    }

    private fun loadIndexEntries(): List<RecipeIndexEntry> {
        if (!indexFile.exists()) {
            return emptyList()
        }
        return try {
            val jsonText = indexFile.readText(Charsets.UTF_8)
            val jsonObject = JSONObject(jsonText)
            val recipesArray = jsonObject.optJSONArray("recipes") ?: JSONArray()
            val entries = mutableListOf<RecipeIndexEntry>()
            for (i in 0 until recipesArray.length()) {
                val entry = recipesArray.getJSONObject(i)
                val fileName = entry.optString("fileName")
                val name = entry.optString("name")
                if (fileName.isNotBlank() && name.isNotBlank()) {
                    entries.add(RecipeIndexEntry(fileName, name))
                }
            }
            entries
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveIndexEntries(entries: List<RecipeIndexEntry>) {
        if (!recipesDir.exists()) {
            recipesDir.mkdirs()
        }
        val jsonObject = JSONObject().apply {
            put("version", "1.0")
            val array = JSONArray()
            entries.forEach { entry ->
                array.put(
                    JSONObject().apply {
                        put("fileName", entry.fileName)
                        put("name", entry.name)
                    }
                )
            }
            put("recipes", array)
        }
        indexFile.writeText(jsonObject.toString(2), Charsets.UTF_8)
    }

    private fun syncIndexWithFiles(entries: MutableList<RecipeIndexEntry>): Boolean {
        val fileMap = listRecipeFiles().associateBy { it.name }
        var updated = false
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!fileMap.containsKey(entry.fileName)) {
                iterator.remove()
                updated = true
            }
        }
        val existingNames = entries.associateBy { it.fileName }.toMutableMap()
        fileMap.values.forEach { file ->
            val existing = existingNames[file.name]
            val parsedName = try {
                val format = readRecipeFile(file)
                format.recipes.firstOrNull()?.name
            } catch (_: Exception) {
                null
            } ?: file.name
            if (existing == null) {
                entries.add(RecipeIndexEntry(file.name, parsedName))
                updated = true
            } else if (existing.name != parsedName) {
                val index = entries.indexOfFirst { it.fileName == file.name }
                if (index >= 0) {
                    entries[index] = entries[index].copy(name = parsedName)
                    updated = true
                }
            }
        }
        return updated
    }

    private fun ensureIngredientCategories(root: JSONObject): Boolean {
        val recipesArray = root.optJSONArray("recipes") ?: return false
        var updated = false
        for (i in 0 until recipesArray.length()) {
            val recipe = recipesArray.getJSONObject(i)
            val ingredientsArray = recipe.optJSONArray("ingredients") ?: continue
            for (j in 0 until ingredientsArray.length()) {
                val ingredient = ingredientsArray.getJSONObject(j)
                val category = ingredient.optString("category")
                if (category.isBlank()) {
                    ingredient.put("category", DEFAULT_CATEGORY)
                    updated = true
                }
            }
        }
        return updated
    }

    companion object {
        private const val RECIPES_DIR_NAME = "recipes"
        private const val ASSETS_RECIPES_DIR = "recipes_samples"
        private const val INDEX_FILE_NAME = "recipes_index.json"
        private const val DEFAULT_CATEGORY = "Autres"
    }

    data class RecipeIndexEntry(
        val fileName: String,
        val name: String
    )
}
