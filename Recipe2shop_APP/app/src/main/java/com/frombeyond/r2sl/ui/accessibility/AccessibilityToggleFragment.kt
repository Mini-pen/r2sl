package com.frombeyond.r2sl.ui.accessibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.AppSettingsManager
import com.google.android.material.button.MaterialButton

/**
 * * Full screen toggle setting (enable/disable + validate).
 */
class AccessibilityToggleFragment : Fragment() {

    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var titleText: TextView
    private lateinit var enableButton: MaterialButton
    private lateinit var disableButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    private var key: String = ""
    private var value: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accessibility_toggle, container, false)
        appSettingsManager = AppSettingsManager(requireContext())

        titleText = root.findViewById(R.id.accessibility_toggle_title)
        enableButton = root.findViewById(R.id.button_accessibility_enable)
        disableButton = root.findViewById(R.id.button_accessibility_disable)

        val args = requireArguments()
        titleText.text = args.getString(ARG_TITLE, "")
        key = args.getString(ARG_KEY, "")
        val defaultValue = args.getBoolean(ARG_DEFAULT, false)
        value = appSettingsManager.getBoolean(key, defaultValue)

        updateSelection()

        enableButton.setOnClickListener {
            value = true
            updateSelection()
            // Sauvegarde automatique
            appSettingsManager.saveBoolean(key, value)
        }

        disableButton.setOnClickListener {
            value = false
            updateSelection()
            // Sauvegarde automatique
            appSettingsManager.saveBoolean(key, value)
        }

        return root
    }

    private fun updateSelection() {
        val context = requireContext()
        val greenColor = ContextCompat.getColor(context, R.color.secondary_green)
        val whiteColor = ContextCompat.getColor(context, R.color.white)
        
        if (value) {
            // Option activée : bouton "enable" en vert (sélectionné), bouton "disable" activé
            enableButton.isEnabled = false // Désactivé car déjà sélectionné
            enableButton.setBackgroundColor(greenColor)
            enableButton.setTextColor(whiteColor)
            
            disableButton.isEnabled = true // Activé pour permettre de changer
            disableButton.backgroundTintList = null // Couleur par défaut du thème
        } else {
            // Option désactivée : bouton "enable" activé, bouton "disable" en vert (sélectionné)
            enableButton.isEnabled = true // Activé pour permettre de changer
            enableButton.backgroundTintList = null // Couleur par défaut du thème
            
            disableButton.isEnabled = false // Désactivé car déjà sélectionné
            disableButton.setBackgroundColor(greenColor)
            disableButton.setTextColor(whiteColor)
        }
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_KEY = "key"
        const val ARG_DEFAULT = "default"
    }
}
