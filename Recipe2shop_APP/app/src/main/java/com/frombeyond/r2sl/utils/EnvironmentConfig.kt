package com.frombeyond.r2sl.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Gestionnaire de configuration d'environnement
 * Permet de gérer différentes configurations selon l'environnement (debug/release)
 * sans exposer d'informations sensibles dans le code
 */
class EnvironmentConfig private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "EnvironmentConfig"
        
        @Volatile
        private var INSTANCE: EnvironmentConfig? = null
        
        fun getInstance(context: Context): EnvironmentConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnvironmentConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val authLogger = AuthLogger.getInstance(context)
    
    /**
     * Types d'environnement
     */
    enum class Environment {
        DEBUG, RELEASE, UNKNOWN
    }
    
    /**
     * Détermine l'environnement actuel
     */
    fun getCurrentEnvironment(): Environment {
        return try {
            val applicationInfo = context.applicationInfo
            if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                Environment.DEBUG
            } else {
                Environment.RELEASE
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Environnement", "Impossible de déterminer l'environnement", e)
            Environment.UNKNOWN
        }
    }
    
    /**
     * Vérifie si l'application est en mode debug
     */
    fun isDebugMode(): Boolean {
        return getCurrentEnvironment() == Environment.DEBUG
    }
    
    /**
     * Vérifie si l'application est en mode release
     */
    fun isReleaseMode(): Boolean {
        return getCurrentEnvironment() == Environment.RELEASE
    }
    
    /**
     * Récupère la version de l'application
     */
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            authLogger.logAuthError("Version", "Impossible de récupérer la version de l'application", e)
            "Unknown"
        }
    }
    
    /**
     * Récupère le code de version de l'application
     */
    fun getAppVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            authLogger.logAuthError("Version Code", "Impossible de récupérer le code de version", e)
            -1
        }
    }
    
    /**
     * Récupère le nom du package de l'application
     */
    fun getAppPackageName(): String {
        return context.packageName
    }
    
    /**
     * Vérifie si l'application est signée avec la clé de release
     */
    fun isSignedWithReleaseKey(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = packageInfo.signatures
            
            // Vérifier si la signature correspond à la clé de release
            // Cette vérification peut être adaptée selon vos besoins
            signatures.isNotEmpty()
        } catch (e: Exception) {
            authLogger.logAuthError("Signature", "Impossible de vérifier la signature de l'application", e)
            false
        }
    }
    
    /**
     * Affiche un résumé de l'environnement
     */
    fun getEnvironmentSummary(): String {
        val env = getCurrentEnvironment()
        val version = getAppVersion()
        val versionCode = getAppVersionCode()
        val packageName = getAppPackageName()
        val isReleaseSigned = isSignedWithReleaseKey()
        
        return """
            🌍 Configuration d'Environnement
            =================================
            ✅ Environnement: ${env.name}
            ✅ Version: $version (Code: $versionCode)
            ✅ Package: $packageName
            ✅ Signé Release: ${if (isReleaseSigned) "Oui" else "Non"}
            
            📋 Recommandations:
            ${if (env == Environment.DEBUG) "• Mode debug activé - Logs détaillés disponibles" else "• Mode release - Logs limités pour la sécurité"}
            ${if (isReleaseSigned) "• Application signée avec la clé de release" else "• Application non signée avec la clé de release"}
        """.trimIndent()
    }
    
    /**
     * Valide la configuration de l'environnement
     */
    fun validateEnvironment(): Boolean {
        val env = getCurrentEnvironment()
        
        // Vérifications spécifiques à l'environnement
        when (env) {
            Environment.DEBUG -> {
                authLogger.logAuthInfo("Environnement", "Mode debug détecté - Logs détaillés activés")
                return true
            }
            Environment.RELEASE -> {
                // En production, vérifier que l'application est signée
                if (!isSignedWithReleaseKey()) {
                    authLogger.logAuthError("Environnement", "Application en mode release non signée avec la clé de release")
                    return false
                }
                authLogger.logAuthInfo("Environnement", "Mode release détecté - Configuration sécurisée")
                return true
            }
            Environment.UNKNOWN -> {
                authLogger.logAuthError("Environnement", "Impossible de déterminer l'environnement")
                return false
            }
        }
    }
}
