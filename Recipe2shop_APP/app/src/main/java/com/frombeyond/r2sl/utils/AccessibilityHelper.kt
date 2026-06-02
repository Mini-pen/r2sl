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
import kotlin.math.roundToInt

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

    /** Clears "applied" tags so settings are re-applied with current values. Keeps original_* tags. */
    private fun clearAppliedTags(view: View) {
        view.setTag(R.id.accessibility_text_size_applied, null)
        view.setTag(R.id.accessibility_ui_size_applied, null)
        view.setTag(R.id.accessibility_contrast_applied, null)
        view.setTag(R.id.accessibility_margin_applied, null)
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
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToButton(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
            is FloatingActionButton -> {
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToFAB(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
            is ImageView -> {
                if (view.getTag(R.id.accessibility_ui_size_applied) == null) {
                    applyIconSize(view, uiSize)
                    view.setTag(R.id.accessibility_ui_size_applied, true)
                }
            }
            is CheckBox -> {
                if (view.getTag(R.id.accessibility_text_size_applied) == null) {
                    applyTextSize(view, textSize)
                    view.setTag(R.id.accessibility_text_size_applied, true)
                }
                if (contrast && view.getTag(R.id.accessibility_contrast_applied) == null) {
                    applyContrastToTextView(view)
                    view.setTag(R.id.accessibility_contrast_applied, true)
                }
            }
        }
        
        // * Apply margin scale to any view with MarginLayoutParams (spacing between blocks/buttons)
        if (view.layoutParams is ViewGroup.MarginLayoutParams && view.getTag(R.id.accessibility_margin_applied) == null) {
            applyMarginScale(view, uiSize)
            view.setTag(R.id.accessibility_margin_applied, true)
        }

        // Apply to children
        if (view is ViewGroup) {
            view.children.forEach { child ->
                applyToView(child, textSize, uiSize, contrast, falc)
            }
        }
    }

    /**
     * UI size multiplier; 3 = 1.0f, 1 = 0.7f, 5 = 1.5f.
     */
    private fun uiSizeMultiplier(uiSize: Int): Float = when (uiSize) {
        1 -> 0.7f
        2 -> 0.85f
        3 -> 1.0f
        4 -> 1.25f
        5 -> 1.5f
        else -> 1.0f
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
     * Applies UI size to margins (spacing between blocks and buttons).
     * Stores original margins once, then sets margins to original * multiplier so content is not clipped.
     */
    private fun applyMarginScale(view: View, uiSize: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val multiplier = uiSizeMultiplier(uiSize)
        @Suppress("UNCHECKED_CAST")
        val stored = view.getTag(R.id.accessibility_original_margins) as? IntArray
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        if (stored != null && stored.size == 4) {
            left = stored[0]
            top = stored[1]
            right = stored[2]
            bottom = stored[3]
        } else {
            left = params.leftMargin
            top = params.topMargin
            right = params.rightMargin
            bottom = params.bottomMargin
            view.setTag(R.id.accessibility_original_margins, intArrayOf(left, top, right, bottom))
        }
        params.leftMargin = (left * multiplier).roundToInt()
        params.topMargin = (top * multiplier).roundToInt()
        params.rightMargin = (right * multiplier).roundToInt()
        params.bottomMargin = (bottom * multiplier).roundToInt()
        view.requestLayout()
    }

    /**
     * Applies UI size to ImageView icons by setting layout width/height to original size * multiplier.
     * Does not scale the view (avoids clipping); the layout gets more space for the icon.
     * Skips views with MATCH_PARENT so full-width/height images are not changed.
     */
    private fun applyIconSize(view: ImageView, uiSize: Int) {
        val params = view.layoutParams ?: return
        if (params.width == ViewGroup.LayoutParams.MATCH_PARENT || params.height == ViewGroup.LayoutParams.MATCH_PARENT) return
        val multiplier = uiSizeMultiplier(uiSize)
        var origW = view.getTag(R.id.accessibility_original_icon_width) as? Int
        var origH = view.getTag(R.id.accessibility_original_icon_height) as? Int
        if (origW == null || origH == null) {
            origW = when {
                params.width > 0 -> params.width
                view.drawable != null -> view.drawable!!.intrinsicWidth
                view.width > 0 -> view.width
                else -> 48
            }
            origH = when {
                params.height > 0 -> params.height
                view.drawable != null -> view.drawable!!.intrinsicHeight
                view.height > 0 -> view.height
                else -> 48
            }
            view.setTag(R.id.accessibility_original_icon_width, origW)
            view.setTag(R.id.accessibility_original_icon_height, origH)
        }
        params.width = (origW * multiplier).roundToInt()
        params.height = (origH * multiplier).roundToInt()
        view.requestLayout()
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