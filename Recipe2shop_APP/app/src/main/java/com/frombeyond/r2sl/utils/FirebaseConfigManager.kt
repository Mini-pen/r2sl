package com.frombeyond.r2sl.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Gestionnaire centralisé de la configuration Firebase
 * Utilise des valeurs par défaut sécurisées pour éviter les plantages
 * Évite d'avoir des informations sensibles en dur dans le code
 */
class FirebaseConfigManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "FirebaseConfigManager"
        
        @Volatile
        private var INSTANCE: FirebaseConfigManager? = null
        
        fun getInstance(context: Context): FirebaseConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val authLogger = AuthLogger.getInstance(context)
    private var configCache: FirebaseConfig? = null
    
    /**
     * Configuration Firebase avec des valeurs par défaut sécurisées
     */
    data class FirebaseConfig(
        val projectId: String,
        val projectNumber: String,
        val storageBucket: String,
        val packageName: String,
        val clientId: String,
        val apiKey: String,
        val mobilesdkAppId: String
    )
    
    /**
     * Récupère la configuration Firebase complète
     */
    fun getFirebaseConfig(): FirebaseConfig? {
        if (configCache != null) {
            return configCache
        }
        
        return try {
            // Essayer de lire depuis google-services.json d'abord
            val config = readConfigFromGoogleServices() ?: createDefaultConfig()
            configCache = config
            authLogger.logAuthStep("Configuration Firebase", "Configuration chargée avec succès")
            config
        } catch (e: Exception) {
            authLogger.logAuthError("Configuration Firebase", "Impossible de charger la configuration", e)
            null
        }
    }
    
    /**
     * Récupère le Client ID Google pour l'authentification
     * Priorité : BuildConfig > google-services.json > valeur par défaut
     */
    fun getGoogleClientId(): String? {
        // 1. Essayer BuildConfig d'abord (priorité)
        val buildConfigClientId = getClientIdFromBuildConfig()
        if (!buildConfigClientId.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID récupéré depuis BuildConfig: ${buildConfigClientId.take(20)}...")
            return buildConfigClientId
        }
        
        // 2. Fallback vers google-services.json
        val configClientId = getFirebaseConfig()?.clientId
        if (!configClientId.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID récupéré depuis google-services.json: ${configClientId.take(20)}...")
            return configClientId
        }
        
        // 3. Aucun Client ID trouvé
        authLogger.logAuthError("Client ID", "Aucun Client ID valide trouvé. Vérifiez la configuration dans build.gradle.kts")
        return null
    }
    
    /**
     * Récupère le Project ID Firebase
     */
    fun getProjectId(): String? {
        return getFirebaseConfig()?.projectId
    }
    
    /**
     * Récupère le Package Name configuré
     */
    fun getPackageName(): String? {
        return getFirebaseConfig()?.packageName
    }
    
    /**
     * Vérifie la cohérence entre le package name de l'app et celui de la config
     */
    fun validatePackageName(): Boolean {
        val configPackageName = getPackageName()
        val appPackageName = context.packageName
        
        return if (configPackageName == appPackageName) {
            authLogger.logAuthStep("Validation Package", "Package name cohérent: $appPackageName")
            true
        } else {
            authLogger.logConfigError("Package Name", appPackageName, configPackageName ?: "NULL")
            false
        }
    }
    
    /**
     * Vérifie que la configuration est complète et valide
     */
    fun validateConfiguration(): Boolean {
        val config = getFirebaseConfig() ?: return false
        
        val requiredFields = listOf(
            "projectId" to config.projectId,
            "clientId" to config.clientId,
            "apiKey" to config.apiKey,
            "packageName" to config.packageName
        )
        
        val missingFields = requiredFields.filter { it.second.isNullOrBlank() }
        
        if (missingFields.isNotEmpty()) {
            val missingNames = missingFields.map { it.first }
            authLogger.logAuthError("Configuration Firebase", "Champs manquants: $missingNames")
            return false
        }
        
        authLogger.logAuthStep("Configuration Firebase", "Configuration validée avec succès")
        return true
    }
    
    /**
     * Lit la configuration depuis google-services.json
     */
    private fun readConfigFromGoogleServices(): FirebaseConfig? {
        return try {
            // Le fichier google-services.json est traité par le plugin Gradle
            // et les valeurs sont disponibles via FirebaseApp
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            val options = firebaseApp.options
            
            val packageName = context.packageName
            
            // Récupérer les valeurs depuis FirebaseOptions
            val projectId = options.projectId ?: "therapia-app"
            // * API Key depuis BuildConfig (chargé depuis local.properties) ou FirebaseOptions
            val apiKey = getApiKeyFromBuildConfig() ?: options.apiKey ?: ""
            val applicationId = options.applicationId ?: "1:457686555916:android:fdacd643758143cd00bd29"
            val storageBucket = options.storageBucket ?: "therapia-app.firebasestorage.app"
            
            // Pour le client ID, on doit le récupérer depuis google-services.json
            // car FirebaseOptions ne l'expose pas directement
            val clientId = getClientIdFromGoogleServices()
            
            FirebaseConfig(
                projectId = projectId,
                projectNumber = "457686555916", // Récupéré depuis google-services.json
                storageBucket = storageBucket,
                packageName = packageName,
                clientId = clientId,
                apiKey = apiKey,
                mobilesdkAppId = applicationId
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Google Services", "Impossible de lire google-services.json", e)
            null
        }
    }
    
    /**
     * Récupère le Client ID depuis google-services.json ou BuildConfig selon le mode
     */
    private fun getClientIdFromGoogleServices(): String {
        return try {
            // D'abord, essayer d'utiliser le client ID depuis BuildConfig (si défini)
            // Cela permet d'utiliser des clients différents pour debug et release
            val buildConfigClientId = getClientIdFromBuildConfig()
            if (!buildConfigClientId.isNullOrEmpty() && buildConfigClientId != "YOUR_DEBUG_CLIENT_ID_HERE" && buildConfigClientId != "YOUR_RELEASE_CLIENT_ID_HERE") {
                authLogger.logAuthInfo("Client ID", "Utilisation du Client ID depuis BuildConfig")
                return buildConfigClientId
            }
            
            // Sinon, lire depuis google-services.json
            val inputStream = context.assets.open("google-services.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            
            // Parser le JSON pour extraire le client_id
            val jsonObject = org.json.JSONObject(jsonString)
            val clientArray = jsonObject.getJSONArray("client")
            val firstClient = clientArray.getJSONObject(0)
            val oauthClientArray = firstClient.getJSONArray("oauth_client")
            
            // Essayer de trouver le client ID approprié selon le build type
            val isDebug = com.frombeyond.r2sl.utils.EnvironmentConfig.getInstance(context).isDebugMode()
            
            // Parcourir tous les clients OAuth pour trouver celui qui correspond
            for (i in 0 until oauthClientArray.length()) {
                val oauthClient = oauthClientArray.getJSONObject(i)
                val clientId = oauthClient.getString("client_id")
                // Si on a plusieurs clients, on pourrait les différencier par d'autres critères
                // Pour l'instant, on prend le premier
                if (i == 0) {
                    authLogger.logAuthInfo("Client ID", "Utilisation du Client ID depuis google-services.json")
                    return clientId
                }
            }
            
            // Fallback
            authLogger.logAuthError("Client ID", "Aucun Client ID trouvé dans google-services.json")
            ""
        } catch (e: Exception) {
            authLogger.logAuthError("Client ID", "Impossible de lire le Client ID depuis google-services.json", e)
            // Fallback vers BuildConfig ou valeur par défaut
            getClientIdFromBuildConfig() ?: ""
        }
    }
    
    /**
     * Récupère le Client ID depuis BuildConfig (défini dans build.gradle.kts)
     */
    private fun getClientIdFromBuildConfig(): String? {
        return try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val isDebug = buildConfigClass.getField("DEBUG").getBoolean(null)
            
            if (isDebug) {
                val debugClientId = buildConfigClass.getField("GOOGLE_CLIENT_ID_DEBUG").get(null) as? String
                debugClientId?.takeIf { it.isNotEmpty() && it != "YOUR_DEBUG_CLIENT_ID_HERE" }
            } else {
                val releaseClientId = buildConfigClass.getField("GOOGLE_CLIENT_ID_RELEASE").get(null) as? String
                releaseClientId?.takeIf { it.isNotEmpty() && it != "YOUR_RELEASE_CLIENT_ID_HERE" }
            }
        } catch (e: Exception) {
            // BuildConfig non disponible ou champs non définis
            null
        }
    }
    
    /**
     * Récupère l'API Key depuis BuildConfig (chargé depuis local.properties)
     */
    private fun getApiKeyFromBuildConfig(): String? {
        return try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val apiKey = buildConfigClass.getField("GOOGLE_API_KEY").get(null) as? String
            apiKey?.takeIf { it.isNotEmpty() && it != "null" }
        } catch (e: Exception) {
            // BuildConfig non disponible ou champ non défini
            null
        }
    }
    
    /**
     * Crée une configuration par défaut sécurisée
     */
    private fun createDefaultConfig(): FirebaseConfig {
        // Utiliser le package name de l'application
        val packageName = context.packageName
        
        // Configuration par défaut pour TherapIA
        // * API Key depuis BuildConfig (chargé depuis local.properties)
        val apiKey = getApiKeyFromBuildConfig() ?: ""
        return FirebaseConfig(
            projectId = "therapia-app",
            projectNumber = "457686555916",
            storageBucket = "therapia-app.firebasestorage.app",
            packageName = packageName,
            clientId = "457686555916-icn3hvgr13soe1gp8gukd6tmtkohgdem.apps.googleusercontent.com",
            apiKey = apiKey,
            mobilesdkAppId = "1:457686555916:android:fdacd643758143cd00bd29"
        )
    }
    
    /**
     * Efface le cache de configuration
     */
    fun clearCache() {
        configCache = null
        authLogger.logAuthStep("Configuration Firebase", "Cache effacé")
    }
    
    /**
     * Affiche un résumé de la configuration (sans informations sensibles)
     */
    fun getConfigSummary(): String {
        val config = getFirebaseConfig() ?: return "Configuration non disponible"
        
        return """
            📋 Configuration Firebase
            =========================
            ✅ Project ID: ${config.projectId}
            ✅ Package Name: ${config.packageName}
            ✅ Client ID: ${config.clientId.take(20)}...
            ✅ API Key: ${config.apiKey.take(10)}...
            ✅ Storage Bucket: ${config.storageBucket}
            ✅ Mobile SDK App ID: ${config.mobilesdkAppId}
        """.trimIndent()
    }
}
