package com.frombeyond.r2sl.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.frombeyond.r2sl.utils.GoogleDriveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Service de sauvegarde automatique vers Google Drive
 * Effectue des sauvegardes périodiques des fichiers de configuration et de logs
 */
class AutoBackupService : Service() {
    
    companion object {
        private const val TAG = "AutoBackupService"
        private const val BACKUP_INTERVAL_HOURS = 6L // Sauvegarde toutes les 6 heures
        private val BACKUP_INTERVAL_MS = TimeUnit.HOURS.toMillis(BACKUP_INTERVAL_HOURS)
        
        const val ACTION_START_BACKUP = "com.frombeyond.r2sl.START_BACKUP"
        const val ACTION_STOP_BACKUP = "com.frombeyond.r2sl.STOP_BACKUP"
        const val ACTION_MANUAL_BACKUP = "com.frombeyond.r2sl.MANUAL_BACKUP"
    }
    
    private var googleDriveManager: GoogleDriveManager? = null
    private var isBackupRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AutoBackupService créé")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BACKUP -> startBackupService()
            ACTION_STOP_BACKUP -> stopBackupService()
            ACTION_MANUAL_BACKUP -> performManualBackup()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopBackupService()
        Log.i(TAG, "AutoBackupService détruit")
    }
    
    /**
     * Démarre le service de sauvegarde automatique
     */
    private fun startBackupService() {
        if (isBackupRunning) {
            Log.w(TAG, "Service de sauvegarde déjà en cours")
            return
        }
        
        Log.i(TAG, "Démarrage du service de sauvegarde automatique")
        isBackupRunning = true
        
        // Initialiser Google Drive Manager
        serviceScope.launch {
            initializeGoogleDriveManager()
        }
        
        // Démarrer la boucle de sauvegarde
        serviceScope.launch {
            while (isBackupRunning) {
                try {
                    performBackup()
                    delay(BACKUP_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur dans la boucle de sauvegarde", e)
                    delay(TimeUnit.MINUTES.toMillis(30)) // Attendre 30 minutes en cas d'erreur
                }
            }
        }
    }
    
    /**
     * Arrête le service de sauvegarde automatique
     */
    private fun stopBackupService() {
        Log.i(TAG, "Arrêt du service de sauvegarde automatique")
        isBackupRunning = false
        stopSelf()
    }
    
    /**
     * Effectue une sauvegarde manuelle
     */
    private fun performManualBackup() {
        Log.i(TAG, "Sauvegarde manuelle demandée")
        
        serviceScope.launch {
            initializeGoogleDriveManager()
            performBackup()
        }
    }
    
    /**
     * Initialise le gestionnaire Google Drive
     */
    private suspend fun initializeGoogleDriveManager() = withContext(Dispatchers.IO) {
        try {
            // Vérifier si l'utilisateur est connecté
            val account = GoogleSignIn.getLastSignedInAccount(this@AutoBackupService)
            if (account == null) {
                Log.w(TAG, "Aucun compte Google connecté pour la sauvegarde")
                return@withContext
            }
            
            // Créer les credentials
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf("https://www.googleapis.com/auth/drive.file")
            )
            credential.selectedAccount = account.account
            
            // Initialiser le gestionnaire Google Drive
            googleDriveManager = GoogleDriveManager(applicationContext).apply {
                initialize(credential)
            }
            
            Log.i(TAG, "Google Drive Manager initialisé avec succès")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de Google Drive Manager", e)
        }
    }
    
    /**
     * Effectue la sauvegarde des fichiers
     */
    private suspend fun performBackup() = withContext(Dispatchers.IO) {
        try {
            val driveManager = googleDriveManager
            if (driveManager == null) {
                Log.w(TAG, "Google Drive Manager non initialisé")
                return@withContext
            }
            
            if (!driveManager.isInitialized()) {
                Log.w(TAG, "Google Drive Manager non prêt")
                return@withContext
            }
            
            Log.i(TAG, "Début de la sauvegarde automatique")
            
            val success = driveManager.performAutomaticBackup()
            
            if (success) {
                Log.i(TAG, "Sauvegarde automatique terminée avec succès")
            } else {
                Log.w(TAG, "Sauvegarde automatique échouée")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde automatique", e)
        }
    }
}
