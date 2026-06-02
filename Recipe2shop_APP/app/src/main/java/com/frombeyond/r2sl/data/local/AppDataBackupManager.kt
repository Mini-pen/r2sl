package com.frombeyond.r2sl.data.local

import android.content.Context
import com.frombeyond.r2sl.data.AppSettingsManager
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppDataBackupManager(private val context: Context) {
    private val appSettingsManager = AppSettingsManager(context)

    fun createBackupZip(outputStream: OutputStream): Int {
        val payload = buildBackupPayload()
        return writePayloadAsZip(payload, outputStream)
    }

    fun restoreBackupZip(inputStream: InputStream): Int {
        return restoreBackupZip(inputStream, ImportStrategy.overwriteAll())
    }

    fun restoreBackupZip(inputStream: InputStream, strategy: ImportStrategy): Int {
        val entries = readZipEntries(inputStream)
        if (entries.isEmpty()) {
            return -1
        }
        val hasManifest = entries.containsKey(MANIFEST_FILE_NAME)
        return if (hasManifest) {
            restoreV2(entries, strategy)
        } else {
            if (strategy.overwriteAll) {
                restoreLegacy(entries)
            } else {
                restoreLegacy(entries, strategy)
            }
        }
    }

    fun previewBackup(inputStream: InputStream): BackupPreview? {
        val entries = readZipEntries(inputStream)
        if (entries.isEmpty()) {
            return null
        }
        val hasManifest = entries.containsKey(MANIFEST_FILE_NAME)
        val meta = if (hasManifest) {
            parseManifest(entries[MANIFEST_FILE_NAME] ?: return null)
        } else null
        return BackupPreview(
            hasManifest = hasManifest,
            exportedAt = meta?.exportedAt ?: 0L,
            data = collectIncomingData(entries, hasManifest)
        )
    }

    fun hasExistingData(): Boolean {
        val filesDir = context.filesDir
        val rootFiles = listOf(
            File(filesDir, MENU_FILE_NAME),
            File(filesDir, DISHES_FILE_NAME),
            File(filesDir, SHOPPING_LISTS_FILE_NAME),
            File(filesDir, SETTINGS_FILE_NAME),
            File(filesDir, PROFILE_FILE_NAME)
        )
        val hasRootFiles = rootFiles.any { it.exists() && it.isFile && it.length() > 0 }
        val recipesDir = File(filesDir, RECIPES_DIR_NAME)
        val hasRecipes = recipesDir.exists() && (recipesDir.listFiles()?.isNotEmpty() == true)
        return hasRootFiles || hasRecipes || appSettingsManager.getLastUpdatedAt() > 0L
    }

    fun clearAllData(): Int {
        val filesDir = context.filesDir
        var deleted = 0
        val rootFiles = listOf(
            File(filesDir, MENU_FILE_NAME),
            File(filesDir, DISHES_FILE_NAME),
            File(filesDir, SHOPPING_LISTS_FILE_NAME),
            File(filesDir, SETTINGS_FILE_NAME),
            File(filesDir, PROFILE_FILE_NAME)
        )
        rootFiles.forEach { file ->
            if (file.exists() && file.delete()) {
                deleted += 1
            }
        }
        val recipesDir = File(filesDir, RECIPES_DIR_NAME)
        if (recipesDir.exists()) {
            recipesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) {
                    deleted += 1
                }
            }
            if (recipesDir.delete()) {
                deleted += 1
            }
        }
        return deleted
    }

    private fun buildBackupPayload(): BackupPayload {
        val filesDir = context.filesDir
        val payload = mutableMapOf<String, ByteArray>()
        var filesCount = 0

        fun addFileIfExists(source: File, targetPath: String) {
            if (source.exists() && source.isFile) {
                payload[targetPath] = source.readBytes()
                filesCount += 1
            }
        }

        addFileIfExists(File(filesDir, MENU_FILE_NAME), "data/$MENU_FILE_NAME")
        addFileIfExists(File(filesDir, DISHES_FILE_NAME), "data/$DISHES_FILE_NAME")
        addFileIfExists(File(filesDir, SHOPPING_LISTS_FILE_NAME), "data/$SHOPPING_LISTS_FILE_NAME")
        addFileIfExists(File(filesDir, PROFILE_FILE_NAME), "data/$PROFILE_FILE_NAME")
        addFileIfExists(File(filesDir, RAYONS_FILE_NAME), "data/$RAYONS_FILE_NAME")

        val configJson = appSettingsManager.exportSettingsAsJson().toString(2).toByteArray(Charsets.UTF_8)
        payload["data/$SETTINGS_FILE_NAME"] = configJson
        filesCount += 1

        val recipesDir = File(filesDir, RECIPES_DIR_NAME)
        if (recipesDir.exists()) {
            recipesDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val rel = file.relativeTo(filesDir).invariantSeparatorsPath
                    payload[rel] = file.readBytes()
                    filesCount += 1
                }
            }
        }

        val manifest = JSONObject().apply {
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("filesCount", filesCount)
            put("files", JSONArray().apply { payload.keys.sorted().forEach { put(it) } })
        }
        payload[MANIFEST_FILE_NAME] = manifest.toString(2).toByteArray(Charsets.UTF_8)
        return BackupPayload(payload)
    }

    private fun writePayloadAsZip(payload: BackupPayload, outputStream: OutputStream): Int {
        var count = 0
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            payload.files.entries.sortedBy { it.key }.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
                count += 1
            }
        }
        return count
    }

    private fun readZipEntries(inputStream: InputStream): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val sanitized = sanitizeEntryName(entry.name)
                if (sanitized != null && !entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    zip.copyTo(buffer)
                    entries[sanitized] = buffer.toByteArray()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun restoreV2(entries: Map<String, ByteArray>, strategy: ImportStrategy): Int {
        parseManifest(entries[MANIFEST_FILE_NAME] ?: return -1)
        val filesDir = context.filesDir
        var restored = 0
        if (strategy.overwriteAll) {
            clearAllData()
            entries.forEach { (path, bytes) ->
                if (path == MANIFEST_FILE_NAME) return@forEach
                val target = resolveTargetFile(path, filesDir) ?: return@forEach
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
                restored += 1
            }
            return restored
        }

        val incoming = collectIncomingData(entries, true)
        val existing = collectExistingData()
        val recipesResult = mergeRecipes(entries, strategy, filesDir)
        restored += recipesResult
        if (shouldUseFile(strategy, CATEGORY_MENUS, incoming.menuUpdatedAt, existing.menuUpdatedAt)) {
            writeIfPresent(entries, "data/$MENU_FILE_NAME", File(filesDir, MENU_FILE_NAME))?.also { restored += 1 }
        }
        if (shouldUseFile(strategy, CATEGORY_SHOPPING_LISTS, incoming.shoppingListsUpdatedAt, existing.shoppingListsUpdatedAt)) {
            writeIfPresent(entries, "data/$SHOPPING_LISTS_FILE_NAME", File(filesDir, SHOPPING_LISTS_FILE_NAME))?.also { restored += 1 }
        }
        if (shouldUseFile(strategy, CATEGORY_DISHES, incoming.dishesUpdatedAt, existing.dishesUpdatedAt)) {
            writeIfPresent(entries, "data/$DISHES_FILE_NAME", File(filesDir, DISHES_FILE_NAME))?.also { restored += 1 }
        }
        if (shouldUseFile(strategy, CATEGORY_USER, incoming.profileUpdatedAt, existing.profileUpdatedAt)) {
            writeIfPresent(entries, "data/$PROFILE_FILE_NAME", File(filesDir, PROFILE_FILE_NAME))?.also { restored += 1 }
        }
        if (shouldUseFile(strategy, CATEGORY_CONFIG, incoming.configUpdatedAt, existing.configUpdatedAt)) {
            entries["data/$SETTINGS_FILE_NAME"]?.let { bytes ->
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                if (appSettingsManager.importSettingsFromJson(json)) {
                    restored += 1
                    File(filesDir, SETTINGS_FILE_NAME).writeText(json.toString(2), Charsets.UTF_8)
                }
            }
            writeIfPresent(entries, "data/$RAYONS_FILE_NAME", File(filesDir, RAYONS_FILE_NAME))?.also { restored += 1 }
        }
        return restored
    }

    private fun restoreLegacy(entries: Map<String, ByteArray>, strategy: ImportStrategy? = null): Int {
        if (strategy?.overwriteAll != false) {
            clearAllData()
        }
        val filesDir = context.filesDir
        var restored = 0
        entries.forEach { (path, bytes) ->
            val target = resolveTargetFile(path, filesDir) ?: return@forEach
            if (strategy != null && !strategy.overwriteAll && target.exists()) {
                return@forEach
            }
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            restored += 1
        }
        return restored
    }

    private fun mergeRecipes(entries: Map<String, ByteArray>, strategy: ImportStrategy, filesDir: File): Int {
        val recipeEntries = entries.keys.filter { it.startsWith("$RECIPES_DIR_NAME/") && !it.contains("/images/") }
        var restored = 0
        val localManager = RecipesLocalFileManager(context)
        recipeEntries.forEach { path ->
            val fileName = path.removePrefix("$RECIPES_DIR_NAME/")
            if (fileName.isBlank()) return@forEach
            val target = File(filesDir, "$RECIPES_DIR_NAME/$fileName")
            val incomingBytes = entries[path] ?: return@forEach
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                target.writeBytes(incomingBytes)
                restored += 1
                restoreRecipeImages(path, entries, filesDir)
                return@forEach
            }
            val incomingIdentity = extractRecipeIdentity(incomingBytes)
            val localFile = localManager.getRecipeFile(fileName)
            val localIdentity = localFile?.let { extractRecipeIdentity(it.readBytes()) }
            val conflict = RecipeConflict(incomingIdentity?.id, localIdentity?.id ?: "", localIdentity?.name ?: fileName)
            val preferIncoming = strategy.resolveRecipeConflict(conflict)
            if (preferIncoming) {
                target.writeBytes(incomingBytes)
                restoreRecipeImages(path, entries, filesDir)
                restored += 1
            }
        }
        return restored
    }

    private fun restoreRecipeImages(recipePath: String, entries: Map<String, ByteArray>, filesDir: File) {
        val jsonBytes = entries[recipePath] ?: return
        val identity = extractRecipeIdentity(jsonBytes) ?: return
        val imageNames = identity.imageFiles
        imageNames.forEach { imageName ->
            val sourcePath = "$RECIPES_IMAGES_DIR/$imageName"
            val bytes = entries[sourcePath] ?: return@forEach
            val target = File(filesDir, sourcePath)
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
        }
    }

    private fun extractRecipeIdentity(bytes: ByteArray): RecipeIdentity? {
        return try {
            val root = JSONObject(String(bytes, Charsets.UTF_8))
            val recipes = root.optJSONArray("recipes") ?: return null
            if (recipes.length() == 0) return null
            val recipe = recipes.getJSONObject(0)
            val metadata = recipe.optJSONObject("metadata")
            val imageFiles = recipe.optJSONArray("imageFiles")?.let { arr ->
                (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
            } ?: emptyList()
            RecipeIdentity(
                id = recipe.optString("id", ""),
                name = recipe.optString("name", ""),
                updatedAt = metadata?.optLong("updatedAt", 0L) ?: 0L,
                imageFiles = imageFiles
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun writeIfPresent(entries: Map<String, ByteArray>, path: String, target: File): File? {
        val bytes = entries[path] ?: return null
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
        return target
    }

    private fun shouldUseFile(strategy: ImportStrategy, category: String, incomingUpdatedAt: Long, existingUpdatedAt: Long): Boolean {
        return strategy.overwriteAll || strategy.categoryChoice(category, incomingUpdatedAt, existingUpdatedAt)
    }

    private fun collectExistingData(): DataTimestamps {
        val filesDir = context.filesDir
        return DataTimestamps(
            menuUpdatedAt = readUpdatedAtFromFile(File(filesDir, MENU_FILE_NAME)),
            dishesUpdatedAt = readUpdatedAtFromFile(File(filesDir, DISHES_FILE_NAME)),
            shoppingListsUpdatedAt = readUpdatedAtFromFile(File(filesDir, SHOPPING_LISTS_FILE_NAME)),
            profileUpdatedAt = readUpdatedAtFromFile(File(filesDir, PROFILE_FILE_NAME)),
            configUpdatedAt = appSettingsManager.getLastUpdatedAt()
        )
    }

    private fun collectIncomingData(entries: Map<String, ByteArray>, hasManifest: Boolean): DataTimestamps {
        val prefix = if (hasManifest) "data/" else ""
        return DataTimestamps(
            menuUpdatedAt = readUpdatedAtFromBytes(entries["$prefix$MENU_FILE_NAME"]),
            dishesUpdatedAt = readUpdatedAtFromBytes(entries["$prefix$DISHES_FILE_NAME"]),
            shoppingListsUpdatedAt = readUpdatedAtFromBytes(entries["$prefix$SHOPPING_LISTS_FILE_NAME"]),
            profileUpdatedAt = readUpdatedAtFromBytes(entries["$prefix$PROFILE_FILE_NAME"]),
            configUpdatedAt = readUpdatedAtFromBytes(entries["$prefix$SETTINGS_FILE_NAME"])
        )
    }

    private fun readUpdatedAtFromFile(file: File): Long {
        if (!file.exists()) return 0L
        return readUpdatedAtFromBytes(file.readBytes())
    }

    private fun readUpdatedAtFromBytes(bytes: ByteArray?): Long {
        if (bytes == null) return 0L
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            json.optLong("updatedAt", json.optLong("settings_updated_at", 0L))
        } catch (_: Exception) {
            0L
        }
    }

    private fun resolveTargetFile(path: String, filesDir: File): File? {
        val normalized = path.removePrefix("data/")
        val allowed = normalized == MENU_FILE_NAME ||
            normalized == DISHES_FILE_NAME ||
            normalized == SHOPPING_LISTS_FILE_NAME ||
            normalized == PROFILE_FILE_NAME ||
            normalized == SETTINGS_FILE_NAME ||
            normalized == RAYONS_FILE_NAME ||
            normalized.startsWith("$RECIPES_DIR_NAME/")
        if (!allowed) return null
        return File(filesDir, normalized)
    }

    private fun parseManifest(bytes: ByteArray): BackupManifest {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return BackupManifest(
            formatVersion = json.optString("formatVersion", "1.0"),
            exportedAt = json.optLong("exportedAt", 0L)
        )
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String): Boolean {
        return try {
            zip.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { input ->
                input.copyTo(zip)
            }
            zip.closeEntry()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sanitizeEntryName(name: String): String? {
        val clean = name.replace("\\", "/")
        if (clean.contains("..") || clean.startsWith("/")) {
            return null
        }
        return clean
    }

    companion object {
        private const val RECIPES_DIR_NAME = "recipes"
        private const val RECIPES_IMAGES_DIR = "recipes/images"
        private const val MANIFEST_FILE_NAME = "backup_manifest.json"
        private const val BACKUP_FORMAT_VERSION = "2.0"
        private const val MENU_FILE_NAME = "menu_assignments.json"
        private const val DISHES_FILE_NAME = "dishes.json"
        private const val SHOPPING_LISTS_FILE_NAME = "shopping_lists.json"
        private const val SETTINGS_FILE_NAME = "config.json"
        private const val PROFILE_FILE_NAME = "therapist_profile.json"
        private const val RAYONS_FILE_NAME = "rayons_list.json"
        const val CATEGORY_USER = "user"
        const val CATEGORY_CONFIG = "config"
        const val CATEGORY_RECIPES = "recipes"
        const val CATEGORY_MENUS = "menus"
        const val CATEGORY_SHOPPING_LISTS = "shopping_lists"
        const val CATEGORY_DISHES = "dishes"
    }

    data class BackupPreview(
        val hasManifest: Boolean,
        val exportedAt: Long,
        val data: DataTimestamps
    )

    data class DataTimestamps(
        val menuUpdatedAt: Long = 0L,
        val dishesUpdatedAt: Long = 0L,
        val shoppingListsUpdatedAt: Long = 0L,
        val profileUpdatedAt: Long = 0L,
        val configUpdatedAt: Long = 0L
    )

    data class RecipeConflict(
        val incomingRecipeId: String?,
        val localRecipeId: String,
        val recipeName: String
    )

    class ImportStrategy(
        val overwriteAll: Boolean,
        private val categoryResolver: (String, Long, Long) -> Boolean,
        private val recipeResolver: (RecipeConflict) -> Boolean
    ) {
        fun categoryChoice(category: String, incomingUpdatedAt: Long, localUpdatedAt: Long): Boolean {
            return categoryResolver(category, incomingUpdatedAt, localUpdatedAt)
        }

        fun resolveRecipeConflict(conflict: RecipeConflict): Boolean {
            return recipeResolver(conflict)
        }

        companion object {
            fun overwriteAll(): ImportStrategy {
                return ImportStrategy(true, { _, _, _ -> true }, { true })
            }
        }
    }

    private data class BackupPayload(val files: Map<String, ByteArray>)
    private data class BackupManifest(val formatVersion: String, val exportedAt: Long)
    private data class RecipeIdentity(
        val id: String,
        val name: String,
        val updatedAt: Long,
        val imageFiles: List<String>
    )
}
