package com.frombeyond.r2sl.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.local.RecipeImageStorageManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RecipePhotoThumbnailBinder {

    fun bind(
        fragment: Fragment,
        container: LinearLayout,
        draft: RecipePhotoDraft,
        imageStorage: RecipeImageStorageManager,
        onPrimaryChanged: () -> Unit,
        onPhotosChanged: () -> Unit
    ) {
        container.removeAllViews()
        draft.imageFiles.forEach { fileName ->
            val thumb = LayoutInflater.from(container.context)
                .inflate(R.layout.item_recipe_photo_thumb, container, false)
            val imageView = thumb.findViewById<ImageView>(R.id.photo_thumb_image)
            val badge = thumb.findViewById<TextView>(R.id.photo_thumb_primary_badge)
            val removeButton = thumb.findViewById<MaterialButton>(R.id.photo_thumb_remove)

            val file = imageStorage.getImageFile(fileName)
            Glide.with(fragment).load(file).centerCrop().into(imageView)
            badge.visibility = if (fileName == draft.primaryImageFile) View.VISIBLE else View.GONE

            thumb.setOnClickListener {
                MaterialAlertDialogBuilder(container.context)
                    .setTitle(R.string.recipe_photo_set_primary)
                    .setMessage(fileName)
                    .setPositiveButton(R.string.recipe_photo_primary_prompt_yes) { _, _ ->
                        draft.setPrimary(fileName)
                        onPrimaryChanged()
                        bind(fragment, container, draft, imageStorage, onPrimaryChanged, onPhotosChanged)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            removeButton.setOnClickListener {
                imageStorage.deleteImage(fileName)
                draft.removeImage(fileName)
                onPhotosChanged()
                bind(fragment, container, draft, imageStorage, onPrimaryChanged, onPhotosChanged)
            }
            container.addView(thumb)
        }
    }

    fun loadPrimaryPreview(
        fragment: Fragment,
        preview: ImageView,
        draft: RecipePhotoDraft,
        imageStorage: RecipeImageStorageManager
    ) {
        val primary = draft.primaryImageFile
        if (primary.isNullOrBlank()) {
            preview.visibility = View.GONE
            return
        }
        preview.visibility = View.VISIBLE
        Glide.with(fragment).load(imageStorage.getImageFile(primary)).centerCrop().into(preview)
    }
}
