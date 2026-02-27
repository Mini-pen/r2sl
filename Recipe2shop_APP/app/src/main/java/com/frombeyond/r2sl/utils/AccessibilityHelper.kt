package com.frombeyond.r2sl.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.AppSettingsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Helper class to apply accessibility settings to views throughout the app.
 */
object AccessibilityHelper {
    
    // Cache pour éviter d'appliquer plusieurs fois les mêmes paramètres
    private val appliedViews = mutableSetOf<View>()
    
    /**
     * Applies all accessibility settings to a view and its children recursively.
     * Clears "applied" tags first so that changing settings takes effect without restart.
     */
    fun applyAccessibilitySettings(context: Context, rootView: View) {
        val settingsManager = AppSettingsManager(context)
        val textSize = settingsManager.getInt(AppSettingsManager.KEY_ACCESS_TEXT_SIZE, 3)
        val uiSize = settingsManager.getInt(AppSettingsManager.KEY_ACCESS_UI_SIZE, 3)
        val contrast = settingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_CONTRAST, false)
        val falc = settingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_FALC, false)
        clearAppliedTags(rootView)
        appliedViews.clear()
        applyToView(rootView, textSize, uiSize, contrast, falc)
    }

    /** Clears "applied" tags so settings are re-applied with current values. Keeps original_text_size. */
    private fun clearAppliedTags(view: View) {
        view.setTag(R.id.accessibility_text_size_applied, null)
        view.setTag(R.id.accessibility_ui_size_applied, null)
        view.setTag(R.id.accessibility_contrast_applied, null)
        if (view is ViewGroup) {
            view.children.forEach { clearAppliedTags(it) }
        }
    }
    
    /**
     * Recursively applies accessibility settings to a view and its children.
     */
    private fun applyToView(view: View, textSize: Int, uiSize: Int, contrast: Boolean, falc: Boolean) {
        // Skip if already applied
        if (appliedViews.contains(view)) {
            return
        }
        appliedViews.add(view)
        
        when (view) {
            is TextView -> {
                if (view.getTag(R.id.accessibility_text_size_applied) == null) {
                    applyTextSize(view, textSize)
                    view.setTag(R.id.accessibility_text_size_applied, true)
                }
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToTextView(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
            is Button, is MaterialButton -> {
                val button = view as TextView
                if (button.getTag(R.id.accessibility_text_size_applied) == null) {
                    applyTextSize(button, textSize)
                    button.setTag(R.id.accessibility_text_size_applied, true)
                }
                if (view.getTag(R.id.accessibility_ui_size_applied) == null) {
                    applyUiSize(view, uiSize)
                    view.setTag(R.id.accessibility_ui_size_applied, true)
                }
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToButton(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
            is FloatingActionButton -> {
                if (view.getTag(R.id.accessibility_ui_size_applied) == null) {
                    applyUiSize(view, uiSize)
                    view.setTag(R.id.accessibility_ui_size_applied, true)
                }
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToFAB(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
            is ImageView -> {
                if (view.getTag(R.id.accessibility_ui_size_applied) == null) {
                    applyUiSize(view, uiSize)
                    view.setTag(R.id.accessibility_ui_size_applied, true)
                }
            }
            is CheckBox -> {
                if (view.getTag(R.id.accessibility_text_size_applied) == null) {
                    applyTextSize(view, textSize)
                    view.setTag(R.id.accessibility_text_size_applied, true)
                }
                if (view.getTag(R.id.accessibility_ui_size_applied) == null) {
                    applyUiSize(view, uiSize)
                    view.setTag(R.id.accessibility_ui_size_applied, true)
                }
            }
        }
        
        // Apply to children
        if (view is ViewGroup) {
            view.children.forEach { child ->
                applyToView(child, textSize, uiSize, contrast, falc)
            }
        }
    }
    
    /**
     * Applies text size multiplier to a TextView.
     * textSize: 1-5, where 3 is default (1.0x), 1 is 0.7x, 5 is 1.5x
     * Uses the original text size from XML as base.
     */
    private fun applyTextSize(textView: TextView, textSize: Int) {
        // Get original size from XML if not already modified
        val originalSize = textView.getTag(R.id.accessibility_original_text_size) as? Float
            ?: run {
                val res = textView.context.resources
                val density = res.displayMetrics.density
                val fontScale = res.configuration.fontScale
                val scaledDensity = density * fontScale
                val size = textView.textSize / scaledDensity
                textView.setTag(R.id.accessibility_original_text_size, size)
                size
            }
        
        val multiplier = when (textSize) {
            1 -> 0.7f
            2 -> 0.85f
            3 -> 1.0f
            4 -> 1.25f
            5 -> 1.5f
            else -> 1.0f
        }
        textView.textSize = originalSize * multiplier
    }
    
    /**
     * Applies UI size multiplier to icons and buttons.
     * uiSize: 1-5, where 3 is default (1.0x), 1 is 0.7x, 5 is 1.5x
     * Uses center pivot so scaling does not break icon/text centering.
     */
    private fun applyUiSize(view: View, uiSize: Int) {
        val multiplier = when (uiSize) {
            1 -> 0.7f
            2 -> 0.85f
            3 -> 1.0f
            4 -> 1.25f
            5 -> 1.5f
            else -> 1.0f
        }
        view.pivotX = (view.width / 2).toFloat().takeIf { view.width > 0 } ?: view.pivotX
        view.pivotY = (view.height / 2).toFloat().takeIf { view.height > 0 } ?: view.pivotY
        view.scaleX = multiplier
        view.scaleY = multiplier
    }
    
    /**
     * Applies high contrast to a TextView.
     * Uses very dark text on light background for maximum contrast.
     */
    private fun applyContrastToTextView(textView: TextView) {
        val context = textView.context
        // Use very dark text for high contrast
        textView.setTextColor(android.graphics.Color.rgb(0, 0, 0))
        // Ensure light background if no background is set
        if (textView.background == null) {
            textView.setBackgroundColor(android.graphics.Color.rgb(255, 255, 255))
        }
    }
    
    /**
     * Applies high contrast to a Button.
     */
    private fun applyContrastToButton(button: View) {
        val context = button.context
        // Use darker background for better contrast
        button.setBackgroundColor(context.getColor(android.R.color.black))
        if (button is TextView) {
            button.setTextColor(context.getColor(android.R.color.white))
        }
    }
    
    /**
     * Applies high contrast to a FloatingActionButton.
     */
    private fun applyContrastToFAB(fab: FloatingActionButton) {
        val context = fab.context
        fab.setBackgroundColor(context.getColor(android.R.color.black))
        fab.setColorFilter(context.getColor(android.R.color.white))
    }
    
    /**
     * Checks if FALC mode is enabled.
     */
    fun isFalcEnabled(context: Context): Boolean {
        val settingsManager = AppSettingsManager(context)
        return settingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_FALC, false)
    }
    
    /**
     * Gets the text size multiplier.
     */
    fun getTextSizeMultiplier(context: Context): Float {
        val settingsManager = AppSettingsManager(context)
        val textSize = settingsManager.getInt(AppSettingsManager.KEY_ACCESS_TEXT_SIZE, 3)
        return when (textSize) {
            1 -> 0.7f
            2 -> 0.85f
            3 -> 1.0f
            4 -> 1.25f
            5 -> 1.5f
            else -> 1.0f
        }
    }
    
    /**
     * Gets the UI size multiplier.
     */
    fun getUiSizeMultiplier(context: Context): Float {
        val settingsManager = AppSettingsManager(context)
        val uiSize = settingsManager.getInt(AppSettingsManager.KEY_ACCESS_UI_SIZE, 3)
        return when (uiSize) {
            1 -> 0.7f
            2 -> 0.85f
            3 -> 1.0f
            4 -> 1.25f
            5 -> 1.5f
            else -> 1.0f
        }
    }
    
    /**
     * Checks if high contrast is enabled.
     */
    fun isHighContrastEnabled(context: Context): Boolean {
        val settingsManager = AppSettingsManager(context)
        return settingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_CONTRAST, false)
    }
}