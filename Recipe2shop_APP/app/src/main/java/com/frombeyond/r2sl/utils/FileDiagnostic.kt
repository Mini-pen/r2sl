package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire de diagnostic pour analyser les fichiers créés par l'application
 * Permet d'identifier les problèmes de stockage et de corruption de données
 */
class FileDiagnostic(private val context: Context) {
    
    companion object {
        private const val TAG = "FileDiagnostic"
        private const val MAX_FILE_SIZE_MB = 100 // Taille maximale suspecte pour un fichier
        private const val MAX_TOTAL_SIZE_MB = 500 // Taille maximale totale suspecte
    }
    
    private val authLogger = AuthLogger.getInstance(context)
    private val humanDateFormat = SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm:ss", Locale.FRENCH)
    
    /**
     * Lance un diagnostic complet des fichiers de l'application
     */
    fun runFullFileDiagnostic(): FileDiagnosticResult {
        authLogger.logAuthStep("Démarrage du diagnostic des fichiers")
        
        val results = mutableListOf<FileDiagnosticItem>()
        
        // 1. Analyser le répertoire de données internes
        results.add(analyzeInternalDataDirectory())
        
        // 2. Analyser le répertoire de cache
        results.add(analyzeCacheDirectory())
        
        // 3. Analyser les fichiers de base de données
        results.add(analyzeDatabaseFiles())
        
        // 4. Analyser les fichiers de configuration
        results.add(analyzeConfigFiles())
        
        // 5. Analyser les fichiers de logs
        results.add(analyzeLogFiles())
        
        // 6. Analyser les fichiers de profil utilisateur
        results.add(analyzeUserProfileFiles())
        
        // 7. Vérifier l'intégrité des fichiers critiques
        results.add(checkFileIntegrity())
        
        // 8. Analyser l'espace disque disponible
        results.add(analyzeDiskSpace())
        
        // Analyser les résultats
        val hasErrors = results.any { it.status == FileDiagnosticStatus.ERROR }
        val hasWarnings = results.any { it.status == FileDiagnosticStatus.WARNING }
        
        val overallStatus = when {
            hasErrors -> FileDiagnosticStatus.ERROR
            hasWarnings -> FileDiagnosticStatus.WARNING
            else -> FileDiagnosticStatus.SUCCESS
        }
        
        val result = FileDiagnosticResult(
            overallStatus = overallStatus,
            items = results,
            summary = generateFileSummary(results),
            totalFiles = results.sumOf { it.fileCount },
            totalSize = results.sumOf { it.totalSizeBytes }
        )
        
        // Logger le résultat final
        when (overallStatus) {
            FileDiagnosticStatus.SUCCESS -> authLogger.logAuthInfo("Diagnostic des fichiers réussi - Aucun problème détecté")
            FileDiagnosticStatus.WARNING -> authLogger.logAuthInfo("Diagnostic des fichiers terminé avec des avertissements", result.summary)
            FileDiagnosticStatus.ERROR -> authLogger.logAuthError("Diagnostic des fichiers échoué", result.summary)
        }
        
        return result
    }
    
    /**
     * Analyser le répertoire de données internes
     */
    private fun analyzeInternalDataDirectory(): FileDiagnosticItem {
        return try {
            val dataDir = context.filesDir
            val files = dataDir.listFiles() ?: emptyArray()
            
            val fileDetails = files.map { file ->
                FileInfo(
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    path = file.absolutePath
                )
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val suspiciousFiles = fileDetails.filter { 
                it.size > MAX_FILE_SIZE_MB * 1024 * 1024 || 
                (!it.canRead && !it.isDirectory) ||
                (!it.canWrite && !it.isDirectory)
            }
            
            val status = when {
                suspiciousFiles.isNotEmpty() -> FileDiagnosticStatus.WARNING
                totalSize > MAX_TOTAL_SIZE_MB * 1024 * 1024 -> FileDiagnosticStatus.WARNING
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Répertoire de données internes", "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}")
            
            FileDiagnosticItem(
                name = "Répertoire de Données Internes",
                status = status,
                message = "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, suspiciousFiles),
                fileCount = files.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Répertoire de données internes", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Répertoire de Données Internes",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser le répertoire de cache
     */
    private fun analyzeCacheDirectory(): FileDiagnosticItem {
        return try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles() ?: emptyArray()
            
            val fileDetails = files.map { file ->
                FileInfo(
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    path = file.absolutePath
                )
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val oldFiles = fileDetails.filter { 
                System.currentTimeMillis() - it.lastModified > 7 * 24 * 60 * 60 * 1000 // Plus de 7 jours
            }
            
            val status = when {
                oldFiles.size > files.size * 0.8 -> FileDiagnosticStatus.WARNING // Plus de 80% de fichiers anciens
                totalSize > 200 * 1024 * 1024 -> FileDiagnosticStatus.WARNING // Plus de 200MB de cache
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Répertoire de cache", "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}")
            
            FileDiagnosticItem(
                name = "Répertoire de Cache",
                status = status,
                message = "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, oldFiles),
                fileCount = files.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Répertoire de cache", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Répertoire de Cache",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser les fichiers de base de données
     */
    private fun analyzeDatabaseFiles(): FileDiagnosticItem {
        return try {
            val databaseDir = File(context.filesDir, "databases")
            val files = if (databaseDir.exists()) databaseDir.listFiles() ?: emptyArray() else emptyArray()
            
            val fileDetails = files.map { file ->
                FileInfo(
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    path = file.absolutePath
                )
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val corruptedFiles = fileDetails.filter { 
                it.size == 0L && !it.isDirectory // Fichiers vides suspects
            }
            
            val status = when {
                corruptedFiles.isNotEmpty() -> FileDiagnosticStatus.ERROR
                totalSize == 0L && files.isNotEmpty() -> FileDiagnosticStatus.WARNING
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Fichiers de base de données", "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}")
            
            FileDiagnosticItem(
                name = "Fichiers de Base de Données",
                status = status,
                message = "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, corruptedFiles),
                fileCount = files.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Fichiers de base de données", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Fichiers de Base de Données",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser les fichiers de configuration
     */
    private fun analyzeConfigFiles(): FileDiagnosticItem {
        return try {
            val configFiles = listOf(
                "therapist_profile.json",
                "app_preferences.json",
                "user_settings.json"
            )
            
            val fileDetails = configFiles.mapNotNull { fileName ->
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    FileInfo(
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = file.isDirectory,
                        canRead = file.canRead(),
                        canWrite = file.canWrite(),
                        path = file.absolutePath
                    )
                } else null
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val missingFiles = configFiles.filter { fileName ->
                !File(context.filesDir, fileName).exists()
            }
            
            val status = when {
                missingFiles.size == configFiles.size -> FileDiagnosticStatus.WARNING
                fileDetails.any { !it.canRead || !it.canWrite } -> FileDiagnosticStatus.ERROR
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Fichiers de configuration", "Analysé: ${fileDetails.size}/${configFiles.size} fichiers")
            
            FileDiagnosticItem(
                name = "Fichiers de Configuration",
                status = status,
                message = "Analysé: ${fileDetails.size}/${configFiles.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, emptyList()) + 
                         if (missingFiles.isNotEmpty()) "\nFichiers manquants: ${missingFiles.joinToString(", ")}" else "",
                fileCount = fileDetails.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Fichiers de configuration", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Fichiers de Configuration",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser les fichiers de logs
     */
    private fun analyzeLogFiles(): FileDiagnosticItem {
        return try {
            val logDir = File(context.filesDir, "logs")
            val files = if (logDir.exists()) logDir.listFiles() ?: emptyArray() else emptyArray()
            
            val fileDetails = files.map { file ->
                FileInfo(
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    path = file.absolutePath
                )
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val largeLogFiles = fileDetails.filter { 
                it.size > 10 * 1024 * 1024 // Plus de 10MB
            }
            
            val status = when {
                largeLogFiles.isNotEmpty() -> FileDiagnosticStatus.WARNING
                totalSize > 50 * 1024 * 1024 -> FileDiagnosticStatus.WARNING // Plus de 50MB de logs
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Fichiers de logs", "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}")
            
            FileDiagnosticItem(
                name = "Fichiers de Logs",
                status = status,
                message = "Analysé: ${files.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, largeLogFiles),
                fileCount = files.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Fichiers de logs", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Fichiers de Logs",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser les fichiers de profil utilisateur
     */
    private fun analyzeUserProfileFiles(): FileDiagnosticItem {
        return try {
            val profileFiles = listOf(
                "therapist_profile.json",
                "user_avatar.jpg",
                "user_avatar.png"
            )
            
            val fileDetails = profileFiles.mapNotNull { fileName ->
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    FileInfo(
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = file.isDirectory,
                        canRead = file.canRead(),
                        canWrite = file.canWrite(),
                        path = file.absolutePath
                    )
                } else null
            }
            
            val totalSize = fileDetails.sumOf { it.size }
            val corruptedFiles = fileDetails.filter { 
                it.size == 0L && !it.isDirectory // Fichiers vides suspects
            }
            
            val status = when {
                corruptedFiles.isNotEmpty() -> FileDiagnosticStatus.ERROR
                fileDetails.isEmpty() -> FileDiagnosticStatus.WARNING
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Fichiers de profil utilisateur", "Analysé: ${fileDetails.size} fichiers")
            
            FileDiagnosticItem(
                name = "Fichiers de Profil Utilisateur",
                status = status,
                message = "Analysé: ${fileDetails.size} fichiers, ${formatBytes(totalSize)}",
                details = buildFileDetails(fileDetails, corruptedFiles),
                fileCount = fileDetails.size,
                totalSizeBytes = totalSize,
                files = fileDetails
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Fichiers de profil utilisateur", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Fichiers de Profil Utilisateur",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Vérifier l'intégrité des fichiers critiques
     */
    private fun checkFileIntegrity(): FileDiagnosticItem {
        return try {
            val criticalFiles = listOf(
                "therapist_profile.json",
                "app_preferences.json"
            )
            
            var corruptedCount = 0
            var totalChecked = 0
            val details = StringBuilder()
            
            criticalFiles.forEach { fileName ->
                val file = File(context.filesDir, fileName)
                totalChecked++
                
                if (file.exists()) {
                    try {
                        // Essayer de lire le fichier pour vérifier l'intégrité
                        val content = file.readText()
                        if (content.isEmpty()) {
                            corruptedCount++
                            details.append("⚠️ $fileName: Fichier vide\n")
                        } else if (fileName.endsWith(".json") && !isValidJson(content)) {
                            corruptedCount++
                            details.append("❌ $fileName: JSON invalide\n")
                            
                            // Pour le fichier de profil, essayer de le réparer
                            if (fileName == "therapist_profile.json") {
                                try {
                                    file.delete()
                                    details.append("   → Fichier corrompu supprimé, sera recréé\n")
                                } catch (e: Exception) {
                                    details.append("   → Impossible de supprimer le fichier corrompu\n")
                                }
                            }
                        } else {
                            // Vérification supplémentaire pour le fichier de profil
                            if (fileName == "therapist_profile.json") {
                                val json = org.json.JSONObject(content)
                                val requiredFields = listOf("firstName", "lastName", "profession", "apiKey")
                                val missingFields = requiredFields.filter { !json.has(it) }
                                
                                if (missingFields.isNotEmpty()) {
                                    corruptedCount++
                                    details.append("❌ $fileName: Champs manquants: ${missingFields.joinToString(", ")}\n")
                                } else {
                                    details.append("✅ $fileName: Intégrité OK\n")
                                }
                            } else {
                                details.append("✅ $fileName: Intégrité OK\n")
                            }
                        }
                    } catch (e: Exception) {
                        corruptedCount++
                        details.append("❌ $fileName: Erreur de lecture - ${e.message}\n")
                    }
                } else {
                    details.append("⚠️ $fileName: Fichier manquant\n")
                }
            }
            
            val status = when {
                corruptedCount > 0 -> FileDiagnosticStatus.ERROR
                totalChecked == 0 -> FileDiagnosticStatus.WARNING
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Intégrité des fichiers", "Vérifié: $totalChecked fichiers, $corruptedCount corrompus")
            
            FileDiagnosticItem(
                name = "Intégrité des Fichiers Critiques",
                status = status,
                message = "Vérifié: $totalChecked fichiers, $corruptedCount corrompus",
                details = details.toString(),
                fileCount = totalChecked,
                totalSizeBytes = 0,
                files = emptyList()
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Intégrité des fichiers", "Erreur lors de la vérification", e)
            FileDiagnosticItem(
                name = "Intégrité des Fichiers Critiques",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Analyser l'espace disque disponible
     */
    private fun analyzeDiskSpace(): FileDiagnosticItem {
        return try {
            val dataDir = context.filesDir
            val freeSpace = dataDir.freeSpace
            val totalSpace = dataDir.totalSpace
            val usedSpace = totalSpace - freeSpace
            
            val freeSpaceMB = freeSpace / (1024 * 1024)
            val usedSpaceMB = usedSpace / (1024 * 1024)
            val totalSpaceMB = totalSpace / (1024 * 1024)
            
            val status = when {
                freeSpaceMB < 100 -> FileDiagnosticStatus.ERROR // Moins de 100MB libres
                freeSpaceMB < 500 -> FileDiagnosticStatus.WARNING // Moins de 500MB libres
                else -> FileDiagnosticStatus.SUCCESS
            }
            
            authLogger.logAuthStep("Espace disque", "Libre: ${freeSpaceMB}MB, Utilisé: ${usedSpaceMB}MB")
            
            FileDiagnosticItem(
                name = "Espace Disque Disponible",
                status = status,
                message = "Libre: ${freeSpaceMB}MB, Utilisé: ${usedSpaceMB}MB sur ${totalSpaceMB}MB",
                details = "Espace libre: ${formatBytes(freeSpace)}\nEspace utilisé: ${formatBytes(usedSpace)}\nEspace total: ${formatBytes(totalSpace)}",
                fileCount = 0,
                totalSizeBytes = usedSpace,
                files = emptyList()
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Espace disque", "Erreur lors de l'analyse", e)
            FileDiagnosticItem(
                name = "Espace Disque Disponible",
                status = FileDiagnosticStatus.ERROR,
                message = "Erreur lors de l'analyse",
                details = e.message ?: "Exception inconnue",
                fileCount = 0,
                totalSizeBytes = 0,
                files = emptyList()
            )
        }
    }
    
    /**
     * Construire les détails des fichiers
     */
    private fun buildFileDetails(files: List<FileInfo>, suspiciousFiles: List<FileInfo>): String {
        val details = StringBuilder()
        
        if (files.isNotEmpty()) {
            details.append("📁 Fichiers détectés:\n")
            files.forEach { file ->
                val lastModified = Date(file.lastModified)
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(lastModified)
                val humanDate = humanDateFormat.format(lastModified)
                val size = formatBytes(file.size)
                val permissions = buildPermissionsString(file)
                val status = if (suspiciousFiles.contains(file)) "⚠️" else "✅"
                
                details.append("$status ${file.name} ($size, $date, $permissions)\n")
                details.append("   📅 Date lisible: $humanDate\n")
            }
        }
        
        if (suspiciousFiles.isNotEmpty()) {
            details.append("\n⚠️ Fichiers suspects:\n")
            suspiciousFiles.forEach { file ->
                details.append("• ${file.name}: ${file.size} bytes\n")
            }
        }
        
        return details.toString()
    }
    
    /**
     * Construire la chaîne de permissions
     */
    private fun buildPermissionsString(file: FileInfo): String {
        val read = if (file.canRead) "R" else "-"
        val write = if (file.canWrite) "W" else "-"
        val dir = if (file.isDirectory) "D" else "F"
        return "$dir$read$write"
    }
    
    /**
     * Vérifier si une chaîne est un JSON valide
     */
    private fun isValidJson(jsonString: String): Boolean {
        return try {
            org.json.JSONObject(jsonString)
            true
        } catch (e: Exception) {
            try {
                org.json.JSONArray(jsonString)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    /**
     * Formater les bytes en unités lisibles
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.2f %s", size, units[unitIndex])
    }
    
    /**
     * Générer un résumé des résultats
     */
    private fun generateFileSummary(items: List<FileDiagnosticItem>): String {
        val errorCount = items.count { it.status == FileDiagnosticStatus.ERROR }
        val warningCount = items.count { it.status == FileDiagnosticStatus.WARNING }
        val successCount = items.count { it.status == FileDiagnosticStatus.SUCCESS }
        val totalFiles = items.sumOf { it.fileCount }
        val totalSize = items.sumOf { it.totalSizeBytes }
        
        return "Résumé: $successCount succès, $warningCount avertissements, $errorCount erreurs\n" +
               "Total: $totalFiles fichiers, ${formatBytes(totalSize)}"
    }
    
    /**
     * Résultat du diagnostic des fichiers
     */
    data class FileDiagnosticResult(
        val overallStatus: FileDiagnosticStatus,
        val items: List<FileDiagnosticItem>,
        val summary: String,
        val totalFiles: Int,
        val totalSize: Long
    )
    
    /**
     * Élément de diagnostic de fichier individuel
     */
    data class FileDiagnosticItem(
        val name: String,
        val status: FileDiagnosticStatus,
        val message: String,
        val details: String,
        val fileCount: Int,
        val totalSizeBytes: Long,
        val files: List<FileInfo>
    )
    
    /**
     * Information sur un fichier
     */
    data class FileInfo(
        val name: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean,
        val canRead: Boolean,
        val canWrite: Boolean,
        val path: String
    )
    
    /**
     * Statut du diagnostic de fichier
     */
    enum class FileDiagnosticStatus {
        SUCCESS, WARNING, ERROR
    }
}
