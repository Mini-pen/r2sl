package com.frombeyond.r2sl.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.frombeyond.r2sl.utils.AuthLogger
import com.frombeyond.r2sl.utils.FirebaseConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(
    private val context: Context,
    private val onAuthSuccess: (GoogleSignInAccount) -> Unit,
    private val onAuthFailure: (Exception) -> Unit
) {
    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private val authLogger = AuthLogger.getInstance(context)
    private val configManager = FirebaseConfigManager.getInstance(context)

    init {
        try {
            setupGoogleSignIn()
            setupFirebaseAuth()
        } catch (e: Exception) {
            authLogger.logAuthError("GoogleAuthManager", "Erreur lors de l'initialisation", e)
            // Ne pas planter l'application, juste logger l'erreur
        }
    }

    private fun setupGoogleSignIn() {
        try {
            // Valider la configuration Firebase
            if (!configManager.validateConfiguration()) {
                authLogger.logAuthError("Configuration Google Sign-In", "Configuration Firebase invalide")
                // Ne pas planter l'application, juste logger l'erreur
                return
            }
            
            // Valider la cohérence du package name
            if (!configManager.validatePackageName()) {
                authLogger.logAuthError("Configuration Google Sign-In", "Package name de l'application incompatible avec la configuration Firebase")
                // Ne pas planter l'application, juste logger l'erreur
                return
            }
            
            val clientId = configManager.getGoogleClientId()
            if (clientId.isNullOrEmpty() || clientId == "YOUR_DEBUG_CLIENT_ID_HERE" || clientId == "YOUR_RELEASE_CLIENT_ID_HERE") {
                val errorMsg = "Client ID Google non configuré ou invalide. Veuillez configurer GOOGLE_CLIENT_ID_DEBUG et GOOGLE_CLIENT_ID_RELEASE dans app/build.gradle.kts"
                authLogger.logAuthError("Configuration Google Sign-In", errorMsg)
                Log.e(TAG, errorMsg)
                // Ne pas planter l'application, juste logger l'erreur
                return
            }
            
            Log.d(TAG, "Client ID configuré: ${clientId.take(30)}...")
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .requestProfile()
                // * Request Google Drive scopes for file access
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))  // Create and modify files
                .requestScopes(Scope("https://www.googleapis.com/auth/drive.metadata.readonly"))  // Read metadata
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            authLogger.logAuthStep("Google Sign-In", "Configuration initialisée avec succès")
            authLogger.logAuthInfo("Client ID", "Chargé depuis la configuration: ${clientId.take(20)}...")
        } catch (e: Exception) {
            authLogger.logAuthError("Configuration Google Sign-In", "Erreur lors de l'initialisation", e)
            // Ne pas planter l'application, juste logger l'erreur
        }
    }

    private fun setupFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        
        // Écouter les changements d'état d'authentification
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d(TAG, "Utilisateur connecté: ${user.email}")
            } else {
                Log.d(TAG, "Utilisateur déconnecté")
            }
        }
    }

    fun getSignInIntent(): Intent? {
        return if (::googleSignInClient.isInitialized) {
            authLogger.logAuthStep("GET_SIGN_IN_INTENT", "Récupération de l'intent de connexion Google")
            authLogger.logAuthInfo("GOOGLE_SIGN_IN_CLIENT_STATUS", "Client Google Sign-In initialisé: true")
            try {
                val intent = googleSignInClient.signInIntent
                authLogger.logAuthInfo("SIGN_IN_INTENT_CREATED", "Intent créé avec succès")
                authLogger.logAuthInfo("SIGN_IN_INTENT_DETAILS", "Intent action: ${intent.action}, component: ${intent.component?.className}")
                Log.d(TAG, "Intent de connexion Google créé avec succès")
                intent
            } catch (e: Exception) {
                authLogger.logAuthError("SIGN_IN_INTENT_CREATION_FAILED", "Erreur lors de la création de l'intent", e)
                Log.e(TAG, "Erreur lors de la création de l'intent: ${e.message}", e)
                null
            }
        } else {
            authLogger.logAuthError("GOOGLE_SIGN_IN_CLIENT_NOT_INITIALIZED", "Client Google Sign-In non initialisé", null)
            Log.e(TAG, "Client Google Sign-In non initialisé")
            null
        }
    }

    suspend fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            authLogger.logAuthStep("HANDLE_SIGN_IN_RESULT_START", "Début du traitement du résultat Google Sign-In")
            authLogger.logAuthInfo("HANDLE_SIGN_IN_RESULT_DATA", "Données reçues: ${data?.toString()}")
            authLogger.logAuthInfo("HANDLE_SIGN_IN_RESULT_DATA_NULL", "Data est null: ${data == null}")
            
            if (data == null) {
                authLogger.logAuthError("HANDLE_SIGN_IN_RESULT_DATA_NULL", "Intent data est null", null)
                Log.e(TAG, "Intent data est null")
                onAuthFailure(Exception("Aucune donnée reçue de Google Sign-In"))
                return null
            }
            
            authLogger.logAuthStep("GOOGLE_SIGN_IN_GET_TASK", "Récupération de la tâche Google Sign-In")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            authLogger.logAuthStep("GOOGLE_SIGN_IN_TASK_CREATED", "Tâche Google Sign-In créée")
            authLogger.logAuthInfo("GOOGLE_SIGN_IN_TASK_COMPLETE", "Tâche complète: ${task.isComplete}")
            
            authLogger.logAuthStep("GOOGLE_SIGN_IN_TASK_AWAIT", "Attente du résultat de la tâche")
            val account = task.await()
            authLogger.logAuthStep("GOOGLE_SIGN_IN_TASK_AWAIT_DONE", "Résultat de la tâche reçu")
            
            authLogger.logAuthInfo("GOOGLE_SIGN_IN_SUCCESS", "Connexion Google réussie: ${account.email}")
            authLogger.logAuthInfo("GOOGLE_SIGN_IN_ACCOUNT_DETAILS", "Détails du compte: ID=${account.id}, Name=${account.displayName}")
            authLogger.logAuthInfo("GOOGLE_SIGN_IN_ACCOUNT_ID_TOKEN", "ID Token présent: ${account.idToken != null}, longueur: ${account.idToken?.length ?: 0}")
            
            Log.d(TAG, "Connexion Google réussie: ${account.email}")
            account
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                12501 -> "L'utilisateur a annulé la connexion"
                7 -> "Erreur de réseau"
                8 -> "Erreur interne"
                10 -> "Erreur de configuration (vérifiez le Client ID)"
                12500 -> "L'utilisateur a annulé la connexion"
                else -> "Erreur API Google Sign-In: ${e.statusCode}"
            }
            authLogger.logAuthError("GOOGLE_SIGN_IN_API_EXCEPTION", errorMessage, e, mapOf(
                "statusCode" to e.statusCode,
                "errorMessage" to errorMessage
            ))
            Log.e(TAG, "Erreur de connexion Google: ${e.statusCode} - $errorMessage")
            onAuthFailure(Exception(errorMessage))
            null
        } catch (e: Exception) {
            authLogger.logAuthError("GOOGLE_SIGN_IN_HANDLE_ERROR", "Erreur lors du traitement du résultat Google Sign-In", e)
            Log.e(TAG, "Erreur inattendue: ${e.message}")
            onAuthFailure(e)
            null
        }
    }

    suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        try {
            authLogger.logAuthStep("FIREBASE_AUTH_START", "Début de l'authentification Firebase avec Google")
            authLogger.logAuthInfo("FIREBASE_AUTH_ACCOUNT", "Compte Google: ${account.email}, ID Token: ${account.idToken?.take(20)}...")
            authLogger.logAuthInfo("FIREBASE_AUTH_ID_TOKEN_NULL", "ID Token est null: ${account.idToken == null}")
            
            if (account.idToken == null) {
                authLogger.logAuthError("FIREBASE_AUTH_ID_TOKEN_MISSING", "ID Token manquant pour l'authentification Firebase", null)
                Log.e(TAG, "ID Token manquant pour l'authentification Firebase")
                onAuthFailure(Exception("ID Token manquant pour l'authentification Firebase"))
                return
            }
            
            authLogger.logAuthStep("FIREBASE_CREDENTIAL_CREATE", "Création du credential Firebase")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authLogger.logAuthStep("FIREBASE_CREDENTIAL_CREATED", "Credential Firebase créé avec succès")
            
            authLogger.logAuthStep("FIREBASE_SIGN_IN_WITH_CREDENTIAL", "Début de signInWithCredential")
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            authLogger.logAuthStep("FIREBASE_AUTH_COMPLETED", "Authentification Firebase terminée")
            authLogger.logAuthInfo("FIREBASE_AUTH_RESULT", "Résultat: user=${authResult.user != null}, additionalUserInfo=${authResult.additionalUserInfo != null}")
            
            if (authResult.user != null) {
                authLogger.logAuthInfo("FIREBASE_AUTH_SUCCESS", "Authentification Firebase réussie: ${authResult.user?.email}")
                authLogger.logAuthInfo("FIREBASE_USER_DETAILS", "UID: ${authResult.user?.uid}, Email: ${authResult.user?.email}, DisplayName: ${authResult.user?.displayName}")
                Log.d(TAG, "Authentification Firebase réussie: ${authResult.user?.email}")
                onAuthSuccess(account)
            } else {
                authLogger.logAuthError("FIREBASE_AUTH_USER_NULL", "Utilisateur Firebase null après authentification", null)
                Log.e(TAG, "Échec de l'authentification Firebase - utilisateur null")
                onAuthFailure(Exception("Échec de l'authentification Firebase - utilisateur null"))
            }
        } catch (e: Exception) {
            authLogger.logAuthError("FIREBASE_AUTH_EXCEPTION", "Erreur d'authentification Firebase", e, mapOf(
                "exceptionType" to e.javaClass.simpleName,
                "exceptionMessage" to (e.message ?: "null")
            ))
            Log.e(TAG, "Erreur d'authentification Firebase: ${e.message}", e)
            onAuthFailure(e)
        }
    }

    fun signOut() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Déconnexion Firebase
                firebaseAuth.signOut()
                
                // Déconnexion Google seulement si le client est initialisé
                if (::googleSignInClient.isInitialized) {
                    googleSignInClient.signOut().await()
                }
                
                Log.d(TAG, "Déconnexion réussie")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la déconnexion: ${e.message}")
            }
        }
    }

    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun getGoogleSignInAccount(): GoogleSignInAccount? {
        return if (::googleSignInClient.isInitialized) {
            GoogleSignIn.getLastSignedInAccount(context)
        } else {
            authLogger.logAuthError("Google Sign-In", "Client Google Sign-In non initialisé")
            null
        }
    }
}

// Extension function pour créer le launcher d'authentification
fun Fragment.createGoogleAuthLauncher(
    onAuthSuccess: (GoogleSignInAccount) -> Unit,
    onAuthFailure: (Exception) -> Unit
): ActivityResultLauncher<Intent> {
    return registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val logger = AuthLogger.getInstance(requireContext())
            logger.logAuthStep("GOOGLE_AUTH_LAUNCHER_SUCCESS", "Résultat Google Sign-In OK - Code: ${result.resultCode}")
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    logger.logAuthStep("GOOGLE_AUTH_LAUNCHER_START", "Début du traitement du résultat")
                    
                    val authManager = GoogleAuthManager(
                        context = requireContext(),
                        onAuthSuccess = onAuthSuccess,
                        onAuthFailure = onAuthFailure
                    )
                    
                    val account = authManager.handleSignInResult(data)
                    if (account != null) {
                        logger.logAuthStep("GOOGLE_AUTH_LAUNCHER_ACCOUNT_OK", "Compte Google récupéré avec succès")
                        authManager.firebaseAuthWithGoogle(account)
                    } else {
                        logger.logAuthError("GOOGLE_AUTH_LAUNCHER_ACCOUNT_NULL", "Compte Google null après traitement", null)
                    }
                } catch (e: Exception) {
                    logger.logAuthError("GOOGLE_AUTH_LAUNCHER_EXCEPTION", "Exception dans le launcher", e)
                    onAuthFailure(e)
                }
            }
        } else {
            val errorMessage = "Connexion annulée par l'utilisateur - Code: ${result.resultCode}"
            val logger = AuthLogger.getInstance(requireContext())
            logger.logAuthError("GOOGLE_SIGN_IN_CANCELLED", errorMessage, null, mapOf(
                "resultCode" to result.resultCode,
                "data" to (result.data?.toString() ?: "null")
            ))
            onAuthFailure(Exception(errorMessage))
        }
    }
}
