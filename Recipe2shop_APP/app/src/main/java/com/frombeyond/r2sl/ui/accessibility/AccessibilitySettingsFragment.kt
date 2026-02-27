package com.frombeyond.r2sl.ui.accessibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.ui.BaseFragment
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.AppSettingsManager
import com.google.android.material.button.MaterialButton

/**
 * * Entry point for accessibility settings.
 */
class AccessibilitySettingsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accessibility_settings, container, false)

        root.findViewById<MaterialButton>(R.id.btn_accessibility_text_size)
            .setOnClickListener {
                navigateToScale(
                    title = getString(R.string.accessibility_text_size),
                    key = AppSettingsManager.KEY_ACCESS_TEXT_SIZE,
                    min = 1,
                    max = 5,
                    step = 1,
                    defaultValue = 3
                )
            }

        root.findViewById<MaterialButton>(R.id.btn_accessibility_ui_size)
            .setOnClickListener {
                navigateToScale(
                    title = getString(R.string.accessibility_ui_size),
                    key = AppSettingsManager.KEY_ACCESS_UI_SIZE,
                    min = 1,
                    max = 5,
                    step = 1,
                    defaultValue = 3
                )
            }

        // Premier bouton : lancer l'assistant
        root.findViewById<MaterialButton>(R.id.btn_accessibility_wizard)
            .setOnClickListener {
                findNavController().navigate(R.id.accessibility_wizard)
            }

        // Autres boutons : raccourcis vers les pages spécifiques
        root.findViewById<MaterialButton>(R.id.btn_accessibility_text_size)
            .setOnClickListener {
                navigateToScale(
                    title = getString(R.string.accessibility_text_size),
                    key = AppSettingsManager.KEY_ACCESS_TEXT_SIZE,
                    min = 1,
                    max = 5,
                    step = 1,
                    defaultValue = 3
                )
            }

        root.findViewById<MaterialButton>(R.id.btn_accessibility_ui_size)
            .setOnClickListener {
                navigateToScale(
                    title = getString(R.string.accessibility_ui_size),
                    key = AppSettingsManager.KEY_ACCESS_UI_SIZE,
                    min = 1,
                    max = 5,
                    step = 1,
                    defaultValue = 3
                )
            }

        root.findViewById<MaterialButton>(R.id.btn_accessibility_contrast)
            .setOnClickListener {
                navigateToToggle(
                    title = getString(R.string.accessibility_contrast),
                    key = AppSettingsManager.KEY_ACCESS_CONTRAST,
                    defaultValue = false
                )
            }

        root.findViewById<MaterialButton>(R.id.btn_accessibility_falc)
            .setOnClickListener {
                navigateToToggle(
                    title = getString(R.string.accessibility_falc),
                    key = AppSettingsManager.KEY_ACCESS_FALC,
                    defaultValue = false
                )
            }

        return root
    }

    private fun navigateToScale(
        title: String,
        key: String,
        min: Int,
        max: Int,
        step: Int,
        defaultValue: Int
    ) {
        val bundle = Bundle().apply {
            putString(AccessibilityScaleFragment.ARG_TITLE, title)
            putString(AccessibilityScaleFragment.ARG_KEY, key)
            putInt(AccessibilityScaleFragment.ARG_MIN, min)
            putInt(AccessibilityScaleFragment.ARG_MAX, max)
            putInt(AccessibilityScaleFragment.ARG_STEP, step)
            putInt(AccessibilityScaleFragment.ARG_DEFAULT, defaultValue)
        }
        findNavController().navigate(R.id.accessibility_scale, bundle)
    }

    private fun navigateToToggle(
        title: String,
        key: String,
        defaultValue: Boolean
    ) {
        val bundle = Bundle().apply {
            putString(AccessibilityToggleFragment.ARG_TITLE, title)
            putString(AccessibilityToggleFragment.ARG_KEY, key)
            putBoolean(AccessibilityToggleFragment.ARG_DEFAULT, defaultValue)
        }
        findNavController().navigate(R.id.accessibility_toggle, bundle)
    }
}
