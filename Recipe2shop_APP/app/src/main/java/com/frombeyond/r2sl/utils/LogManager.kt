package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire unifié pour tous les logs de l'application
 * Centralise la création et la sauvegarde des logs de diagnostics et d'authentification
 */
class LogManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LogManager"
        private const val AUTH_LOGS_FILE = "auth_logs.json"
        private const val DIAGNOSTIC_LOGS_FILE = "diagnostic_logs.json"
        private const val FILE_DIAGNOSTIC_LOGS_FILE = "file_diagnostic_logs.json"
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val humanDateFormat = SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm:ss", Locale.FRENCH)
    
    /**
     * Sauvegarde tous les logs dans des fichiers JSON
     */
    fun saveAllLogs(): Boolean {
        return try {
            var allSuccess = true
            
            // Sauvegarder les logs d'authentification
            if (!saveAuthLogs()) allSuccess = false
            
            // Sauvegarder les logs de diagnostics
            if (!saveDiagnosticLogs()) allSuccess = false
            
            // Sauvegarder les logs de diagnostics de fichiers
            if (!saveFileDiagnosticLogs()) allSuccess = false
            
            Log.i(TAG, "Sauvegarde des logs: ${if (allSuccess) "Succès" else "Échec partiel"}")
            allSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des logs", e)
            false
        }
    }
    
    /**
     * Sauvegarde les logs d'authentification
     */
    private fun saveAuthLogs(): Boolean {
        return try {
            val authLogger = AuthLogger.getInstance(context)
            
            // Créer un objet JSON avec les informations du logger
            val now = Date()
            val authLogData = JSONObject().apply {
                put("log_file_path", authLogger.getLogFilePath())
                put("log_file_accessible", authLogger.isLogFileAccessible())
                put("log_file_size", authLogger.getLogFileSize())
                put("log_directory", authLogger.getLogDirectoryPath())
                put("timestamp", dateFormat.format(now))
                put("human_timestamp", humanDateFormat.format(now))
                put("unix_timestamp", now.time)
            }
            
            val file = File(context.filesDir, AUTH_LOGS_FILE)
            file.writeText(authLogData.toString(2))
            
            Log.i(TAG, "Logs d'authentification sauvegardés")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des logs d'authentification", e)
            false
        }
    }
    
    /**
     * Sauvegarde les logs de diagnostics
     */
    private fun saveDiagnosticLogs(): Boolean {
        return try {
            val diagnosticData = mutableMapOf<String, Any>()
            val now = Date()
            
            // Informations système
            diagnosticData["system_info"] = mapOf(
                "android_version" to android.os.Build.VERSION.RELEASE,
                "api_level" to android.os.Build.VERSION.SDK_INT,
                "device_model" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER,
                "app_version" to getAppVersion(),
                "timestamp" to dateFormat.format(now),
                "human_timestamp" to humanDateFormat.format(now),
                "unix_timestamp" to now.time
            )
            
            // Informations d'authentification
            val authInfo = getAuthInfo()
            diagnosticData["auth_info"] = authInfo
            
            // Informations de stockage
            val storageInfo = getStorageInfo()
            diagnosticData["storage_info"] = storageInfo
            
            // Informations de performance
            val performanceInfo = getPerformanceInfo()
            diagnosticData["performance_info"] = performanceInfo
            
            val file = File(context.filesDir, DIAGNOSTIC_LOGS_FILE)
            file.writeText(JSONObject(diagnosticData as Map<String, Any>).toString(2))
            
            Log.i(TAG, "Logs de diagnostics sauvegardés")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des logs de diagnostics", e)
            false
        }
    }
    
    /**
     * Sauvegarde les logs de diagnostics de fichiers
     */
    private fun saveFileDiagnosticLogs(): Boolean {
        return try {
            val fileDiagnostic = FileDiagnostic(context)
            val diagnosticResult = fileDiagnostic.runFullFileDiagnostic()
            val now = Date()
            
            val diagnosticData = mapOf(
                "timestamp" to dateFormat.format(now),
                "human_timestamp" to humanDateFormat.format(now),
                "unix_timestamp" to now.time,
                "file_analysis" to diagnosticResult,
                "app_files" to getAppFilesInfo(),
                "cache_files" to getCacheFilesInfo(),
                "storage_usage" to getStorageUsageInfo()
            )
            
            val file = File(context.filesDir, FILE_DIAGNOSTIC_LOGS_FILE)
            file.writeText(JSONObject(diagnosticData as Map<String, Any>).toString(2))
            
            Log.i(TAG, "Logs de diagnostics de fichiers sauvegardés")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des logs de diagnostics de fichiers", e)
            false
        }
    }
    
    /**
     * Obtient les informations d'authentification
     */
    private fun getAuthInfo(): Map<String, Any> {
        return try {
            val authLogger = AuthLogger.getInstance(context)
            val now = Date()
            
            mapOf(
                "log_file_path" to authLogger.getLogFilePath(),
                "log_file_accessible" to authLogger.isLogFileAccessible(),
                "log_file_size" to authLogger.getLogFileSize(),
                "log_directory" to authLogger.getLogDirectoryPath(),
                "timestamp" to dateFormat.format(now),
                "human_timestamp" to humanDateFormat.format(now),
                "unix_timestamp" to now.time
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Obtient les informations de stockage
     */
    private fun getStorageInfo(): Map<String, Any> {
        return try {
            val filesDir = context.filesDir
            val cacheDir = context.cacheDir
            val externalFilesDir = context.getExternalFilesDir(null)
            
            mapOf(
                "internal_storage" to mapOf(
                    "files_dir" to filesDir.absolutePath,
                    "files_size" to getDirectorySize(filesDir),
                    "files_count" to getFileCount(filesDir)
                ),
                "cache_storage" to mapOf(
                    "cache_dir" to cacheDir.absolutePath,
                    "cache_size" to getDirectorySize(cacheDir),
                    "cache_count" to getFileCount(cacheDir)
                ),
                "external_storage" to if (externalFilesDir != null) {
                    mapOf(
                        "external_dir" to externalFilesDir.absolutePath,
                        "external_size" to getDirectorySize(externalFilesDir),
                        "external_count" to getFileCount(externalFilesDir)
                    )
                } else {
                    mapOf("available" to false)
                }
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Obtient les informations de performance
     */
    private fun getPerformanceInfo(): Map<String, Any> {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            mapOf(
                "memory" to mapOf(
                    "max_memory_mb" to maxMemory / (1024 * 1024),
                    "total_memory_mb" to totalMemory / (1024 * 1024),
                    "free_memory_mb" to freeMemory / (1024 * 1024),
                    "used_memory_mb" to usedMemory / (1024 * 1024),
                    "memory_usage_percent" to (usedMemory * 100 / maxMemory)
                ),
                "uptime" to android.os.SystemClock.uptimeMillis(),
                "timestamp" to dateFormat.format(Date()),
                "human_timestamp" to humanDateFormat.format(Date()),
                "unix_timestamp" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Obtient les informations sur les fichiers de l'application
     */
    private fun getAppFilesInfo(): Map<String, Any> {
        return try {
            val filesDir = context.filesDir
            val files = filesDir.listFiles() ?: emptyArray()
            
            mapOf(
                "total_files" to files.size,
                "files" to files.map { file ->
                    mapOf(
                        "name" to file.name,
                        "size" to file.length(),
                        "modified" to file.lastModified(),
                        "is_directory" to file.isDirectory
                    )
                }
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Obtient les informations sur les fichiers de cache
     */
    private fun getCacheFilesInfo(): Map<String, Any> {
        return try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles() ?: emptyArray()
            
            mapOf(
                "total_files" to files.size,
                "total_size" to getDirectorySize(cacheDir),
                "files" to files.map { file ->
                    mapOf(
                        "name" to file.name,
                        "size" to file.length(),
                        "modified" to file.lastModified(),
                        "is_directory" to file.isDirectory
                    )
                }
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Obtient les informations d'utilisation du stockage
     */
    private fun getStorageUsageInfo(): Map<String, Any> {
        return try {
            val filesDir = context.filesDir
            val cacheDir = context.cacheDir
            
            mapOf(
                "internal_storage_mb" to getDirectorySize(filesDir) / (1024 * 1024),
                "cache_storage_mb" to getDirectorySize(cacheDir) / (1024 * 1024),
                "total_storage_mb" to (getDirectorySize(filesDir) + getDirectorySize(cacheDir)) / (1024 * 1024)
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Erreur inconnue"))
        }
    }
    
    /**
     * Calcule la taille d'un répertoire
     */
    private fun getDirectorySize(directory: File): Long {
        return try {
            if (!directory.exists() || !directory.isDirectory) return 0L
            
            var size = 0L
            val files = directory.listFiles() ?: return 0L
            
            for (file in files) {
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
            size
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du calcul de la taille du répertoire: ${directory.absolutePath}", e)
            0L
        }
    }
    
    /**
     * Compte le nombre de fichiers dans un répertoire
     */
    private fun getFileCount(directory: File): Int {
        return try {
            if (!directory.exists() || !directory.isDirectory) return 0
            
            var count = 0
            val files = directory.listFiles() ?: return 0
            
            for (file in files) {
                count += if (file.isDirectory) {
                    getFileCount(file)
                } else {
                    1
                }
            }
            count
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du comptage des fichiers: ${directory.absolutePath}", e)
            0
        }
    }
    
    /**
     * Obtient la version de l'application
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Version inconnue"
        }
    }
    
    /**
     * Efface tous les logs
     */
    fun clearAllLogs(): Boolean {
        return try {
            val files = listOf(
                File(context.filesDir, AUTH_LOGS_FILE),
                File(context.filesDir, DIAGNOSTIC_LOGS_FILE),
                File(context.filesDir, FILE_DIAGNOSTIC_LOGS_FILE)
            )
            
            var allSuccess = true
            files.forEach { file ->
                if (file.exists()) {
                    if (!file.delete()) {
                        allSuccess = false
                        Log.w(TAG, "Impossible de supprimer le fichier: ${file.absolutePath}")
                    }
                }
            }
            
            Log.i(TAG, "Logs effacés: ${if (allSuccess) "Succès" else "Échec partiel"}")
            allSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'effacement des logs", e)
            false
        }
    }
}
