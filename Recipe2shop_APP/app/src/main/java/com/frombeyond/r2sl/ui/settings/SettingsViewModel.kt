package com.frombeyond.r2sl.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Paramètres\n\nConfiguration de l'application et de Sandrine.AI :\n\n• Profil thérapeute\n• Préférences de l'application\n• Paramètres de sécurité\n• Configuration de Sandrine.AI\n• À propos de TherapIA\n\nFonctionnalité en développement"
    }
    val text: LiveData<String> = _text
}
