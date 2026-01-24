package com.frombeyond.r2sl.ui.accessibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.AppSettingsManager
import com.google.android.material.button.MaterialButton

/**
 * * Full screen scale setting (plus/minus + validate).
 */
class AccessibilityScaleFragment : Fragment() {

    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var titleText: TextView
    private lateinit var previewText: TextView
    private lateinit var valueText: TextView
    private lateinit var minusButton: MaterialButton
    private lateinit var plusButton: MaterialButton

    private var key: String = ""
    private var minValue: Int = 1
    private var maxValue: Int = 5
    private var stepValue: Int = 1
    private var value: Int = 3

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accessibility_scale, container, false)
        appSettingsManager = AppSettingsManager(requireContext())

        titleText = root.findViewById(R.id.accessibility_scale_title)
        previewText = root.findViewById(R.id.accessibility_scale_preview)
        valueText = root.findViewById(R.id.accessibility_scale_value)
        minusButton = root.findViewById(R.id.button_accessibility_minus)
        plusButton = root.findViewById(R.id.button_accessibility_plus)

        val args = requireArguments()
        titleText.text = args.getString(ARG_TITLE, "")
        key = args.getString(ARG_KEY, "")
        minValue = args.getInt(ARG_MIN, 1)
        maxValue = args.getInt(ARG_MAX, 5)
        stepValue = args.getInt(ARG_STEP, 1)
        val defaultValue = args.getInt(ARG_DEFAULT, 3)
        value = appSettingsManager.getInt(key, defaultValue)

        updatePreview()

        minusButton.setOnClickListener {
            value = (value - stepValue).coerceAtLeast(minValue)
            updatePreview()
            // Sauvegarde automatique
            appSettingsManager.saveInt(key, value)
        }

        plusButton.setOnClickListener {
            value = (value + stepValue).coerceAtMost(maxValue)
            updatePreview()
            // Sauvegarde automatique
            appSettingsManager.saveInt(key, value)
        }

        return root
    }

    private fun updatePreview() {
        valueText.text = getString(R.string.accessibility_scale_value, value)
        val size = 16f + (value - 1) * 2f
        previewText.textSize = size
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_KEY = "key"
        const val ARG_MIN = "min"
        const val ARG_MAX = "max"
        const val ARG_STEP = "step"
        const val ARG_DEFAULT = "default"
    }
}
