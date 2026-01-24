package com.frombeyond.r2sl.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * * Stores reusable dishes in a local JSON file.
 */
class DishStorageManager(private val context: Context) {

    fun loadDishes(): List<DishEntry> {
        val file = getFile()
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val array = json.optJSONArray(KEY_DISHES) ?: JSONArray()
            (0 until array.length()).mapNotNull { index ->
                DishEntry.fromJson(array.optJSONObject(index))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDishes(dishes: List<DishEntry>) {
        val file = getFile()
        val array = JSONArray().apply {
            dishes.forEach { put(it.toJson()) }
        }
        val json = JSONObject().apply {
            put(KEY_DISHES, array)
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2), Charsets.UTF_8)
    }

    fun addDishIfMissing(name: String, recipeFileName: String?): DishEntry {
        val dishes = loadDishes().toMutableList()
        val existing = dishes.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            return existing
        }
        val entry = DishEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            source = if (recipeFileName == null) SOURCE_CUSTOM else SOURCE_RECIPE,
            recipeFileName = recipeFileName
        )
        dishes.add(entry)
        saveDishes(dishes)
        return entry
    }

    fun getDishById(id: String): DishEntry? {
        return loadDishes().firstOrNull { it.id == id }
    }

    private fun getFile(): File {
        return File(context.filesDir, FILE_NAME)
    }

    data class DishEntry(
        val id: String,
        val name: String,
        val source: String,
        val recipeFileName: String?
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("source", source)
                recipeFileName?.let { put("recipeFileName", it) }
            }
        }

        companion object {
            fun fromJson(json: JSONObject?): DishEntry? {
                if (json == null) {
                    return null
                }
                return DishEntry(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    source = json.getString("source"),
                    recipeFileName = json.optString("recipeFileName").takeIf { it.isNotEmpty() }
                )
            }
        }
    }

    companion object {
        private const val FILE_NAME = "dishes.json"
        private const val KEY_DISHES = "dishes"
        const val SOURCE_RECIPE = "recipe"
        const val SOURCE_CUSTOM = "custom"
    }
}
