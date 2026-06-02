package com.frombeyond.r2sl.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale

class RecipeImageStorageManager(private val context: Context) {

    private val imagesDir: File by lazy {
        File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
    }

    fun getImageFile(fileName: String): File = File(imagesDir, fileName)

    fun nextImageFileName(recipeId: String, recipeName: String): String {
        val base = sanitizeBaseName(recipeName.ifBlank { recipeId })
        var index = 1
        var candidate: String
        do {
            candidate = "${base}_img_${index.toString().padStart(3, '0')}.jpg"
            index++
        } while (getImageFile(candidate).exists())
        return candidate
    }

    fun saveBitmap(fileName: String, bitmap: Bitmap, quality: Int = JPEG_QUALITY): File {
        val target = getImageFile(fileName)
        FileOutputStream(target).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
        return target
    }

    fun loadBitmap(fileName: String): Bitmap? {
        val file = getImageFile(fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun deleteImage(fileName: String): Boolean {
        val file = getImageFile(fileName)
        return !file.exists() || file.delete()
    }

    companion object {
        const val IMAGES_DIR = "recipes/images"
        const val OUTPUT_WIDTH = 800
        const val OUTPUT_HEIGHT = 600
        const val INNER_GUIDE_WIDTH = 600
        private const val JPEG_QUALITY = 90

        fun toRelativePath(fileName: String): String = "$IMAGES_DIR/$fileName"

        fun fileNameFromRelativePath(relativePath: String?): String? {
            if (relativePath.isNullOrBlank()) return null
            return relativePath.removePrefix("$IMAGES_DIR/").removePrefix("/")
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
