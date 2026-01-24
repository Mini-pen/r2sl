package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.frombeyond.r2sl.data.BackupPathConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as AndroidFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestionnaire pour les opérations Google Drive
 * Permet la sauvegarde automatique des fichiers de configuration, profil et logs
 * avec des chemins configurables et versioning
 */
class GoogleDriveManager(private val context: Context) {
    
    /**
     * Classe pour gérer le versioning des sauvegardes
     */
    data class BackupVersion(
        val versionNumber: String,
        val buildDate: String,
        val buildTime: String
    ) {
        fun getFormattedVersion(): String {
            return "${versionNumber}_${buildDate}_${buildTime}"
        }
    }
    
    /**
     * Génère une version de sauvegarde avec la date/heure actuelle
     */
    private fun generateBackupVersion(versionNumber: String): BackupVersion {
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        return BackupVersion(
            versionNumber = versionNumber,
            buildDate = dateFormat.format(now),
            buildTime = timeFormat.format(now)
        )
    }
    
    companion object {
        private const val TAG = "GoogleDriveManager"
        private val SCOPES = listOf(DriveScopes.DRIVE_FILE)
        private const val THERAPIA_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    }
    
    private var driveService: Drive? = null
    private var therapiaFolderId: String? = null
    private val backupPathConfig = BackupPathConfig(context)
    private val logManager = LogManager(context)
    
    // Cache des IDs des dossiers
    private var profileFolderId: String? = null
    private var configFolderId: String? = null
    private var logsFolderId: String? = null
    
    /**
     * Initialise le service Google Drive avec les credentials
     */
    fun initialize(credential: GoogleAccountCredential) {
        try {
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            )
                .setApplicationName("TherapIA")
                .build()
            
            Log.i(TAG, "Service Google Drive initialisé avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation du service Google Drive", e)
        }
    }
    
    /**
     * Vérifie si le service est initialisé
     */
    fun isInitialized(): Boolean {
        return driveService != null
    }
    
    /**
     * Teste la connexion Google Drive en listant les fichiers
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized()) {
                Log.e(TAG, "Service Google Drive non initialisé pour le test de connexion")
                return@withContext false
            }
            
            Log.i(TAG, "Test de connexion Google Drive en cours...")
            
            // Essayer de lister les fichiers (limite à 1 pour le test)
            val result = driveService!!.files().list()
                .setPageSize(1)
                .setFields("nextPageToken, files(id, name)")
                .execute()
            
            Log.i(TAG, "Test de connexion Google Drive réussi - ${result.files?.size ?: 0} fichiers trouvés")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Test de connexion Google Drive échoué", e)
            Log.e(TAG, "Type d'erreur: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message d'erreur: ${e.message}")
            if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                Log.e(TAG, "Code d'erreur Google: ${e.statusCode}")
                Log.e(TAG, "Détails Google: ${e.details}")
            }
            return@withContext false
        }
    }
    
    /**
     * Crée ou récupère le dossier racine TherapIA dans Google Drive
     */
    suspend fun ensureTherapiaFolder(): String? = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext null
            }
            
            // Vérifier si le dossier racine existe déjà
            val existingFolder = findFolder(backupPathConfig.rootFolder, null)
            if (existingFolder != null) {
                therapiaFolderId = existingFolder.id
                Log.i(TAG, "Dossier racine TherapIA trouvé: ${existingFolder.id}")
                return@withContext existingFolder.id
            }
            
            // Créer le dossier racine s'il n'existe pas
            val folderMetadata = File().apply {
                name = backupPathConfig.rootFolder
                mimeType = THERAPIA_FOLDER_MIME_TYPE
            }
            
            val folder = driveService!!.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            therapiaFolderId = folder.id
            Log.i(TAG, "Dossier racine TherapIA créé: ${folder.id}")
            return@withContext folder.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la création/récupération du dossier racine TherapIA", e)
            return@withContext null
        }
    }
    
    /**
     * Recherche un dossier par nom et parent
     */
    private suspend fun findFolder(folderName: String, parentId: String?): File? = withContext(Dispatchers.IO) {
        try {
            val parentQuery = if (parentId != null) "'$parentId' in parents" else "parents in 'root'"
            val query = "name='$folderName' and mimeType='$THERAPIA_FOLDER_MIME_TYPE' and trashed=false and $parentQuery"
            val result: FileList = driveService!!.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            return@withContext result.files?.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la recherche du dossier: $folderName", e)
            return@withContext null
        }
    }
    
    /**
     * Crée ou récupère un dossier spécifique (profil, config, logs)
     */
    private suspend fun ensureSubFolder(folderName: String, parentId: String?): String? = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext null
            }
            
            // Vérifier si le dossier existe déjà
            val existingFolder = findFolder(folderName, parentId)
            if (existingFolder != null) {
                Log.i(TAG, "Dossier $folderName trouvé: ${existingFolder.id}")
                return@withContext existingFolder.id
            }
            
            // Créer le dossier s'il n'existe pas
            val folderMetadata = File().apply {
                name = folderName
                mimeType = THERAPIA_FOLDER_MIME_TYPE
                if (parentId != null) {
                    parents = listOf(parentId)
                }
            }
            
            val folder = driveService!!.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            Log.i(TAG, "Dossier $folderName créé: ${folder.id}")
            return@withContext folder.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la création/récupération du dossier: $folderName", e)
            return@withContext null
        }
    }
    
    /**
     * Assure que tous les dossiers de sauvegarde existent
     */
    suspend fun ensureAllBackupFolders(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Créer/récupérer le dossier racine
            val rootId = ensureTherapiaFolder()
            if (rootId == null) {
                Log.e(TAG, "Impossible de créer/récupérer le dossier racine")
                return@withContext false
            }
            
            // Créer/récupérer les sous-dossiers
            profileFolderId = ensureSubFolder(backupPathConfig.profileFolder, rootId)
            configFolderId = ensureSubFolder(backupPathConfig.configFolder, rootId)
            logsFolderId = ensureSubFolder(backupPathConfig.logsFolder, rootId)
            
            val allCreated = profileFolderId != null && configFolderId != null && logsFolderId != null
            
            if (allCreated) {
                Log.i(TAG, "Tous les dossiers de sauvegarde sont prêts")
                Log.i(TAG, "Structure: ${backupPathConfig.rootFolder}/")
                Log.i(TAG, "  ├── ${backupPathConfig.profileFolder}/")
                Log.i(TAG, "  ├── ${backupPathConfig.configFolder}/")
                Log.i(TAG, "  └── ${backupPathConfig.logsFolder}/")
            } else {
                Log.e(TAG, "Échec de la création de certains dossiers de sauvegarde")
            }
            
            return@withContext allCreated
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la création des dossiers de sauvegarde", e)
            return@withContext false
        }
    }
    
    /**
     * Sauvegarde un fichier local vers Google Drive dans un dossier spécifique
     */
    suspend fun uploadFile(
        localFile: AndroidFile,
        driveFileName: String,
        folderType: BackupFolderType,
        mimeType: String = "text/plain",
        versionNumber: String = "0.3"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext false
            }
            
            // S'assurer que tous les dossiers existent
            if (!ensureAllBackupFolders()) {
                Log.e(TAG, "Impossible de créer/récupérer les dossiers de sauvegarde")
                return@withContext false
            }
            
            // Obtenir l'ID du dossier approprié
            val folderId = when (folderType) {
                BackupFolderType.PROFILE -> profileFolderId
                BackupFolderType.CONFIG -> configFolderId
                BackupFolderType.LOGS -> logsFolderId
            }
            
            if (folderId == null) {
                Log.e(TAG, "ID du dossier $folderType non disponible")
                return@withContext false
            }
            
            // Lire le contenu du fichier local
            val fileContent = localFile.readBytes()
            
            // Générer la version de sauvegarde
            val backupVersion = generateBackupVersion(versionNumber)
            
            // Créer le nom de fichier avec versioning (seulement si pas déjà présent)
            val fileNameWithoutExt = driveFileName.substringBeforeLast(".")
            val fileExtension = if (driveFileName.contains(".")) ".${driveFileName.substringAfterLast(".")}" else ""
            
            // Vérifier si le nom contient déjà un timestamp (format yyyy-MM-dd)
            val hasTimestamp = fileNameWithoutExt.contains(Regex("\\d{4}-\\d{2}-\\d{2}"))
            val versionedFileName = if (hasTimestamp) {
                // Le fichier a déjà un timestamp, ne pas en ajouter un autre
                driveFileName
            } else {
                // Ajouter le versioning seulement si pas de timestamp
                "${fileNameWithoutExt}_v${backupVersion.getFormattedVersion()}$fileExtension"
            }
            
            // Créer les métadonnées du fichier
            val fileMetadata = File().apply {
                name = versionedFileName
                parents = listOf(folderId)
            }
            
            // Créer le contenu du fichier
            val mediaContent = com.google.api.client.http.ByteArrayContent(mimeType, fileContent)
            
            // Uploader le fichier
            val uploadedFile = driveService!!.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size")
                .execute()
            
            Log.i(TAG, "Fichier uploadé avec succès: ${uploadedFile.name} (ID: ${uploadedFile.id}) dans le dossier $folderType")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'upload du fichier: ${localFile.name}", e)
            Log.e(TAG, "Type d'erreur: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message d'erreur: ${e.message}")
            Log.e(TAG, "Cause: ${e.cause?.message}")
            if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                Log.e(TAG, "Code d'erreur Google: ${e.statusCode}")
                Log.e(TAG, "Détails Google: ${e.details}")
            }
            return@withContext false
        }
    }
    
    /**
     * Types de dossiers de sauvegarde
     */
    enum class BackupFolderType {
        PROFILE, CONFIG, LOGS
    }
    
    /**
     * Sauvegarde les fichiers de configuration et de profil avec les chemins configurables
     */
    suspend fun backupConfigurationFiles(versionNumber: String = "0.3"): Boolean = withContext(Dispatchers.IO) {
        try {
            var allSuccess = true
            
            // Sauvegarder tous les logs avant la sauvegarde
            logManager.saveAllLogs()
            
            // Sauvegarder le fichier de profil
            val profileFile = AndroidFile(context.filesDir, "therapist_profile.json")
            if (profileFile.exists()) {
                val fileName = backupPathConfig.generateProfileFileName()
                val success = uploadFile(
                    profileFile,
                    fileName,
                    BackupFolderType.PROFILE,
                    "application/json",
                    versionNumber
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder le fichier de paramètres
            val settingsFile = AndroidFile(context.filesDir, "app_preferences.json")
            if (settingsFile.exists()) {
                val fileName = backupPathConfig.generateConfigFileName()
                val success = uploadFile(
                    settingsFile,
                    fileName,
                    BackupFolderType.CONFIG,
                    "application/json"
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs de tests
            val simpleLogFile = AndroidFile(context.filesDir, "test_results_simple.log")
            if (simpleLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("test_results_simple")
                val success = uploadFile(
                    simpleLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "text/plain"
                )
                if (!success) allSuccess = false
            }
            
            val verboseLogFile = AndroidFile(context.filesDir, "test_results_verbose.log")
            if (verboseLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("test_results_verbose")
                val success = uploadFile(
                    verboseLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "text/plain"
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs d'authentification
            val authLogFile = AndroidFile(context.filesDir, "auth_logs.json")
            if (authLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("auth_logs", "json")
                val success = uploadFile(
                    authLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json",
                    versionNumber
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs de diagnostics
            val diagnosticLogFile = AndroidFile(context.filesDir, "diagnostic_logs.json")
            if (diagnosticLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("diagnostic_logs", "json")
                val success = uploadFile(
                    diagnosticLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json"
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs de fichiers
            val fileDiagnosticLogFile = AndroidFile(context.filesDir, "file_diagnostic_logs.json")
            if (fileDiagnosticLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("file_diagnostic_logs", "json")
                val success = uploadFile(
                    fileDiagnosticLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json"
                )
                if (!success) allSuccess = false
            }
            
            Log.i(TAG, "Sauvegarde des fichiers de configuration: ${if (allSuccess) "Succès" else "Échec partiel"}")
            Log.i(TAG, "Structure utilisée: ${backupPathConfig.getConfigSummary()}")
            return@withContext allSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des fichiers de configuration", e)
            return@withContext false
        }
    }
    
    /**
     * Liste les fichiers dans tous les dossiers de sauvegarde
     */
    suspend fun listBackupFiles(): List<File> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext emptyList()
            }
            
            if (!ensureAllBackupFolders()) {
                Log.e(TAG, "Impossible de créer/récupérer les dossiers de sauvegarde")
                return@withContext emptyList()
            }
            
            val allFiles = mutableListOf<File>()
            
            // Lister les fichiers de chaque dossier
            listOf(
                profileFolderId to "Profil",
                configFolderId to "Configuration", 
                logsFolderId to "Logs"
            ).forEach { (folderId, folderName) ->
                if (folderId != null) {
                    val query = "'$folderId' in parents and trashed=false"
                    val result: FileList = driveService!!.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, name, size, createdTime, modifiedTime, parents)")
                        .setOrderBy("modifiedTime desc")
                        .execute()
                    
                    result.files?.forEach { file ->
                        // Ajouter le type de dossier au nom pour l'affichage
                        val displayName = "[$folderName] ${file.name}"
                        val fileWithType = file.clone().apply { name = displayName }
                        allFiles.add(fileWithType)
                    }
                }
            }
            
            // Trier par date de modification
            allFiles.sortByDescending { it.modifiedTime?.value }
            
            Log.i(TAG, "Trouvé ${allFiles.size} fichiers de sauvegarde")
            return@withContext allFiles
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la liste des fichiers de sauvegarde", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Obtient la configuration des chemins de sauvegarde
     */
    fun getBackupPathConfig(): BackupPathConfig {
        return backupPathConfig
    }
    
    /**
     * Met à jour la configuration des chemins de sauvegarde
     */
    fun updateBackupPathConfig(
        rootFolder: String? = null,
        profileFolder: String? = null,
        configFolder: String? = null,
        logsFolder: String? = null,
        useTimestamp: Boolean? = null,
        timestampFormat: String? = null
    ) {
        rootFolder?.let { backupPathConfig.rootFolder = it }
        profileFolder?.let { backupPathConfig.profileFolder = it }
        configFolder?.let { backupPathConfig.configFolder = it }
        logsFolder?.let { backupPathConfig.logsFolder = it }
        useTimestamp?.let { backupPathConfig.useTimestamp = it }
        timestampFormat?.let { backupPathConfig.timestampFormat = it }
        
        // Réinitialiser les IDs des dossiers pour forcer la recréation
        profileFolderId = null
        configFolderId = null
        logsFolderId = null
        
        Log.i(TAG, "Configuration des chemins de sauvegarde mise à jour")
    }
    
    /**
     * Télécharge un fichier depuis Google Drive
     */
    suspend fun downloadFile(fileId: String, localFile: AndroidFile): Boolean = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext false
            }
            
            val outputStream = localFile.outputStream()
            driveService!!.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            
            Log.i(TAG, "Fichier téléchargé avec succès: ${localFile.name}")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du téléchargement du fichier: $fileId", e)
            return@withContext false
        }
    }
    
    /**
     * Supprime un fichier de Google Drive
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                Log.e(TAG, "Service Google Drive non initialisé")
                return@withContext false
            }
            
            driveService!!.files().delete(fileId).execute()
            Log.i(TAG, "Fichier supprimé avec succès: $fileId")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression du fichier: $fileId", e)
            return@withContext false
        }
    }
    
    /**
     * Sauvegarde uniquement les logs de diagnostics et d'authentification
     */
    suspend fun backupLogsOnly(versionNumber: String = "0.3"): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Démarrage de la sauvegarde des logs uniquement")
            
            // Vérifier que le service est initialisé
            if (!isInitialized()) {
                Log.e(TAG, "Service Google Drive non initialisé pour la sauvegarde des logs")
                return@withContext false
            }
            
            // S'assurer que tous les dossiers existent
            if (!ensureAllBackupFolders()) {
                Log.e(TAG, "Impossible de créer/récupérer les dossiers de sauvegarde")
                return@withContext false
            }
            
            // Sauvegarder tous les logs
            logManager.saveAllLogs()
            
            var allSuccess = true
            
            // Sauvegarder les logs d'authentification
            val authLogFile = AndroidFile(context.filesDir, "auth_logs.json")
            if (authLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("auth_logs", "json")
                val success = uploadFile(
                    authLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json",
                    versionNumber
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs de diagnostics
            val diagnosticLogFile = AndroidFile(context.filesDir, "diagnostic_logs.json")
            if (diagnosticLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("diagnostic_logs", "json")
                val success = uploadFile(
                    diagnosticLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json"
                )
                if (!success) allSuccess = false
            }
            
            // Sauvegarder les logs de diagnostics de fichiers
            val fileDiagnosticLogFile = AndroidFile(context.filesDir, "file_diagnostic_logs.json")
            if (fileDiagnosticLogFile.exists()) {
                val fileName = backupPathConfig.generateLogFileName("file_diagnostic_logs", "json")
                val success = uploadFile(
                    fileDiagnosticLogFile,
                    fileName,
                    BackupFolderType.LOGS,
                    "application/json"
                )
                if (!success) allSuccess = false
            }
            
            if (allSuccess) {
                Log.i(TAG, "Sauvegarde des logs terminée avec succès")
            } else {
                Log.w(TAG, "Sauvegarde des logs terminée avec des erreurs")
            }
            
            return@withContext allSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des logs", e)
            Log.e(TAG, "Type d'erreur: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message d'erreur: ${e.message}")
            Log.e(TAG, "Cause: ${e.cause?.message}")
            if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                Log.e(TAG, "Code d'erreur Google: ${e.statusCode}")
                Log.e(TAG, "Détails Google: ${e.details}")
            }
            return@withContext false
        }
    }
    
    /**
     * Sauvegarde automatique périodique
     */
    suspend fun performAutomaticBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Démarrage de la sauvegarde automatique")
            
            // Vérifier que le service est initialisé
            if (!isInitialized()) {
                Log.e(TAG, "Service Google Drive non initialisé pour la sauvegarde automatique")
                return@withContext false
            }
            
            // Effectuer la sauvegarde
            val success = backupConfigurationFiles()
            
            if (success) {
                Log.i(TAG, "Sauvegarde automatique terminée avec succès")
            } else {
                Log.w(TAG, "Sauvegarde automatique terminée avec des erreurs")
            }
            
            return@withContext success
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde automatique", e)
            return@withContext false
        }
    }
}
