package com.frombeyond.r2sl.data.export

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RecipePackageService(private val context: Context) {

    fun exportRecipePackage(recipe: RecipeJson, outputStream: OutputStream): Int {
        val baseName = sanitizeBaseName(recipe.name)
        val allImageFiles = collectAllImageFileNames(recipe)
        val normalizedRecipe = recipe.copy(
            imageUrl = recipe.primaryRelativePathOrFallback(),
            imageFiles = allImageFiles.takeIf { it.isNotEmpty() }
        )

        var count = 0
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            val jsonEntryName = "$baseName.json"
            val json = RecipeJsonFormat(
                version = "1.1",
                recipes = listOf(normalizedRecipe)
            ).toJsonObject().toString(2)
            zip.putNextEntry(ZipEntry(jsonEntryName))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            count += 1

            allImageFiles.forEach { imageFileName ->
                val source = resolveImageFile(imageFileName)
                if (source != null && source.exists()) {
                    zip.putNextEntry(ZipEntry(imageFileName))
                    source.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    count += 1
                }
            }
        }
        return count
    }

    private fun collectAllImageFileNames(recipe: RecipeJson): List<String> {
        val names = linkedSetOf<String>()
        recipe.imageFiles?.forEach { names.add(it) }
        primaryFileNameFromUrl(recipe.imageUrl)?.let { names.add(it) }
        recipe.steps.forEach { step ->
            step.imageFiles?.forEach { names.add(it) }
        }
        return names.toList()
    }

    private fun resolveImageFile(fileName: String): File? {
        val filesDir = context.filesDir
        val relative = fileName.removePrefix("recipes/images/")
        val file = File(filesDir, "recipes/images/$relative")
        if (file.exists()) return file
        val direct = File(filesDir, fileName)
        if (direct.exists()) return direct
        return null
    }

    private fun RecipeJson.primaryRelativePathOrFallback(): String? {
        val primary = primaryImageFile ?: imageFiles?.firstOrNull() ?: primaryFileNameFromUrl(imageUrl)
        return primary?.let { "recipes/images/${it.removePrefix("recipes/images/")}" }
    }

    companion object {
        fun primaryFileNameFromUrl(imageUrl: String?): String? {
            if (imageUrl.isNullOrBlank()) return null
            return imageUrl.removePrefix("recipes/images/").removePrefix("/").substringAfterLast('/')
                .takeIf { it.isNotBlank() }
        }

        private fun sanitizeBaseName(name: String): String {
            val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            val withoutAccents = normalized.replace("\\p{Mn}+".toRegex(), "")
            val slug = withoutAccents.replace("[^A-Za-z0-9]+".toRegex(), "_")
                .trim('_')
                .lowercase(Locale.ROOT)
            return if (slug.isBlank()) "recette" else slug
        }
    }
}
