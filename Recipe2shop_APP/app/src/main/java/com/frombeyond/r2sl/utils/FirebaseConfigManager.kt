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
     * Returns the Client ID to use for requestIdToken() (Google Sign-In / Firebase Auth).
     * Firebase requires the Web client (OAuth type 3), not the Android client (type 1), to avoid Code 10 / DEVELOPER_ERROR.
     * Priority: 1) Plugin-generated default_web_client_id (from google-services.json), 2) Web (type 3) from assets JSON, 3) BuildConfig FIREBASE_WEB_CLIENT_ID, 4) Android (type 1) as last resort.
     */
    fun getGoogleClientId(): String? {
        val webFromRes = getWebClientIdFromGeneratedResources()
        if (!webFromRes.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID (Web) depuis default_web_client_id: ${webFromRes.take(20)}...")
            return webFromRes
        }
        val webFromJson = getWebClientIdFromGoogleServicesAssets()
        if (!webFromJson.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID (Web type 3) depuis google-services.json (assets): ${webFromJson.take(20)}...")
            return webFromJson
        }
        val webFromBuildConfig = getFirebaseWebClientIdFromBuildConfig()
        if (!webFromBuildConfig.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID (Web) depuis BuildConfig FIREBASE_WEB_CLIENT_ID: ${webFromBuildConfig.take(20)}...")
            return webFromBuildConfig
        }
        val androidFromJson = getAndroidClientIdFromGoogleServicesAssets()
        if (!androidFromJson.isNullOrEmpty()) {
            authLogger.logAuthInfo("Client ID", "Client ID (Android type 1, secours) depuis google-services.json: ${androidFromJson.take(20)}...")
            return androidFromJson
        }
        authLogger.logAuthError("Client ID", "Aucun Client ID valide trouvé (Web type 3 ou Android type 1). Vérifiez google-services.json dans app/ et empreintes SHA-1 dans Firebase.")
        return null
    }

    /**
     * Returns Web client ID from plugin-generated resource (default_web_client_id).
     * The Google Services plugin injects the Web client from google-services.json into res/values at build time.
     */
    private fun getWebClientIdFromGeneratedResources(): String? {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId).takeIf { it.isNotEmpty() }
            } else null
        } catch (e: Exception) {
            null
        }
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
     * Lit la configuration depuis FirebaseApp.options (traité par le plugin Google Services)
     * * Le plugin Google Services traite google-services.json au build et injecte les valeurs dans FirebaseApp
     */
    private fun readConfigFromGoogleServices(): FirebaseConfig? {
        return try {
            // * Utiliser FirebaseApp.options qui contient les valeurs traitées par le plugin Google Services
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            val options = firebaseApp.options
            
            val packageName = context.packageName
            
            // * Récupérer les valeurs depuis FirebaseOptions (traitées par le plugin)
            val projectId = options.projectId ?: return null
            val projectNumber = extractProjectNumberFromAppId(options.applicationId) ?: return null
            val storageBucket = options.storageBucket ?: return null
            val applicationId = options.applicationId ?: return null
            
            // * API Key depuis BuildConfig (priorité) ou FirebaseOptions
            val apiKey = getApiKeyFromBuildConfig() ?: options.apiKey ?: return null
            
            // * Client ID depuis BuildConfig (priorité) ou depuis google-services.json via assets
            val clientId = getClientIdFromBuildConfig() ?: getClientIdFromGoogleServicesAssets() ?: return null
            
            authLogger.logAuthInfo("Google Services", "Configuration chargée depuis FirebaseApp.options")
            
            FirebaseConfig(
                projectId = projectId,
                projectNumber = projectNumber,
                storageBucket = storageBucket,
                packageName = packageName,
                clientId = clientId,
                apiKey = apiKey,
                mobilesdkAppId = applicationId
            )
        } catch (e: Exception) {
            authLogger.logAuthError("Google Services", "Impossible de lire la configuration Firebase", e)
            null
        }
    }
    
    /**
     * Extrait le project number depuis l'application ID (format: 1:PROJECT_NUMBER:android:APP_ID)
     */
    private fun extractProjectNumberFromAppId(applicationId: String?): String? {
        return try {
            applicationId?.split(":")?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
    
    /** OAuth client_type: 1 = Android, 3 = Web. requestIdToken() must use Web (3). */
    private fun getOAuthClientsFromGoogleServicesAssets(): List<Pair<Int, String>>? {
        return try {
            val inputStream = context.assets.open("google-services.json")
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            val clientArray = jsonObject.getJSONArray("client")
            val androidClient = clientArray.getJSONObject(0)
            val oauthClientArray = androidClient.getJSONArray("oauth_client")
            (0 until oauthClientArray.length()).map { i ->
                val obj = oauthClientArray.getJSONObject(i)
                obj.getInt("client_type") to obj.getString("client_id")
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Returns Web client (client_type 3) client_id for requestIdToken(), or null. */
    private fun getWebClientIdFromGoogleServicesAssets(): String? {
        val clients = getOAuthClientsFromGoogleServicesAssets() ?: return null
        return clients.firstOrNull { it.first == 3 }?.second
    }

    /** Returns Android client (client_type 1) client_id, or null. Used only as last resort. */
    private fun getAndroidClientIdFromGoogleServicesAssets(): String? {
        val clients = getOAuthClientsFromGoogleServicesAssets() ?: return null
        return clients.firstOrNull { it.first == 1 }?.second
    }

    /**
     * Récupère le Client ID depuis google-services.json (priorité Web type 3, puis Android type 1).
     */
    private fun getClientIdFromGoogleServicesAssets(): String? {
        return getWebClientIdFromGoogleServicesAssets() ?: getAndroidClientIdFromGoogleServicesAssets()
    }

    /**
     * Récupère le Web Client ID depuis BuildConfig (FIREBASE_WEB_CLIENT_ID dans local.properties).
     */
    private fun getFirebaseWebClientIdFromBuildConfig(): String? {
        return try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val webClientId = buildConfigClass.getField("FIREBASE_WEB_CLIENT_ID").get(null) as? String
            webClientId?.takeIf { it.isNotEmpty() && it != "null" }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Récupère le Client ID depuis BuildConfig ou depuis les assets/google-services.json
     * * Cette méthode est utilisée comme fallback dans createDefaultConfig
     */
    private fun getClientIdFromGoogleServices(): String {
        return try {
            // D'abord, essayer d'utiliser le client ID depuis BuildConfig (si défini)
            val buildConfigClientId = getClientIdFromBuildConfig()
            if (!buildConfigClientId.isNullOrEmpty() && buildConfigClientId != "YOUR_DEBUG_CLIENT_ID_HERE" && buildConfigClientId != "YOUR_RELEASE_CLIENT_ID_HERE") {
                authLogger.logAuthInfo("Client ID", "Utilisation du Client ID depuis BuildConfig")
                return buildConfigClientId
            }
            
            // Sinon, essayer depuis les assets
            val clientIdFromAssets = getClientIdFromGoogleServicesAssets()
            if (!clientIdFromAssets.isNullOrEmpty()) {
                return clientIdFromAssets
            }
            
            // Fallback : essayer depuis FirebaseApp.options (via applicationId pour déduire)
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            val options = firebaseApp.options
            // Note: FirebaseOptions n'expose pas directement le client ID, donc on retourne vide
            authLogger.logAuthError("Client ID", "Aucun Client ID trouvé")
            ""
        } catch (e: Exception) {
            authLogger.logAuthError("Client ID", "Impossible de lire le Client ID", e)
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
     * Crée une configuration par défaut en cas d'échec de lecture
     * * Essaie d'utiliser FirebaseApp.options comme fallback
     */
    private fun createDefaultConfig(): FirebaseConfig {
        // * Essayer d'utiliser FirebaseApp.options comme fallback
        val configFromFirebase = try {
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            val options = firebaseApp.options
            
            val projectId = options.projectId
            val projectNumber = extractProjectNumberFromAppId(options.applicationId)
            val storageBucket = options.storageBucket
            val applicationId = options.applicationId
            val apiKey = getApiKeyFromBuildConfig() ?: options.apiKey
            val clientId = getClientIdFromBuildConfig() ?: getClientIdFromGoogleServicesAssets()
            
            if (projectId != null && projectNumber != null && storageBucket != null && applicationId != null && apiKey != null && clientId != null) {
                FirebaseConfig(
                    projectId = projectId,
                    projectNumber = projectNumber,
                    storageBucket = storageBucket,
                    packageName = context.packageName,
                    clientId = clientId,
                    apiKey = apiKey,
                    mobilesdkAppId = applicationId
                )
            } else {
                null
            }
        } catch (e: Exception) {
            authLogger.logAuthError("Default Config", "Erreur lors de la lecture de FirebaseApp.options", e)
            null
        }
        
        // * Si la lecture depuis FirebaseApp a réussi, retourner cette configuration
        if (configFromFirebase != null) {
            return configFromFirebase
        }
        
        // * Fallback final : configuration vide
        authLogger.logAuthError("Default Config", "Impossible de créer une configuration par défaut - Firebase non configuré")
        return FirebaseConfig(
            projectId = "",
            projectNumber = "",
            storageBucket = "",
            packageName = context.packageName,
            clientId = "",
            apiKey = "",
            mobilesdkAppId = ""
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
