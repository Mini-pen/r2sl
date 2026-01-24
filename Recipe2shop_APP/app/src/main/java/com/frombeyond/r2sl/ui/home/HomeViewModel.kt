package com.frombeyond.r2sl.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Bienvenue dans TherapIA\n\nVotre application de gestion thérapeutique pour accompagner vos patients dans leur parcours de soin.\n\n• Gérez votre liste de patients\n• Planifiez et suivez vos séances\n• Utilisez Sandrine.AI pour des analyses\n• Accédez à votre bibliothèque de ressources\n\nCommencez par consulter votre tableau de bord ou ajouter un nouveau patient."
    }
    val text: LiveData<String> = _text
}