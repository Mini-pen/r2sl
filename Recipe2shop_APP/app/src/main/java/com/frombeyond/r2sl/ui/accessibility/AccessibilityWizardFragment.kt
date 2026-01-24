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
 * Assistant d'accessibilité avec écrans successifs pour chaque question.
 * Réutilise les layouts existants avec navigation par flèches.
 */
class AccessibilityWizardFragment : Fragment() {

    private lateinit var appSettingsManager: AppSettingsManager
    
    private enum class WizardStep {
        TEXT_SIZE,
        UI_SIZE,
        CONTRAST,
        FALC
    }
    
    private var currentStep = WizardStep.TEXT_SIZE
    private var textSize: Int = 3
    private var uiSize: Int = 3
    private var contrast: Boolean = false
    private var falc: Boolean = false
    
    private lateinit var titleText: TextView
    private lateinit var previewText: TextView
    private lateinit var valueText: TextView
    private lateinit var minusButton: MaterialButton
    private lateinit var plusButton: MaterialButton
    private lateinit var prevButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var stepIndicator: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accessibility_wizard, container, false)
        appSettingsManager = AppSettingsManager(requireContext())

        titleText = root.findViewById(R.id.accessibility_scale_title)
        previewText = root.findViewById(R.id.accessibility_scale_preview)
        valueText = root.findViewById(R.id.accessibility_scale_value)
        minusButton = root.findViewById(R.id.button_accessibility_minus)
        plusButton = root.findViewById(R.id.button_accessibility_plus)
        prevButton = root.findViewById(R.id.button_wizard_prev)
        nextButton = root.findViewById(R.id.button_wizard_next)
        stepIndicator = root.findViewById(R.id.wizard_step_indicator)

        // Charger les valeurs existantes
        textSize = appSettingsManager.getInt(AppSettingsManager.KEY_ACCESS_TEXT_SIZE, 3)
        uiSize = appSettingsManager.getInt(AppSettingsManager.KEY_ACCESS_UI_SIZE, 3)
        contrast = appSettingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_CONTRAST, false)
        falc = appSettingsManager.getBoolean(AppSettingsManager.KEY_ACCESS_FALC, false)

        prevButton.setOnClickListener { previousStep() }
        nextButton.setOnClickListener { nextStep() }

        minusButton.setOnClickListener {
            when (currentStep) {
                WizardStep.TEXT_SIZE -> {
                    textSize = (textSize - 1).coerceAtLeast(1)
                    appSettingsManager.saveInt(AppSettingsManager.KEY_ACCESS_TEXT_SIZE, textSize)
                }
                WizardStep.UI_SIZE -> {
                    uiSize = (uiSize - 1).coerceAtLeast(1)
                    appSettingsManager.saveInt(AppSettingsManager.KEY_ACCESS_UI_SIZE, uiSize)
                }
                WizardStep.CONTRAST -> {
                    contrast = false
                    appSettingsManager.saveBoolean(AppSettingsManager.KEY_ACCESS_CONTRAST, contrast)
                }
                WizardStep.FALC -> {
                    falc = false
                    appSettingsManager.saveBoolean(AppSettingsManager.KEY_ACCESS_FALC, falc)
                }
            }
            updateDisplay()
        }

        plusButton.setOnClickListener {
            when (currentStep) {
                WizardStep.TEXT_SIZE -> {
                    textSize = (textSize + 1).coerceAtMost(5)
                    appSettingsManager.saveInt(AppSettingsManager.KEY_ACCESS_TEXT_SIZE, textSize)
                }
                WizardStep.UI_SIZE -> {
                    uiSize = (uiSize + 1).coerceAtMost(5)
                    appSettingsManager.saveInt(AppSettingsManager.KEY_ACCESS_UI_SIZE, uiSize)
                }
                WizardStep.CONTRAST -> {
                    contrast = true
                    appSettingsManager.saveBoolean(AppSettingsManager.KEY_ACCESS_CONTRAST, contrast)
                }
                WizardStep.FALC -> {
                    falc = true
                    appSettingsManager.saveBoolean(AppSettingsManager.KEY_ACCESS_FALC, falc)
                }
            }
            updateDisplay()
        }

        updateDisplay()
        return root
    }

    private fun updateDisplay() {
        when (currentStep) {
            WizardStep.TEXT_SIZE -> {
                titleText.text = getString(R.string.accessibility_text_size)
                previewText.visibility = View.VISIBLE
                valueText.visibility = View.VISIBLE
                valueText.text = getString(R.string.accessibility_scale_value, textSize)
                val size = 16f + (textSize - 1) * 2f
                previewText.textSize = size
                minusButton.visibility = View.VISIBLE
                plusButton.visibility = View.VISIBLE
                minusButton.text = "-"
                plusButton.text = "+"
            }
            WizardStep.UI_SIZE -> {
                titleText.text = getString(R.string.accessibility_ui_size)
                previewText.visibility = View.VISIBLE
                valueText.visibility = View.VISIBLE
                valueText.text = getString(R.string.accessibility_scale_value, uiSize)
                val size = 16f + (uiSize - 1) * 2f
                previewText.textSize = size
                minusButton.visibility = View.VISIBLE
                plusButton.visibility = View.VISIBLE
                minusButton.text = "-"
                plusButton.text = "+"
            }
            WizardStep.CONTRAST -> {
                titleText.text = getString(R.string.accessibility_contrast)
                previewText.visibility = View.GONE
                valueText.visibility = View.GONE
                minusButton.visibility = View.VISIBLE
                plusButton.visibility = View.VISIBLE
                minusButton.text = getString(R.string.accessibility_toggle_disable)
                plusButton.text = getString(R.string.accessibility_toggle_enable)
                minusButton.isEnabled = contrast
                plusButton.isEnabled = !contrast
            }
            WizardStep.FALC -> {
                titleText.text = getString(R.string.accessibility_falc)
                previewText.visibility = View.GONE
                valueText.visibility = View.GONE
                minusButton.visibility = View.VISIBLE
                plusButton.visibility = View.VISIBLE
                minusButton.text = getString(R.string.accessibility_toggle_disable)
                plusButton.text = getString(R.string.accessibility_toggle_enable)
                minusButton.isEnabled = falc
                plusButton.isEnabled = !falc
            }
        }
        
        prevButton.isEnabled = currentStep != WizardStep.TEXT_SIZE
        nextButton.text = if (currentStep == WizardStep.FALC) getString(R.string.accessibility_save) else "→"
        stepIndicator.text = "${currentStep.ordinal + 1}/4"
    }

    private fun previousStep() {
        when (currentStep) {
            WizardStep.UI_SIZE -> currentStep = WizardStep.TEXT_SIZE
            WizardStep.CONTRAST -> currentStep = WizardStep.UI_SIZE
            WizardStep.FALC -> currentStep = WizardStep.CONTRAST
            else -> {}
        }
        updateDisplay()
    }

    private fun nextStep() {
        if (currentStep == WizardStep.FALC) {
            // Terminer l'assistant
            findNavController().popBackStack()
            return
        }
        
        when (currentStep) {
            WizardStep.TEXT_SIZE -> currentStep = WizardStep.UI_SIZE
            WizardStep.UI_SIZE -> currentStep = WizardStep.CONTRAST
            WizardStep.CONTRAST -> currentStep = WizardStep.FALC
            else -> {}
        }
        updateDisplay()
    }
}
