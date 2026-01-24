package com.frombeyond.r2sl.ui.shoppinglists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * * ViewModel for ShoppingListsFragment.
 */
class ShoppingListsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is shopping lists fragment"
    }
    val text: LiveData<String> = _text
}
