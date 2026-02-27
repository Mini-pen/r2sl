package com.frombeyond.r2sl.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Bienvenue dans Recipe2shoplist\n\nVotre application de gestion de recettes et de listes de courses.\n\n• Créez et gérez vos recettes\n• Générez automatiquement vos listes de courses\n• Planifiez vos menus de la semaine\n• Exportez vos recettes en PDF\n\nCommencez par créer une recette ou consultez votre menu de la semaine."
    }
    val text: LiveData<String> = _text
}