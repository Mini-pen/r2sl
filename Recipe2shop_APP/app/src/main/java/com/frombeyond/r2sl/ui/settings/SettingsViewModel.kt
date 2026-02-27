package com.frombeyond.r2sl.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Paramètres\n\nConfiguration de l'application Recipe2shoplist :\n\n• Compte utilisateur\n• Préférences de l'application\n• Options d'accessibilité\n• Sauvegarde et restauration\n• À propos de Recipe2shoplist"
    }
    val text: LiveData<String> = _text
}
