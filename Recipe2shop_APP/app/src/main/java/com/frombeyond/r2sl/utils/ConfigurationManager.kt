package com.frombeyond.r2sl.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.analytics.FirebaseAnalytics
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*

/**
 * Gestionnaire de configuration automatique pour Firebase et Google API
 * Vérifie et configure automatiquement les services Google
 */
class ConfigurationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ConfigurationManager"
        private const val GOOGLE_SERVICES_FILE = "google-services.json"
        private const val FIREBASE_APP_ID = "1:123456789:android:abcdef123456"
    }
    
    private val googleApiAvailability = GoogleApiAvailability.getInstance()
    
    /**
     * Vérifie et configure automatiquement tous les services Google
     */
    fun checkAndConfigureAll(): ConfigurationResult {
        val result = ConfigurationResult()
        
        try {
            // 1. Vérifier Google Play Services
            result.googlePlayServices = checkGooglePlayServices()
            
            // 2. Vérifier Firebase
            result.firebase = checkFirebaseConfiguration()
            
            // 3. Vérifier Google Services JSON
            result.googleServicesJson = checkGoogleServicesJson()
            
            // 4. Obtenir le SHA-1
            result.sha1Fingerprint = getSHA1Fingerprint()
            
            // 5. Vérifier les permissions
            result.permissions = checkPermissions()
            
            // 6. Vérifier la configuration OAuth
            result.oauthConfig = checkOAuthConfiguration()
            
            result.isValid = result.googlePlayServices && result.firebase && result.googleServicesJson
            
            Log.i(TAG, "Configuration check completed: ${if (result.isValid) "SUCCESS" else "FAILED"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during configuration check", e)
            result.error = e.message ?: "Unknown error"
        }
        
        return result
    }
    
    /**
     * Vérifie si Google Play Services est disponible et à jour
     */
    private fun checkGooglePlayServices(): Boolean {
        return try {
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    Log.i(TAG, "Google Play Services: Available")
                    true
                }
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "Google Play Services: Update required")
                    false
                }
                else -> {
                    Log.e(TAG, "Google Play Services: Not available (Code: $resultCode)")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
            false
        }
    }
    
    /**
     * Vérifie la configuration Firebase
     */
    private fun checkFirebaseConfiguration(): Boolean {
        return try {
            // Vérifier si Firebase est initialisé
            val firebaseApp = FirebaseApp.getInstance()
            
            // Vérifier Firebase Auth
            val auth = FirebaseAuth.getInstance()
            
            // Vérifier Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            
            Log.i(TAG, "Firebase: Properly configured")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firebase configuration", e)
            false
        }
    }
    
    /**
     * Vérifie si le fichier google-services.json existe et est valide
     */
    private fun checkGoogleServicesJson(): Boolean {
        return try {
            // Chercher le fichier dans le dossier app/ (emplacement correct)
            val appDir = File(context.filesDir.parent, "app")
            val file = File(appDir, GOOGLE_SERVICES_FILE)
            
            if (!file.exists()) {
                Log.e(TAG, "google-services.json: File not found at ${file.absolutePath}")
                return false
            }
            
            // Vérifier que le fichier n'est pas vide
            if (file.length() == 0L) {
                Log.e(TAG, "google-services.json: File is empty")
                return false
            }
            
            // Vérifier que le fichier contient des données JSON valides
            val content = file.readText()
            if (!content.contains("project_info") || !content.contains("client")) {
                Log.e(TAG, "google-services.json: Invalid JSON structure")
                return false
            }
            
            Log.i(TAG, "google-services.json: Valid file found at ${file.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking google-services.json", e)
            false
        }
    }
    
    /**
     * Obtient le SHA-1 fingerprint de l'application
     */
    private fun getSHA1Fingerprint(): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            
            val signatures = packageInfo.signatures
            if (signatures.isNotEmpty()) {
                val signature = signatures[0]
                val md = MessageDigest.getInstance("SHA1")
                val digest = md.digest(signature.toByteArray())
                
                val sha1 = digest.joinToString(":") { "%02x".format(it) }
                Log.i(TAG, "SHA-1 Fingerprint: $sha1")
                sha1
            } else {
                Log.e(TAG, "No signatures found")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SHA-1 fingerprint", e)
            null
        }
    }
    
    /**
     * Vérifie les permissions nécessaires
     */
    private fun checkPermissions(): Map<String, Boolean> {
        val permissions = mapOf(
            "INTERNET" to context.checkSelfPermission(android.Manifest.permission.INTERNET),
            "ACCESS_NETWORK_STATE" to context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE),
            "WRITE_EXTERNAL_STORAGE" to context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            "READ_EXTERNAL_STORAGE" to context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        )
        
        permissions.forEach { (permission, granted) ->
            Log.i(TAG, "Permission $permission: ${if (granted == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        }
        
        return permissions.mapValues { it.value == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * Vérifie la configuration OAuth
     */
    private fun checkOAuthConfiguration(): Boolean {
        return try {
            // Vérifier que les dépendances Google sont présentes
            val hasGoogleAuth = try {
                Class.forName("com.google.android.gms.auth.api.signin.GoogleSignIn")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            val hasGoogleDrive = try {
                Class.forName("com.google.api.services.drive.Drive")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            val hasGoogleApiClient = try {
                Class.forName("com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            val oauthReady = hasGoogleAuth && hasGoogleDrive && hasGoogleApiClient
            
            Log.i(TAG, "OAuth Configuration: Google Auth=$hasGoogleAuth, Google Drive=$hasGoogleDrive, Google API Client=$hasGoogleApiClient")
            
            oauthReady
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking OAuth configuration", e)
            false
        }
    }
    
    /**
     * Génère un rapport de configuration détaillé
     */
    fun generateConfigurationReport(): String {
        val result = checkAndConfigureAll()
        
        return buildString {
            appendLine("=== RAPPORT DE CONFIGURATION THERAPIA ===")
            appendLine("Timestamp: ${Date()}")
            appendLine()
            
            appendLine("1. GOOGLE PLAY SERVICES:")
            appendLine("   Status: ${if (result.googlePlayServices) "✅ DISPONIBLE" else "❌ NON DISPONIBLE"}")
            appendLine()
            
            appendLine("2. FIREBASE:")
            appendLine("   Status: ${if (result.firebase) "✅ CONFIGURÉ" else "❌ NON CONFIGURÉ"}")
            appendLine()
            
            appendLine("3. GOOGLE SERVICES JSON:")
            appendLine("   Status: ${if (result.googleServicesJson) "✅ FICHIER VALIDE" else "❌ FICHIER INVALIDE"}")
            appendLine()
            
            appendLine("4. SHA-1 FINGERPRINT:")
            appendLine("   Value: ${result.sha1Fingerprint ?: "❌ NON DISPONIBLE"}")
            appendLine()
            
            appendLine("5. PERMISSIONS:")
            result.permissions.forEach { (permission, granted) ->
                appendLine("   $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
            }
            appendLine()
            
            appendLine("6. OAUTH CONFIGURATION:")
            appendLine("   Status: ${if (result.oauthConfig) "✅ PRÊT" else "❌ NON CONFIGURÉ"}")
            appendLine()
            
            appendLine("7. RÉSULTAT GLOBAL:")
            appendLine("   Status: ${if (result.isValid) "✅ CONFIGURATION VALIDE" else "❌ CONFIGURATION INVALIDE"}")
            
            if (result.error != null) {
                appendLine()
                appendLine("8. ERREURS:")
                appendLine("   ${result.error}")
            }
            
            appendLine()
            appendLine("=== INSTRUCTIONS DE CONFIGURATION ===")
            if (!result.googlePlayServices) {
                appendLine("• Mettre à jour Google Play Services sur l'appareil")
            }
            if (!result.firebase) {
                appendLine("• Vérifier la configuration Firebase dans build.gradle")
            }
            if (!result.googleServicesJson) {
                appendLine("• Placer google-services.json dans le dossier app/")
            }
            if (result.sha1Fingerprint == null) {
                appendLine("• Vérifier la signature de l'application")
            }
            if (result.permissions.values.any { !it }) {
                appendLine("• Accorder les permissions manquantes")
            }
            if (!result.oauthConfig) {
                appendLine("• Vérifier les dépendances Google dans build.gradle")
            }
        }
    }
    
    /**
     * Résultat de la vérification de configuration
     */
    data class ConfigurationResult(
        var googlePlayServices: Boolean = false,
        var firebase: Boolean = false,
        var googleServicesJson: Boolean = false,
        var sha1Fingerprint: String? = null,
        var permissions: Map<String, Boolean> = emptyMap(),
        var oauthConfig: Boolean = false,
        var isValid: Boolean = false,
        var error: String? = null
    )
}
