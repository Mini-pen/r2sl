package com.frombeyond.r2sl.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.frombeyond.r2sl.utils.FirebaseConfigManager
import com.frombeyond.r2sl.utils.EnvironmentConfig

/**
 * Gestionnaire de diagnostic pour l'authentification
 * Vérifie automatiquement la configuration et identifie les problèmes
 */
class AuthDiagnostic(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthDiagnostic"
    }
    
    private val authLogger = AuthLogger.getInstance(context)
    private val configManager = FirebaseConfigManager.getInstance(context)
    private val envConfig = EnvironmentConfig.getInstance(context)
    
    /**
     * Lance un diagnostic complet de l'authentification
     */
    fun runFullDiagnostic(): DiagnosticResult {
        authLogger.logAuthStep("Démarrage du diagnostic complet")
        
        val results = mutableListOf<DiagnosticItem>()
        
        // 1. Vérifier Google Play Services
        results.add(checkGooglePlayServices())
        
        // 2. Vérifier la configuration Firebase
        results.add(checkFirebaseConfiguration())
        
        // 3. Vérifier la configuration Google Sign-In
        results.add(checkGoogleSignInConfiguration())
        
        // 4. Vérifier les permissions
        results.add(checkPermissions())
        
        // 5. Vérifier la connectivité réseau
        results.add(checkNetworkConnectivity())
        
        // 6. Vérifier la configuration d'environnement
        results.add(checkEnvironmentConfiguration())
        
        // Analyser les résultats
        val hasErrors = results.any { it.status == DiagnosticStatus.ERROR }
        val hasWarnings = results.any { it.status == DiagnosticStatus.WARNING }
        
        val overallStatus = when {
            hasErrors -> DiagnosticStatus.ERROR
            hasWarnings -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.SUCCESS
        }
        
        val result = DiagnosticResult(
            overallStatus = overallStatus,
            items = results,
            summary = generateSummary(results)
        )
        
        // Logger le résultat final
        when (overallStatus) {
            DiagnosticStatus.SUCCESS -> authLogger.logAuthInfo("Diagnostic réussi - Aucun problème détecté")
            DiagnosticStatus.WARNING -> authLogger.logAuthInfo("Diagnostic terminé avec des avertissements", result.summary)
            DiagnosticStatus.ERROR -> authLogger.logAuthError("Diagnostic échoué", result.summary)
        }
        
        return result
    }
    
    /**
     * Vérifier Google Play Services
     */
    private fun checkGooglePlayServices(): DiagnosticItem {
        return try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            when (resultCode) {
                com.google.android.gms.common.ConnectionResult.SUCCESS -> {
                    authLogger.logAuthStep("Google Play Services", "Disponible et à jour")
                    DiagnosticItem(
                        name = "Google Play Services",
                        status = DiagnosticStatus.SUCCESS,
                        message = "Disponible et à jour",
                        details = "Version: ${googleApiAvailability.getApkVersion(context)}"
                    )
                }
                else -> {
                    val errorString = googleApiAvailability.getErrorString(resultCode)
                    authLogger.logAuthError("Google Play Services", "Non disponible: $errorString")
                    DiagnosticItem(
                        name = "Google Play Services",
                        status = DiagnosticStatus.ERROR,
                        message = "Non disponible: $errorString",
                        details = "Code d'erreur: $resultCode"
                    )
                }
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Google Play Services", "Erreur lors de la vérification", e)
            DiagnosticItem(
                name = "Google Play Services",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Vérifier la configuration Firebase
     */
    private fun checkFirebaseConfiguration(): DiagnosticItem {
        return try {
            // Vérifier si Firebase est initialisé
            if (!FirebaseApp.getApps(context).isEmpty()) {
                val firebaseApp = FirebaseApp.getInstance()
                val options = firebaseApp.options
                
                authLogger.logAuthStep("Firebase", "Initialisé avec succès")
                DiagnosticItem(
                    name = "Configuration Firebase",
                    status = DiagnosticStatus.SUCCESS,
                    message = "Firebase initialisé avec succès",
                    details = "Project ID: ${options.projectId}, Storage Bucket: ${options.storageBucket}"
                )
            } else {
                authLogger.logAuthError("Firebase", "Firebase non initialisé")
                DiagnosticItem(
                    name = "Configuration Firebase",
                    status = DiagnosticStatus.ERROR,
                    message = "Firebase non initialisé",
                    details = "Aucune application Firebase trouvée"
                )
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Firebase", "Erreur lors de la vérification Firebase", e)
            DiagnosticItem(
                name = "Configuration Firebase",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification Firebase",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Vérifier la configuration Google Sign-In
     */
    private fun checkGoogleSignInConfiguration(): DiagnosticItem {
        return try {
            // Utiliser le gestionnaire de configuration centralisé
            val config = configManager.getFirebaseConfig()
            if (config == null) {
                return DiagnosticItem(
                    name = "Configuration Google Sign-In",
                    status = DiagnosticStatus.ERROR,
                    message = "Configuration Firebase non disponible",
                    details = "Impossible de lire le fichier google-services.json"
                )
            }
            
            // Vérifier la cohérence avec le package name de l'application
            val appPackageName = context.packageName
            
            if (config.packageName == appPackageName) {
                authLogger.logAuthStep("Google Sign-In", "Configuration cohérente")
                DiagnosticItem(
                    name = "Configuration Google Sign-In",
                    status = DiagnosticStatus.SUCCESS,
                    message = "Configuration cohérente",
                    details = "Package: ${config.packageName}, Client ID: ${config.clientId.take(20)}..."
                )
            } else {
                authLogger.logConfigError("Package Name", appPackageName, config.packageName)
                DiagnosticItem(
                    name = "Configuration Google Sign-In",
                    status = DiagnosticStatus.ERROR,
                    message = "Incohérence du package name",
                    details = "App: $appPackageName, Config: ${config.packageName}"
                )
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Google Sign-In", "Erreur lors de la vérification de la configuration", e)
            DiagnosticItem(
                name = "Configuration Google Sign-In",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification de la configuration",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Vérifier les permissions
     */
    private fun checkPermissions(): DiagnosticItem {
        return try {
            val internetPermission = context.checkSelfPermission(android.Manifest.permission.INTERNET)
            val networkStatePermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
            
            if (internetPermission == PackageManager.PERMISSION_GRANTED && 
                networkStatePermission == PackageManager.PERMISSION_GRANTED) {
                authLogger.logAuthStep("Permissions", "Toutes les permissions nécessaires accordées")
                DiagnosticItem(
                    name = "Permissions",
                    status = DiagnosticStatus.SUCCESS,
                    message = "Toutes les permissions nécessaires accordées",
                    details = "INTERNET et ACCESS_NETWORK_STATE accordées"
                )
            } else {
                authLogger.logAuthError("Permissions", "Permissions manquantes")
                DiagnosticItem(
                    name = "Permissions",
                    status = DiagnosticStatus.ERROR,
                    message = "Permissions manquantes",
                    details = "INTERNET: ${if (internetPermission == PackageManager.PERMISSION_GRANTED) "OK" else "MANQUANT"}, " +
                            "ACCESS_NETWORK_STATE: ${if (networkStatePermission == PackageManager.PERMISSION_GRANTED) "OK" else "MANQUANT"}"
                )
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Permissions", "Erreur lors de la vérification des permissions", e)
            DiagnosticItem(
                name = "Permissions",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification des permissions",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Vérifier la connectivité réseau
     */
    private fun checkNetworkConnectivity(): DiagnosticItem {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            
            if (networkInfo?.isConnected == true) {
                authLogger.logAuthStep("Connectivité réseau", "Réseau disponible")
                DiagnosticItem(
                    name = "Connectivité réseau",
                    status = DiagnosticStatus.SUCCESS,
                    message = "Réseau disponible",
                    details = "Type: ${networkInfo.typeName}, Connecté: ${networkInfo.isConnected}"
                )
            } else {
                authLogger.logAuthError("Connectivité réseau", "Aucun réseau disponible")
                DiagnosticItem(
                    name = "Connectivité réseau",
                    status = DiagnosticStatus.ERROR,
                    message = "Aucun réseau disponible",
                    details = "Vérifiez votre connexion internet"
                )
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Connectivité réseau", "Erreur lors de la vérification réseau", e)
            DiagnosticItem(
                name = "Connectivité réseau",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification réseau",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Vérifier la configuration d'environnement
     */
    private fun checkEnvironmentConfiguration(): DiagnosticItem {
        return try {
            val env = envConfig.getCurrentEnvironment()
            val isValid = envConfig.validateEnvironment()
            
            if (isValid) {
                authLogger.logAuthStep("Environnement", "Configuration d'environnement valide")
                DiagnosticItem(
                    name = "Configuration d'Environnement",
                    status = DiagnosticStatus.SUCCESS,
                    message = "Configuration d'environnement valide",
                    details = "Environnement: ${env.name}, Version: ${envConfig.getAppVersion()}"
                )
            } else {
                authLogger.logAuthError("Environnement", "Configuration d'environnement invalide")
                DiagnosticItem(
                    name = "Configuration d'Environnement",
                    status = DiagnosticStatus.ERROR,
                    message = "Configuration d'environnement invalide",
                    details = "Environnement: ${env.name}, Vérifiez la signature de l'application"
                )
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Environnement", "Erreur lors de la vérification de l'environnement", e)
            DiagnosticItem(
                name = "Configuration d'Environnement",
                status = DiagnosticStatus.ERROR,
                message = "Erreur lors de la vérification de l'environnement",
                details = e.message ?: "Exception inconnue"
            )
        }
    }
    
    /**
     * Générer un résumé des résultats
     */
    private fun generateSummary(items: List<DiagnosticItem>): String {
        val errorCount = items.count { it.status == DiagnosticStatus.ERROR }
        val warningCount = items.count { it.status == DiagnosticStatus.WARNING }
        val successCount = items.count { it.status == DiagnosticStatus.SUCCESS }
        
        return "Résumé: $successCount succès, $warningCount avertissements, $errorCount erreurs"
    }
    
    /**
     * Résultat du diagnostic
     */
    data class DiagnosticResult(
        val overallStatus: DiagnosticStatus,
        val items: List<DiagnosticItem>,
        val summary: String
    )
    
    /**
     * Élément de diagnostic individuel
     */
    data class DiagnosticItem(
        val name: String,
        val status: DiagnosticStatus,
        val message: String,
        val details: String
    )
    
    /**
     * Statut du diagnostic
     */
    enum class DiagnosticStatus {
        SUCCESS, WARNING, ERROR
    }
}
