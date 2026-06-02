package com.frombeyond.r2sl.ui.recipes

import com.frombeyond.r2sl.data.local.RecipeImageStorageManager

/**
 * * In-memory photo list while editing a recipe or a single step.
 */
data class RecipePhotoDraft(
    val imageFiles: MutableList<String> = mutableListOf(),
    var primaryImageFile: String? = null
) {
    fun addImage(fileName: String, setAsPrimary: Boolean) {
        if (!imageFiles.contains(fileName)) {
            imageFiles.add(fileName)
        }
        if (setAsPrimary || primaryImageFile == null) {
            primaryImageFile = fileName
        }
    }

    fun removeImage(fileName: String) {
        imageFiles.remove(fileName)
        if (primaryImageFile == fileName) {
            primaryImageFile = imageFiles.firstOrNull()
        }
    }

    fun setPrimary(fileName: String) {
        if (imageFiles.contains(fileName)) {
            primaryImageFile = fileName
        }
    }

    fun primaryRelativePath(): String? {
        val primary = primaryImageFile ?: imageFiles.firstOrNull() ?: return null
        return RecipeImageStorageManager.toRelativePath(primary)
    }

    companion object {
        fun fromRecipe(
            imageFiles: List<String>?,
            primaryImageFile: String?,
            imageUrl: String?
        ): RecipePhotoDraft {
            val files = imageFiles?.toMutableList() ?: mutableListOf()
            val fromUrl = RecipeImageStorageManager.fileNameFromRelativePath(imageUrl)
            if (fromUrl != null && !files.contains(fromUrl)) {
                files.add(0, fromUrl)
            }
            val primary = primaryImageFile?.takeIf { it.isNotBlank() && files.contains(it) }
                ?: fromUrl?.takeIf { files.contains(it) }
                ?: files.firstOrNull()
            return RecipePhotoDraft(files, primary)
        }

        fun fromStep(imageFiles: List<String>?, primaryImageFile: String?): RecipePhotoDraft {
            val files = imageFiles?.toMutableList() ?: mutableListOf()
            val primary = primaryImageFile?.takeIf { it.isNotBlank() && files.contains(it) }
                ?: files.firstOrNull()
            return RecipePhotoDraft(files, primary)
        }
    }
}
