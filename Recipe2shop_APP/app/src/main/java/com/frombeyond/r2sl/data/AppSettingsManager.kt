package com.frombeyond.r2sl.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Gestionnaire des paramètres de l'application
 * Gère la sauvegarde automatique des préférences utilisateur
 */
class AppSettingsManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "therapia_settings"
        private const val KEY_DEV_FEATURES_ENABLED = "dev_features_enabled"
        private const val TAG = "AppSettingsManager"
        const val KEY_ACCESS_TEXT_SIZE = "access_text_size"
        const val KEY_ACCESS_UI_SIZE = "access_ui_size"
        const val KEY_ACCESS_CONTRAST = "access_contrast" // Boolean: true = fort, false = normal
        const val KEY_ACCESS_FALC = "access_falc"
        // KEY_ACCESS_TTS removed - to be implemented later
        // KEY_ACCESS_REDUCE_ANIMATIONS removed
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Active ou désactive les fonctionnalités pour développeur
     */
    fun setDevFeaturesEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean(KEY_DEV_FEATURES_ENABLED, enabled)
                .apply()
            Log.d(TAG, "Fonctionnalités développeur ${if (enabled) "activées" else "désactivées"}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des paramètres développeur", e)
        }
    }
    
    /**
     * Vérifie si les fonctionnalités pour développeur sont activées
     */
    fun isDevFeaturesEnabled(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_DEV_FEATURES_ENABLED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture des paramètres développeur", e)
            false
        }
    }
    
    /**
     * Sauvegarde automatique d'une valeur booléenne
     */
    fun saveBoolean(key: String, value: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean(key, value)
                .apply()
            Log.d(TAG, "Paramètre sauvegardé: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde du paramètre $key", e)
        }
    }
    
    /**
     * Récupère une valeur booléenne
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            sharedPreferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture du paramètre $key", e)
            defaultValue
        }
    }
    
    /**
     * Sauvegarde automatique d'une valeur string
     */
    fun saveString(key: String, value: String) {
        try {
            sharedPreferences.edit()
                .putString(key, value)
                .apply()
            Log.d(TAG, "Paramètre sauvegardé: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde du paramètre $key", e)
        }
    }
    
    /**
     * Récupère une valeur string
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            sharedPreferences.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture du paramètre $key", e)
            defaultValue
        }
    }

    /**
     * Sauvegarde automatique d'une valeur int
     */
    fun saveInt(key: String, value: Int) {
        try {
            sharedPreferences.edit()
                .putInt(key, value)
                .apply()
            Log.d(TAG, "Paramètre sauvegardé: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde du paramètre $key", e)
        }
    }

    /**
     * Récupère une valeur int
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            sharedPreferences.getInt(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture du paramètre $key", e)
            defaultValue
        }
    }
    
    /**
     * Efface tous les paramètres
     */
    fun clearAllSettings() {
        try {
            sharedPreferences.edit().clear().apply()
            Log.d(TAG, "Tous les paramètres ont été effacés")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'effacement des paramètres", e)
        }
    }
    
    /**
     * Charge les paramètres depuis un fichier spécifique
     */
    fun loadSettingsFromFile(file: java.io.File): Boolean {
        return try {
            if (!file.exists()) {
                Log.w(TAG, "Fichier de paramètres n'existe pas: ${file.absolutePath}")
                return false
            }
            
            val content = file.readText()
            if (content.isBlank()) {
                Log.w(TAG, "Fichier de paramètres vide: ${file.absolutePath}")
                return false
            }
            
            val json = org.json.JSONObject(content)
            
            // Restaurer les paramètres depuis le JSON
            val editor = sharedPreferences.edit()
            
            // Restaurer les fonctionnalités développeur
            val devFeaturesEnabled = json.optBoolean(KEY_DEV_FEATURES_ENABLED, false)
            editor.putBoolean(KEY_DEV_FEATURES_ENABLED, devFeaturesEnabled)
            
            // Restaurer d'autres paramètres si présents
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != KEY_DEV_FEATURES_ENABLED) {
                    val value = json.optString(key, "")
                    editor.putString(key, value)
                }
            }
            
            editor.apply()
            Log.d(TAG, "Paramètres restaurés depuis le fichier: ${file.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement des paramètres depuis le fichier: ${file.absolutePath}", e)
            false
        }
    }
}
