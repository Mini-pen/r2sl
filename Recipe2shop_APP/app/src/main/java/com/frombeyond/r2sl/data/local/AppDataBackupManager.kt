package com.frombeyond.r2sl.data.local

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppDataBackupManager(private val context: Context) {

    fun createBackupZip(outputStream: OutputStream): Int {
        val filesDir = context.filesDir
        val recipesDir = File(filesDir, RECIPES_DIR_NAME)
        val zipStream = ZipOutputStream(BufferedOutputStream(outputStream))
        var count = 0
        zipStream.use { zip ->
            val rootFiles = listOf(
                File(filesDir, MENU_FILE_NAME),
                File(filesDir, DISHES_FILE_NAME),
                File(filesDir, SHOPPING_LISTS_FILE_NAME)
            )
            rootFiles.forEach { file ->
                if (file.exists() && file.isFile) {
                    if (addFile(zip, file, file.name)) {
                        count += 1
                    }
                }
            }

            if (recipesDir.exists() && recipesDir.isDirectory) {
                recipesDir.listFiles { file ->
                    file.isFile && file.extension.equals("json", ignoreCase = true)
                }?.forEach { file ->
                    val entryName = "$RECIPES_DIR_NAME/${file.name}"
                    if (addFile(zip, file, entryName)) {
                        count += 1
                    }
                }
            }
        }
        return count
    }

    fun restoreBackupZip(inputStream: InputStream): Int {
        clearAllData()
        val filesDir = context.filesDir
        var count = 0
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val sanitized = sanitizeEntryName(entry.name)
                if (sanitized == null) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                val targetFile = File(filesDir, sanitized)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    BufferedOutputStream(targetFile.outputStream()).use { output ->
                        zip.copyTo(output)
                    }
                    count += 1
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return count
    }

    fun clearAllData(): Int {
        val filesDir = context.filesDir
        var deleted = 0
        val rootFiles = listOf(
            File(filesDir, MENU_FILE_NAME),
            File(filesDir, DISHES_FILE_NAME),
            File(filesDir, SHOPPING_LISTS_FILE_NAME)
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
        private const val MENU_FILE_NAME = "menu_assignments.json"
        private const val DISHES_FILE_NAME = "dishes.json"
        private const val SHOPPING_LISTS_FILE_NAME = "shopping_lists.json"
    }
}
