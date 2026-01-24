package com.frombeyond.r2sl.ui.weeklymenu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * * ViewModel for WeeklyMenuFragment.
 */
class WeeklyMenuViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is weekly menu fragment"
    }
    val text: LiveData<String> = _text
}
