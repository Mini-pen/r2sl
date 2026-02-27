package com.frombeyond.r2sl.data.local

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * * Manages a dictionary mapping ingredient names (or words) to emojis.
 * Dictionary is stored as JSON and can be loaded from assets (default) or imported.
 */
class IngredientEmojiManager(private val context: Context) {

    private val dictFile: File by lazy {
        File(context.filesDir, DICT_FILE_NAME)
    }

    private var cache: MutableMap<String, String> = mutableMapOf()

    init {
        load()
    }

    fun load() {
        cache.clear()
        if (!dictFile.exists()) return
        try {
            val json = JSONObject(dictFile.readText(Charsets.UTF_8))
            json.keys().forEach { key ->
                cache[key] = json.optString(key, "")
            }
        } catch (_: Exception) { }
    }

    fun save() {
        dictFile.parentFile?.mkdirs()
        val json = JSONObject()
        cache.forEach { (k, v) -> json.put(k, v) }
        dictFile.writeText(json.toString(2), Charsets.UTF_8)
    }

    /** Returns all entries: key -> emoji */
    fun getAll(): Map<String, String> = cache.toMap()

    /** Sets or removes mapping for a key */
    fun set(key: String, emoji: String?) {
        val k = key.trim().lowercase(Locale.getDefault()).ifBlank { return }
        if (emoji.isNullOrBlank()) cache.remove(k) else cache[k] = emoji
    }

    /** Gets emoji for an exact key (normalized) */
    fun getEmojiFor(key: String): String? = cache[key.trim().lowercase(Locale.getDefault())]

    /**
     * Returns suggested emojis for an ingredient name: first emojis matching words in the name,
     * then all emojis from the dictionary (without duplicates).
     */
    fun getSuggestions(ingredientName: String): List<String> {
        val name = ingredientName.trim().lowercase(Locale.getDefault()).ifBlank { return emptyList() }
        val words = name.split(Regex("\\s+")).filter { it.length > 1 }
        val fromWords = mutableSetOf<String>()
        words.forEach { word ->
            getEmojiFor(word)?.let { fromWords.add(it) }
        }
        val rest = cache.values.filter { it !in fromWords }.distinct()
        return fromWords.toList() + rest
    }

    /** Loads default dictionary from assets (default_emoji_dict.json) and merges with current */
    fun loadDefaultFromAssets(): Int {
        var count = 0
        try {
            context.assets.open(DEFAULT_ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JSONObject(reader.readText())
                json.keys().forEach { key ->
                    val emoji = json.optString(key, "")
                    if (emoji.isNotEmpty()) {
                        cache[key] = emoji
                        count++
                    }
                }
            }
            save()
        } catch (_: Exception) { }
        return count
    }

    /** Exports current dictionary as JSON string */
    fun exportToJson(): String {
        val json = JSONObject()
        cache.forEach { (k, v) -> json.put(k, v) }
        return json.toString(2)
    }

    /** Imports from JSON string; merges with existing (keys are overwritten) */
    fun importFromJson(jsonString: String): Int {
        var count = 0
        try {
            val json = JSONObject(jsonString)
            json.keys().forEach { key ->
                val emoji = json.optString(key, "")
                if (emoji.isNotEmpty()) {
                    cache[key] = emoji
                    count++
                }
            }
            save()
        } catch (_: Exception) { }
        return count
    }

    companion object {
        private const val DICT_FILE_NAME = "ingredient_emoji_dict.json"
        private const val DEFAULT_ASSET_NAME = "default_emoji_dict.json"
    }
}
