package com.frombeyond.r2sl.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.frombeyond.r2sl.utils.AccessibilityHelper

/**
 * Base fragment that automatically applies accessibility settings.
 * All fragments should extend this class to have accessibility settings applied.
 */
abstract class BaseFragment : Fragment() {
    
    override fun onResume() {
        super.onResume()
        view?.let {
            AccessibilityHelper.applyAccessibilitySettings(requireContext(), it)
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Apply accessibility settings when view is created
        AccessibilityHelper.applyAccessibilitySettings(requireContext(), view)
    }
}