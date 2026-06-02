package com.frombeyond.r2sl.ui.recipes

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.frombeyond.r2sl.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RecipeCreationLauncher {

    fun show(
        fragment: Fragment,
        navController: NavController,
        addToMealDate: String? = null,
        addToMealType: String? = null,
        openRecipeAfterSave: Boolean = false
    ) {
        val context = fragment.requireContext()
        val options = arrayOf(
            context.getString(R.string.recipe_creation_wizard),
            context.getString(R.string.recipe_creation_full_edit)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.recipe_creation_choice_title)
            .setItems(options) { _, which ->
                val bundle = Bundle().apply {
                    addToMealDate?.let { putString(EditRecipeFragment.ARG_ADD_TO_MEAL_DATE, it) }
                    addToMealType?.let { putString(EditRecipeFragment.ARG_ADD_TO_MEAL_TYPE, it) }
                    if (openRecipeAfterSave) {
                        putBoolean(NewRecipeWizardFragment.ARG_OPEN_RECIPE_AFTER, true)
                    }
                }
                if (which == 0) {
                    navController.navigate(R.id.new_recipe_wizard, bundle)
                } else {
                    navController.navigate(R.id.edit_recipe, bundle)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
