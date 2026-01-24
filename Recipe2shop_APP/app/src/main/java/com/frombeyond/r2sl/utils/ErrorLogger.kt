package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gestionnaire centralisé des erreurs de l'application
 * Capture toutes les exceptions non gérées et les logs d'erreurs
 */
class ErrorLogger private constructor() {
    
    companion object {
        private const val TAG = "ErrorLogger"
        private const val MAX_LOG_ENTRIES = 100
        private const val LOG_FILE_NAME = "app_errors.log"
        private const val MAX_LOG_FILE_SIZE = 1024 * 1024 // 1MB max
        
        @Volatile
        private var INSTANCE: ErrorLogger? = null
        
        fun getInstance(): ErrorLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ErrorLogger().also { INSTANCE = it }
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val errorLogs = CopyOnWriteArrayList<ErrorLogEntry>()
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    private fun getLogFile(): File? {
        val ctx = context ?: return null
        val logDir = File(ctx.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    /**
     * Enregistre une erreur
     */
    fun logError(message: String, exception: Throwable? = null, tag: String = TAG) {
        val timestamp = dateFormat.format(Date())
        val entry = ErrorLogEntry(
            timestamp = timestamp,
            tag = tag,
            message = message,
            exception = exception,
            stackTrace = exception?.stackTraceToString()
        )
        
        synchronized(errorLogs) {
            errorLogs.add(0, entry) // Ajouter au début
            // Limiter le nombre d'entrées
            if (errorLogs.size > MAX_LOG_ENTRIES) {
                errorLogs.removeAt(errorLogs.size - 1)
            }
        }
        
        // Log aussi dans Logcat
        if (exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.e(tag, message)
        }
        
        // Sauvegarder dans le fichier
        saveToFile(entry)
    }
    
    private fun saveToFile(entry: ErrorLogEntry) {
        val logFile = getLogFile() ?: return
        try {
            // Vérifier la taille du fichier
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile(logFile)
            }
            
            val logEntry = buildString {
                append("[${entry.timestamp}] ${entry.tag}\n")
                append("Message: ${entry.message}\n")
                if (entry.stackTrace != null) {
                    append("Stack trace:\n${entry.stackTrace}\n")
                }
                append("\n")
            }
            
            logFile.appendText(logEntry, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'écrire dans le fichier de log: ${e.message}")
        }
    }
    
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
    
    fun getLogFilePath(): String? {
        return getLogFile()?.absolutePath
    }
    
    /**
     * Récupère tous les logs d'erreurs
     */
    fun getAllLogs(): List<ErrorLogEntry> {
        return synchronized(errorLogs) {
            errorLogs.toList()
        }
    }
    
    /**
     * Efface tous les logs
     */
    fun clearLogs() {
        synchronized(errorLogs) {
            errorLogs.clear()
        }
    }
    
    /**
     * Format les logs pour l'affichage
     */
    fun getFormattedLogs(): String {
        return getAllLogs().joinToString("\n\n") { entry ->
            buildString {
                append("[${entry.timestamp}] ${entry.tag}\n")
                append("Message: ${entry.message}\n")
                if (entry.stackTrace != null) {
                    append("Stack trace:\n${entry.stackTrace}")
                }
            }
        }
    }
    
    data class ErrorLogEntry(
        val timestamp: String,
        val tag: String,
        val message: String,
        val exception: Throwable?,
        val stackTrace: String?
    )
}
