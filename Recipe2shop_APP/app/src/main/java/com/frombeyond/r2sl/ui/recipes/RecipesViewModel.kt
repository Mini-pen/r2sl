package com.frombeyond.r2sl.ui.recipes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * * ViewModel for RecipesFragment.
 */
class RecipesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is recipes fragment"
    }
    val text: LiveData<String> = _text
}
