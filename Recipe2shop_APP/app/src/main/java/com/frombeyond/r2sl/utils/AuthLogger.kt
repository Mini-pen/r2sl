package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire centralisé de logging pour l'authentification
 * Permet de tracer toutes les erreurs et étapes du processus d'auth
 */
class AuthLogger private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthLogger"
        private const val LOG_FILE_NAME = "auth_errors.log"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB max
        
        @Volatile
        private var INSTANCE: AuthLogger? = null
        
        fun getInstance(context: Context): AuthLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val humanDateFormat = SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm:ss", Locale.FRENCH)
    
    /**
     * Log une erreur d'authentification avec tous les détails
     */
    fun logAuthError(
        errorType: String,
        errorMessage: String,
        exception: Exception? = null,
        additionalInfo: Map<String, Any>? = null
    ) {
        val timestamp = dateFormat.format(Date())
        val logEntry = buildLogEntry(timestamp, errorType, errorMessage, exception, additionalInfo)
        
        // Log dans Logcat
        Log.e(TAG, logEntry)
        
        // Log dans le fichier
        writeToLogFile(logEntry)
        
        // Afficher un toast pour l'utilisateur
        showUserToast(errorMessage)
    }
    
    /**
     * Log une étape de l'authentification
     */
    fun logAuthStep(step: String, details: String? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] STEP: $step${if (details != null) " - $details" else ""}"
        
        Log.d(TAG, logEntry)
        writeToLogFile(logEntry)
    }
    
    /**
     * Log une information d'authentification
     */
    fun logAuthInfo(info: String, details: String? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] INFO: $info${if (details != null) " - $details" else ""}"
        
        Log.i(TAG, logEntry)
        writeToLogFile(logEntry)
    }
    
    /**
     * Log une erreur de configuration
     */
    fun logConfigError(configName: String, expectedValue: String, actualValue: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] CONFIG_ERROR: $configName - Expected: $expectedValue, Actual: $actualValue"
        
        Log.e(TAG, logEntry)
        writeToLogFile(logEntry)
        
        // Toast spécifique pour les erreurs de configuration
        showUserToast("Erreur de configuration détectée. Vérifiez les logs.")
    }
    
    /**
     * Construit l'entrée de log complète
     */
    private fun buildLogEntry(
        timestamp: String,
        errorType: String,
        errorMessage: String,
        exception: Exception?,
        additionalInfo: Map<String, Any>?
    ): String {
        val sb = StringBuilder()
        val now = Date()
        val humanTimestamp = humanDateFormat.format(now)
        sb.append("[$timestamp] ($humanTimestamp) ERROR: $errorType - $errorMessage")
        
        exception?.let { ex ->
            sb.append("\nException: ${ex.javaClass.simpleName}")
            sb.append("\nMessage: ${ex.message}")
            sb.append("\nStack Trace:")
            ex.stackTrace.take(10).forEach { element ->
                sb.append("\n  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
            }
        }
        
        additionalInfo?.let { info ->
            sb.append("\nAdditional Info:")
            info.forEach { (key, value) ->
                sb.append("\n  $key: $value")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Écrit dans le fichier de log
     */
    private fun writeToLogFile(logEntry: String) {
        try {
            val logFile = getLogFile()
            
            // Vérifier la taille du fichier
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile(logFile)
            }
            
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry).append("\n\n")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Impossible d'écrire dans le fichier de log: ${e.message}")
        }
    }
    
    /**
     * Rotation du fichier de log quand il devient trop gros
     */
    private fun rotateLogFile(logFile: File) {
        try {
            val backupFile = File(logFile.parent, "${logFile.name}.backup")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de faire la rotation du fichier de log: ${e.message}")
        }
    }
    
    /**
     * Récupère le fichier de log
     */
    private fun getLogFile(): File {
        // Créer le dossier dans le stockage externe accessible
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    /**
     * Affiche un toast à l'utilisateur
     */
    private fun showUserToast(message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'afficher le toast: ${e.message}")
        }
    }
    
    /**
     * Récupère le contenu du fichier de log
     */
    fun getLogContent(): String {
        return try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "Aucun fichier de log trouvé"
            }
        } catch (e: Exception) {
            "Erreur lors de la lecture du fichier de log: ${e.message}"
        }
    }
    
    /**
     * Efface le fichier de log
     */
    fun clearLog() {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'effacer le fichier de log: ${e.message}")
        }
    }
    
    /**
     * Récupère le chemin du fichier de log pour l'utilisateur
     */
    fun getLogFilePath(): String {
        return getLogFile().absolutePath
    }
    
    /**
     * Vérifie si le fichier de log existe et est accessible
     */
    fun isLogFileAccessible(): Boolean {
        val logFile = getLogFile()
        return logFile.exists() && logFile.canRead()
    }
    
    /**
     * Récupère la taille du fichier de log
     */
    fun getLogFileSize(): Long {
        val logFile = getLogFile()
        return if (logFile.exists()) logFile.length() else 0
    }
    
    /**
     * Récupère le dossier parent du fichier de log
     */
    fun getLogDirectoryPath(): String {
        return getLogFile().parent ?: "Inaccessible"
    }
}
