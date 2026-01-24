package com.frombeyond.r2sl.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * * Stores shopping lists in a local JSON file.
 */
class ShoppingListStorageManager(private val context: Context) {

    fun loadLists(): List<ShoppingListEntry> {
        val file = getFile()
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val array = json.optJSONArray(KEY_LISTS) ?: JSONArray()
            (0 until array.length()).mapNotNull { index ->
                ShoppingListEntry.fromJson(array.optJSONObject(index))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLists(lists: List<ShoppingListEntry>) {
        val file = getFile()
        val json = JSONObject().apply {
            put(KEY_LISTS, JSONArray().apply {
                lists.forEach { put(it.toJson()) }
            })
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2), Charsets.UTF_8)
    }

    fun createList(
        startDate: String,
        endDate: String,
        items: List<ShoppingListItem>
    ): ShoppingListEntry {
        val list = ShoppingListEntry(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = endDate,
            createdAt = System.currentTimeMillis(),
            items = items
        )
        val lists = loadLists().toMutableList()
        lists.add(list)
        saveLists(lists)
        return list
    }

    fun updateList(entry: ShoppingListEntry) {
        val lists = loadLists().map { if (it.id == entry.id) entry else it }
        saveLists(lists)
    }

    fun getListById(id: String): ShoppingListEntry? {
        return loadLists().firstOrNull { it.id == id }
    }

    fun deleteList(id: String) {
        val lists = loadLists().filter { it.id != id }
        saveLists(lists)
    }

    private fun getFile(): File {
        return File(context.filesDir, FILE_NAME)
    }

    data class ShoppingListEntry(
        val id: String,
        val startDate: String,
        val endDate: String,
        val createdAt: Long,
        val items: List<ShoppingListItem>
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("startDate", startDate)
                put("endDate", endDate)
                put("createdAt", createdAt)
                put("items", JSONArray().apply {
                    items.forEach { put(it.toJson()) }
                })
            }
        }

        companion object {
            fun fromJson(json: JSONObject?): ShoppingListEntry? {
                if (json == null) {
                    return null
                }
                val itemsArray = json.optJSONArray("items") ?: JSONArray()
                val items = (0 until itemsArray.length()).mapNotNull { index ->
                    ShoppingListItem.fromJson(itemsArray.optJSONObject(index))
                }
                return ShoppingListEntry(
                    id = json.getString("id"),
                    startDate = json.getString("startDate"),
                    endDate = json.getString("endDate"),
                    createdAt = json.optLong("createdAt"),
                    items = items
                )
            }
        }
    }

    data class ShoppingListItem(
        val name: String,
        val quantity: Double,
        val unit: String,
        val category: String,
        val checked: Boolean,
        val canceled: Boolean,
        val mealSources: List<MealSource> = emptyList()
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("quantity", quantity)
                put("unit", unit)
                put("category", category)
                put("checked", checked)
                put("canceled", canceled)
                put("mealSources", JSONArray().apply {
                    mealSources.forEach { put(it.toJson()) }
                })
            }
        }

        companion object {
            fun fromJson(json: JSONObject?): ShoppingListItem? {
                if (json == null) {
                    return null
                }
                val mealSourcesArray = json.optJSONArray("mealSources") ?: JSONArray()
                val mealSources = (0 until mealSourcesArray.length()).mapNotNull { index ->
                    MealSource.fromJson(mealSourcesArray.optJSONObject(index))
                }
                return ShoppingListItem(
                    name = json.getString("name"),
                    quantity = json.optDouble("quantity", 0.0),
                    unit = json.optString("unit"),
                    category = json.optString("category"),
                    checked = json.optBoolean("checked", false),
                    canceled = json.optBoolean("canceled", false),
                    mealSources = mealSources
                )
            }
        }
    }

    data class MealSource(
        val date: String,
        val mealType: String,
        val recipeName: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("date", date)
                put("mealType", mealType)
                put("recipeName", recipeName)
            }
        }

        companion object {
            fun fromJson(json: JSONObject?): MealSource? {
                if (json == null) {
                    return null
                }
                return MealSource(
                    date = json.getString("date"),
                    mealType = json.getString("mealType"),
                    recipeName = json.getString("recipeName")
                )
            }
        }
    }

    companion object {
        private const val FILE_NAME = "shopping_lists.json"
        private const val KEY_LISTS = "lists"
    }
}
