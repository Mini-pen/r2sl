package com.frombeyond.r2sl.data.local

import android.content.Context
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * * Manages the predefined list of store aisles (rayons) for ingredient categories.
 * List is stored as JSON and can be edited in Settings; "Autre" allows adding a rayon on the fly.
 */
class RayonsManager(private val context: Context) {

    private val file: File by lazy {
        File(context.filesDir, FILE_NAME)
    }

    private var cache: MutableList<String> = mutableListOf()

    init {
        load()
    }

    fun getRayons(): List<String> = cache.toList()

    fun addRayon(name: String) {
        val trimmed = name.trim().ifBlank { return }
        if (cache.any { it.equals(trimmed, ignoreCase = true) }) return
        cache.add(trimmed)
        save()
    }

    fun removeRayon(name: String) {
        cache.removeAll { it.equals(name, ignoreCase = true) }
        save()
    }

    /** Reassigns all ingredients with oldRayon to newRayon across all recipe files */
    fun reassignInAllRecipes(oldRayon: String, newRayon: String): Int {
        val recipesDir = File(context.filesDir, RECIPES_DIR_NAME)
        if (!recipesDir.exists()) return 0
        var count = 0
        recipesDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }?.forEach { file ->
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val recipes = json.optJSONArray("recipes") ?: return@forEach
                var modified = false
                for (i in 0 until recipes.length()) {
                    val recipe = recipes.getJSONObject(i)
                    val ingredients = recipe.optJSONArray("ingredients") ?: continue
                    for (j in 0 until ingredients.length()) {
                        val ing = ingredients.getJSONObject(j)
                        val cat = ing.optString("category", "").trim()
                        if (cat.equals(oldRayon, ignoreCase = true)) {
                            ing.put("category", newRayon)
                            modified = true
                            count++
                        }
                    }
                }
                if (modified) file.writeText(json.toString(2), Charsets.UTF_8)
            } catch (_: Exception) { }
        }
        return count
    }

    /** Collects all distinct ingredient categories from recipe files and merges into the rayons list */
    fun loadFromRecipes(recipesFileManager: RecipesLocalFileManager): Int {
        val existing = cache.toSet()
        val fromRecipes = mutableSetOf<String>()
        recipesFileManager.listRecipeFiles().forEach { file ->
            try {
                val format = recipesFileManager.readRecipeFile(file)
                format.recipes.forEach { recipe ->
                    recipe.ingredients.forEach { ing ->
                        val cat = ing.category.trim().ifBlank { DEFAULT_CATEGORY }
                        fromRecipes.add(cat)
                    }
                }
            } catch (_: Exception) { }
        }
        val added = fromRecipes.filter { it !in existing }
        added.forEach { cache.add(it) }
        cache.sortWith(String.CASE_INSENSITIVE_ORDER)
        save()
        return added.size
    }

    private fun load() {
        cache.clear()
        if (!file.exists()) {
            seedDefault()
            return
        }
        try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val arr = json.optJSONArray(KEY_RAYONS) ?: JSONArray()
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotEmpty()) cache.add(s)
            }
            cache.sortWith(String.CASE_INSENSITIVE_ORDER)
        } catch (_: Exception) {
            seedDefault()
        }
    }

    private fun save() {
        file.parentFile?.mkdirs()
        val json = JSONObject().put(KEY_RAYONS, JSONArray().apply {
            cache.forEach { put(it) }
        })
        file.writeText(json.toString(2), Charsets.UTF_8)
    }

    private fun seedDefault() {
        cache.clear()
        cache.addAll(DEFAULT_RAYONS)
        cache.sortWith(String.CASE_INSENSITIVE_ORDER)
        save()
    }

    companion object {
        private const val FILE_NAME = "rayons_list.json"
        private const val KEY_RAYONS = "rayons"
        const val DEFAULT_CATEGORY = "Autres"
        const val OTHER_LABEL = "Autre"
        private const val RECIPES_DIR_NAME = "recipes"

        private val DEFAULT_RAYONS = listOf(
            "Autres",
            "Boulangerie",
            "Crèmerie",
            "Épicerie salée",
            "Épicerie sucrée",
            "Fruits et légumes",
            "Poissonnerie",
            "Boucherie",
            "Boissons",
            "Surgelés",
            "Conserves",
            "Huiles et condiments"
        )
    }
}
