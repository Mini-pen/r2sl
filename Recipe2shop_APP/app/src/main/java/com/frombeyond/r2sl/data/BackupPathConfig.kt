package com.frombeyond.r2sl.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration des chemins de sauvegarde Google Drive
 * Permet de personnaliser la structure des dossiers de sauvegarde
 */
class BackupPathConfig(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "backup_path_config"
        private const val KEY_ROOT_FOLDER = "root_folder"
        private const val KEY_PROFILE_FOLDER = "profile_folder"
        private const val KEY_CONFIG_FOLDER = "config_folder"
        private const val KEY_LOGS_FOLDER = "logs_folder"
        private const val KEY_USE_TIMESTAMP = "use_timestamp"
        private const val KEY_TIMESTAMP_FORMAT = "timestamp_format"
        
        // Chemins par défaut
        private const val DEFAULT_ROOT_FOLDER = "TherapIA"
        private const val DEFAULT_PROFILE_FOLDER = "Profile"
        private const val DEFAULT_CONFIG_FOLDER = "Config"
        private const val DEFAULT_LOGS_FOLDER = "Logs"
        private const val DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Dossier racine de sauvegarde
     */
    var rootFolder: String
        get() = prefs.getString(KEY_ROOT_FOLDER, DEFAULT_ROOT_FOLDER) ?: DEFAULT_ROOT_FOLDER
        set(value) = prefs.edit().putString(KEY_ROOT_FOLDER, value).apply()
    
    /**
     * Dossier pour les profils
     */
    var profileFolder: String
        get() = prefs.getString(KEY_PROFILE_FOLDER, DEFAULT_PROFILE_FOLDER) ?: DEFAULT_PROFILE_FOLDER
        set(value) = prefs.edit().putString(KEY_PROFILE_FOLDER, value).apply()
    
    /**
     * Dossier pour les configurations
     */
    var configFolder: String
        get() = prefs.getString(KEY_CONFIG_FOLDER, DEFAULT_CONFIG_FOLDER) ?: DEFAULT_CONFIG_FOLDER
        set(value) = prefs.edit().putString(KEY_CONFIG_FOLDER, value).apply()
    
    /**
     * Dossier pour les logs
     */
    var logsFolder: String
        get() = prefs.getString(KEY_LOGS_FOLDER, DEFAULT_LOGS_FOLDER) ?: DEFAULT_LOGS_FOLDER
        set(value) = prefs.edit().putString(KEY_LOGS_FOLDER, value).apply()
    
    /**
     * Utiliser un timestamp dans les noms de fichiers
     */
    var useTimestamp: Boolean
        get() = prefs.getBoolean(KEY_USE_TIMESTAMP, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_TIMESTAMP, value).apply()
    
    /**
     * Format du timestamp
     */
    var timestampFormat: String
        get() = prefs.getString(KEY_TIMESTAMP_FORMAT, DEFAULT_TIMESTAMP_FORMAT) ?: DEFAULT_TIMESTAMP_FORMAT
        set(value) = prefs.edit().putString(KEY_TIMESTAMP_FORMAT, value).apply()
    
    /**
     * Obtient le chemin complet du dossier de profil
     */
    fun getProfilePath(): String {
        return "$rootFolder/$profileFolder"
    }
    
    /**
     * Obtient le chemin complet du dossier de configuration
     */
    fun getConfigPath(): String {
        return "$rootFolder/$configFolder"
    }
    
    /**
     * Obtient le chemin complet du dossier de logs
     */
    fun getLogsPath(): String {
        return "$rootFolder/$logsFolder"
    }
    
    /**
     * Génère un nom de fichier avec timestamp si activé
     */
    fun generateFileName(baseName: String, extension: String = ""): String {
        return if (useTimestamp) {
            val timestamp = java.text.SimpleDateFormat(timestampFormat, java.util.Locale.getDefault())
                .format(java.util.Date())
            val ext = if (extension.isNotEmpty()) ".$extension" else ""
            "${baseName}_$timestamp$ext"
        } else {
            val ext = if (extension.isNotEmpty()) ".$extension" else ""
            "$baseName$ext"
        }
    }
    
    /**
     * Génère un nom de fichier de profil
     */
    fun generateProfileFileName(baseName: String = "therapist_profile"): String {
        return generateFileName(baseName, "json")
    }
    
    /**
     * Génère un nom de fichier de configuration
     */
    fun generateConfigFileName(baseName: String = "app_preferences"): String {
        return generateFileName(baseName, "json")
    }
    
    /**
     * Génère un nom de fichier de log
     */
    fun generateLogFileName(baseName: String, extension: String = "log"): String {
        return generateFileName(baseName, extension)
    }
    
    /**
     * Réinitialise tous les paramètres aux valeurs par défaut
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Obtient un résumé de la configuration
     */
    fun getConfigSummary(): Map<String, String> {
        return mapOf(
            "Dossier racine" to rootFolder,
            "Dossier profil" to getProfilePath(),
            "Dossier configuration" to getConfigPath(),
            "Dossier logs" to getLogsPath(),
            "Timestamp activé" to if (useTimestamp) "Oui" else "Non",
            "Format timestamp" to timestampFormat
        )
    }
    
    /**
     * Valide la configuration
     */
    fun validateConfig(): List<String> {
        val errors = mutableListOf<String>()
        
        if (rootFolder.isBlank()) {
            errors.add("Le dossier racine ne peut pas être vide")
        }
        
        if (profileFolder.isBlank()) {
            errors.add("Le dossier profil ne peut pas être vide")
        }
        
        if (configFolder.isBlank()) {
            errors.add("Le dossier configuration ne peut pas être vide")
        }
        
        if (logsFolder.isBlank()) {
            errors.add("Le dossier logs ne peut pas être vide")
        }
        
        // Vérifier les caractères interdits
        val invalidChars = listOf("/", "\\", ":", "*", "?", "\"", "<", ">", "|")
        val folders = listOf(rootFolder, profileFolder, configFolder, logsFolder)
        
        folders.forEach { folder ->
            invalidChars.forEach { char ->
                if (folder.contains(char)) {
                    errors.add("Le dossier '$folder' contient un caractère invalide: '$char'")
                }
            }
        }
        
        return errors
    }
}
