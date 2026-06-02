package com.frombeyond.r2sl.ui.recipes

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.frombeyond.r2sl.R
import com.google.android.material.button.MaterialButton

class RecipeImageCropEditorActivity : AppCompatActivity() {

    private lateinit var cropEditorView: RecipeCropEditorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_image_crop)

        cropEditorView = findViewById(R.id.recipe_crop_editor_view)
        val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI)?.let { Uri.parse(it) }
        if (sourceUri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val bitmap = loadBitmap(sourceUri)
        if (bitmap == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        cropEditorView.setImageBitmap(bitmap)

        findViewById<MaterialButton>(R.id.button_crop_cancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        findViewById<MaterialButton>(R.id.button_crop_confirm).setOnClickListener {
            val cropped = cropEditorView.exportCroppedBitmap()
            if (cropped == null) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }
            val tempFile = java.io.File(cacheDir, "crop_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            cropped.recycle()
            val result = Intent().apply {
                putExtra(EXTRA_CROPPED_URI, Uri.fromFile(tempFile).toString())
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (_: Exception) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_CROPPED_URI = "extra_cropped_uri"
    }
}
