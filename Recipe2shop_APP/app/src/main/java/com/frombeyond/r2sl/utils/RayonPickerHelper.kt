package com.frombeyond.r2sl.utils

import android.content.Context
import android.widget.LinearLayout
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.local.RayonsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * * Shows a single-choice dialog to pick a store aisle (rayon) from settings list or add via "Autre".
 */
object RayonPickerHelper {

    fun show(
        context: Context,
        rayonsManager: RayonsManager,
        currentSelection: String?,
        onSelected: (String) -> Unit
    ) {
        val rayons = rayonsManager.getRayons()
        if (rayons.isEmpty()) {
            onSelected(RayonsManager.DEFAULT_CATEGORY)
            return
        }
        val labels = (rayons + RayonsManager.OTHER_LABEL).toTypedArray()
        val checkedIndex = currentSelection?.let { sel ->
            rayons.indexOfFirst { it.equals(sel, ignoreCase = true) }.takeIf { it >= 0 }
        } ?: -1

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.rayon_picker_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                if (which == rayons.size) {
                    dialog.dismiss()
                    promptCustomRayon(context, rayonsManager, onSelected)
                } else {
                    onSelected(rayons[which])
                    dialog.dismiss()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun promptCustomRayon(
        context: Context,
        rayonsManager: RayonsManager,
        onSelected: (String) -> Unit
    ) {
        val inputLayout = TextInputLayout(context).apply {
            hint = context.getString(R.string.rayon_other_hint)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val margin = (16 * context.resources.displayMetrics.density).toInt()
            params.setMargins(margin, margin, margin, 0)
            layoutParams = params
        }
        val input = TextInputEditText(context)
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.rayon_other_title)
            .setView(inputLayout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    rayonsManager.addRayon(name)
                    onSelected(name)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
