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
     * Neutral default emoji for ingredients with no match in the dictionary.
     * Used as first suggestion so .firstOrNull() never returns a random dict emoji (e.g. egg, corn).
     */
    private val neutralDefaultEmoji = "🍽️"

    /**
     * Returns suggested emojis for an ingredient name.
     *
     * Order of priority:
     * 1) Emojis that match individual words of the ingredient name.
     * 2) If no word matched: neutral default (🍽️) first, then generic options, then rest of dict.
     * 3) Generic / neutral food emojis (🥘, 🛒, etc.) when present in dict.
     * 4) Remaining emojis from the dictionary (egg last) for picker variety only.
     */
    fun getSuggestions(ingredientName: String): List<String> {
        val name = ingredientName.trim().lowercase(Locale.getDefault()).ifBlank { return listOf(neutralDefaultEmoji) }
        val words = name.split(Regex("\\s+")).filter { it.length > 1 }

        // 1) Emojis corresponding to words in the ingredient name
        val fromWords = mutableSetOf<String>()
        words.forEach { word ->
            getEmojiFor(word)?.let { fromWords.add(it) }
        }

        val genericEmojis = listOf("🍽️", "🥘", "🛒")
        val allDistinct = cache.values.distinct()

        // Put egg explicitly last in "rest" so it never appears as default
        val eggEmoji = "🥚"
        val rest = allDistinct
            .filter { it !in fromWords && it !in genericEmojis && it != eggEmoji }
            .toMutableList()
        if (eggEmoji in allDistinct) {
            rest.add(eggEmoji)
        }

        val result = mutableListOf<String>()
        result.addAll(fromWords)

        // When no word matched: neutral default first so display/picker use a neutral emoji, not a random one from dict
        if (fromWords.isEmpty()) {
            result.add(neutralDefaultEmoji)
        }
        genericEmojis.forEach { e ->
            if (e !in result && e in allDistinct) {
                result.add(e)
            }
        }
        rest.forEach { e ->
            if (e !in result) {
                result.add(e)
            }
        }

        return result
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
