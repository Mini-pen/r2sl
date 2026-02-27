package com.frombeyond.r2sl.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

/**
 * Vérificateur de permissions pour Google Drive
 * Vérifie et met à jour les permissions demandées
 */
class PermissionChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionChecker"
        private val REQUIRED_SCOPES = listOf(
            DriveScopes.DRIVE_FILE,
            "https://www.googleapis.com/auth/drive.metadata.readonly"
        )
    }
    
    /**
     * Vérifie si toutes les permissions Google Drive sont accordées
     */
    fun checkGoogleDrivePermissions(): PermissionStatus {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                PermissionStatus(
                    isGranted = false,
                    missingScopes = REQUIRED_SCOPES,
                    message = "Aucun compte Google connecté"
                )
            } else {
                val grantedScopes = account.grantedScopes ?: emptySet<Scope>()
                val missingScopes = REQUIRED_SCOPES.filter { !grantedScopes.contains(Scope(it)) }
                
                PermissionStatus(
                    isGranted = missingScopes.isEmpty(),
                    missingScopes = missingScopes,
                    message = if (missingScopes.isEmpty()) {
                        "Toutes les permissions Google Drive sont accordées"
                    } else {
                        "Permissions manquantes: ${missingScopes.joinToString(", ")}"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification des permissions", e)
            PermissionStatus(
                isGranted = false,
                missingScopes = REQUIRED_SCOPES,
                message = "Erreur: ${e.message}"
            )
        }
    }
    
    /**
     * Crée un GoogleSignInClient avec tous les scopes requis
     */
    fun createGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.metadata.readonly"))
            .build()
        
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Vérifie les permissions Android requises
     */
    fun checkAndroidPermissions(): Map<String, Boolean> {
        val permissions = mapOf(
            "INTERNET" to context.checkSelfPermission(android.Manifest.permission.INTERNET),
            "ACCESS_NETWORK_STATE" to context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE),
            "WRITE_EXTERNAL_STORAGE" to context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            "READ_EXTERNAL_STORAGE" to context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        )
        
        return permissions.mapValues { it.value == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * Génère un rapport de permissions complet
     */
    fun generatePermissionReport(): String {
        val googleDriveStatus = checkGoogleDrivePermissions()
        val androidPermissions = checkAndroidPermissions()
        
        return buildString {
            appendLine("=== RAPPORT DE PERMISSIONS R2SL ===")
            appendLine("Timestamp: ${java.util.Date()}")
            appendLine()
            
            appendLine("1. PERMISSIONS GOOGLE DRIVE:")
            appendLine("   Status: ${if (googleDriveStatus.isGranted) "✅ ACCORDÉES" else "❌ MANQUANTES"}")
            appendLine("   Message: ${googleDriveStatus.message}")
            if (googleDriveStatus.missingScopes.isNotEmpty()) {
                appendLine("   Scopes manquants:")
                googleDriveStatus.missingScopes.forEach { scope ->
                    appendLine("     - $scope")
                }
            }
            appendLine()
            
            appendLine("2. PERMISSIONS ANDROID:")
            androidPermissions.forEach { (permission, granted) ->
                appendLine("   $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
            }
            appendLine()
            
            appendLine("3. RECOMMANDATIONS:")
            if (!googleDriveStatus.isGranted) {
                appendLine("   • Reconnectez-vous avec Google pour accorder les nouvelles permissions")
                appendLine("   • Vérifiez la configuration OAuth dans Google Cloud Console")
            }
            if (androidPermissions.values.any { !it }) {
                appendLine("   • Accorder les permissions Android manquantes")
            }
            if (googleDriveStatus.isGranted && androidPermissions.values.all { it }) {
                appendLine("   • Toutes les permissions sont correctement configurées !")
            }
        }
    }
    
    /**
     * Status des permissions Google Drive
     */
    data class PermissionStatus(
        val isGranted: Boolean,
        val missingScopes: List<String>,
        val message: String
    )
}
