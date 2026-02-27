package com.frombeyond.r2sl

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.frombeyond.r2sl.auth.GoogleAuthManager
import com.frombeyond.r2sl.data.AppSettingsManager
import com.frombeyond.r2sl.databinding.ActivityMainBinding
import com.frombeyond.r2sl.utils.AccessibilityHelper
import com.frombeyond.r2sl.utils.AuthLogger
import com.frombeyond.r2sl.utils.ErrorLogger
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: GoogleAuthManager
    private lateinit var appSettingsManager: AppSettingsManager
    
    
    // Éléments du header d'authentification
    private lateinit var layoutNotConnected: LinearLayout
    private lateinit var layoutConnected: LinearLayout
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Configuration de la navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_recipes,
                R.id.nav_weekly_menu, R.id.nav_shopping_lists, R.id.nav_settings,
                R.id.nav_accessibility, R.id.nav_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Initialiser les paramètres
        setupSettings()
        
        // Initialiser l'authentification
        setupAuthentication()
        
        // Initialiser les éléments du header
        setupAuthHeader()
        
        // Vérifier l'état de connexion
        checkAuthState()
        
        // Mettre à jour la visibilité du menu diagnostic
        updateDiagnosticMenuVisibility()
        
        // Appliquer les paramètres d'accessibilité à la vue principale
        applyAccessibilitySettings()
        
        // Installer le gestionnaire d'exceptions global
        setupGlobalExceptionHandler()
    }
    
    private fun setupGlobalExceptionHandler() {
        // Initialiser ErrorLogger avec le contexte
        ErrorLogger.getInstance().initialize(this)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // Logger l'erreur
            ErrorLogger.getInstance().logError(
                message = "Uncaught exception in thread ${thread.name}",
                exception = exception,
                tag = "UncaughtException"
            )
            
            // Appeler le handler par défaut pour le comportement standard
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Réappliquer les paramètres d'accessibilité à chaque retour sur l'activité
        applyAccessibilitySettings()
    }
    
    private fun applyAccessibilitySettings() {
        binding.root.let {
            AccessibilityHelper.applyAccessibilitySettings(this, it)
        }
    }
    
    private fun setupSettings() {
        appSettingsManager = AppSettingsManager(this)
    }
    
    fun updateDiagnosticMenuVisibility() {
        // Les menus de diagnostic et de tests ont été supprimés dans R2SL
        // Cette fonction est conservée pour compatibilité mais ne fait plus rien
    }

    private fun setupAuthentication() {
        authManager = GoogleAuthManager(
            context = this,
            onAuthSuccess = { account ->
                updateAuthHeader(true, account)
                Snackbar.make(binding.root, "Connexion réussie !", Snackbar.LENGTH_SHORT).show()
            },
            onAuthFailure = { exception ->
                updateAuthHeader(false, null)
                Snackbar.make(binding.root, "Erreur d'authentification: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }
    
    private fun setupAuthHeader() {
        val headerView = binding.navView.getHeaderView(0)
        
        layoutNotConnected = headerView.findViewById(R.id.layout_not_connected)
        layoutConnected = headerView.findViewById(R.id.layout_connected)
        btnLogin = headerView.findViewById(R.id.btn_login)
        btnLogout = headerView.findViewById(R.id.btn_logout)
        tvUserName = headerView.findViewById(R.id.tv_user_name)
        tvUserEmail = headerView.findViewById(R.id.tv_user_email)
        
        // Configurer les boutons
        btnLogin.setOnClickListener {
            // Lancer l'authentification Google réelle
            startGoogleSignIn()
        }
        
        btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun startGoogleSignIn() {
        val authLogger = AuthLogger.getInstance(this)
        authLogger.logAuthStep("MAIN_ACTIVITY_START_GOOGLE_SIGN_IN", "Début de la connexion Google depuis MainActivity")
        Log.d("MainActivity", "Début de la connexion Google")
        
        val signInIntent = authManager.getSignInIntent()
        if (signInIntent != null) {
            authLogger.logAuthStep("MAIN_ACTIVITY_INTENT_OK", "Intent de connexion obtenu, lancement de l'activité")
            Log.d("MainActivity", "Lancement de l'activité de connexion Google")
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } else {
            authLogger.logAuthError("MAIN_ACTIVITY_INTENT_NULL", "Intent de connexion Google null", null)
            Log.e("MainActivity", "Erreur: Intent de connexion Google null")
            Snackbar.make(binding.root, "Erreur de configuration Google Sign-In", Snackbar.LENGTH_LONG).show()
        }
    }
    
    private fun checkAuthState() {
        try {
            if (authManager.isUserSignedIn()) {
                val user = authManager.getCurrentUser()
                if (user != null) {
                    // Utilisateur connecté via Firebase
                    val account = authManager.getGoogleSignInAccount()
                    if (account != null) {
                        // Compte Google disponible, utiliser les vraies données
                        updateAuthHeader(true, account)
                    } else {
                        // Pas de compte Google disponible, mais utilisateur Firebase connecté
                        // Afficher un état de connexion partiel avec les données Firebase
                        Log.i("MainActivity", "Utilisateur Firebase connecté mais pas Google Sign-In")
                        updateAuthHeaderWithFirebaseUser(user)
                    }
                } else {
                    updateAuthHeader(false, null)
                }
            } else {
                updateAuthHeader(false, null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors de la vérification de l'état d'authentification", e)
            // En cas d'erreur, considérer comme déconnecté
            updateAuthHeader(false, null)
        }
    }
    
    private fun updateAuthHeaderWithFirebaseUser(firebaseUser: FirebaseUser) {
        try {
            // Afficher le profil utilisateur avec les données Firebase
            layoutNotConnected.visibility = View.GONE
            layoutConnected.visibility = View.VISIBLE
            
            // Afficher les informations utilisateur
            tvUserName.text = firebaseUser.displayName ?: "Utilisateur"
            tvUserEmail.text = firebaseUser.email ?: "Email non disponible"
            
            // Charger la photo de profil si disponible
            firebaseUser.photoUrl?.let { photoUrl ->
                loadProfileImage(photoUrl.toString())
            } ?: run {
                // Pas de photo, utiliser l'icône par défaut
                // Note: imgProfile sera défini dans updateAuthHeader
            }
            
            Log.d("MainActivity", "Interface mise à jour avec les données Firebase: ${firebaseUser.email}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors de la mise à jour de l'interface avec Firebase", e)
            // En cas d'erreur, afficher l'état déconnecté
            updateAuthHeader(false, null)
        }
    }
    
    private fun updateAuthHeader(isConnected: Boolean, account: GoogleSignInAccount?) {
        if (isConnected && account != null) {
            // Afficher le profil utilisateur
            layoutNotConnected.visibility = View.GONE
            layoutConnected.visibility = View.VISIBLE
            
            // Utiliser les données Firebase si disponibles
            val firebaseUser = authManager.getCurrentUser()
            if (firebaseUser != null) {
                tvUserName.text = firebaseUser.displayName ?: "Utilisateur"
                tvUserEmail.text = firebaseUser.email ?: "email@example.com"
                
                // Charger la photo de profil Firebase
                loadProfileImage(firebaseUser.photoUrl?.toString())
            } else {
                tvUserName.text = account.displayName ?: "Utilisateur"
                tvUserEmail.text = account.email ?: "email@example.com"
                
                // Charger la photo de profil Google
                loadProfileImage(account.photoUrl?.toString())
            }
        } else {
            // Afficher le bouton de connexion
            layoutNotConnected.visibility = View.VISIBLE
            layoutConnected.visibility = View.GONE
        }
    }
    
    private fun loadProfileImage(photoUrl: String?) {
        val profileImageView = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.iv_profile)
        
        if (!photoUrl.isNullOrEmpty()) {
            // Charger l'image avec Glide
            try {
                com.bumptech.glide.Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_menu_patients)
                    .error(R.drawable.ic_menu_patients)
                    .override(48, 48) // Forcer la taille de l'image
                    .into(profileImageView)
            } catch (e: Exception) {
                // En cas d'erreur, utiliser l'icône par défaut
                profileImageView.setImageResource(R.drawable.ic_menu_patients)
            }
        } else {
            // Pas de photo disponible, utiliser l'icône par défaut
            profileImageView.setImageResource(R.drawable.ic_menu_patients)
        }
    }
    
    private fun logout() {
        authManager.signOut()
        updateAuthHeader(false, null)
        Snackbar.make(binding.root, "Déconnexion réussie", Snackbar.LENGTH_SHORT).show()
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        val authLogger = AuthLogger.getInstance(this)
        authLogger.logAuthStep("MAIN_ACTIVITY_ON_ACTIVITY_RESULT", "onActivityResult appelé")
        authLogger.logAuthInfo("MAIN_ACTIVITY_RESULT_CODE", "requestCode=$requestCode, resultCode=$resultCode")
        authLogger.logAuthInfo("MAIN_ACTIVITY_RESULT_DATA", "data=${data?.toString()}, dataNull=${data == null}")
        
        Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data=${data?.toString()}")
        if (requestCode == com.frombeyond.r2sl.ui.settings.SettingsFragment.RC_DRIVE_PERMISSIONS) {
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment)
                ?.childFragmentManager?.primaryNavigationFragment?.let { frag ->
                    if (frag is com.frombeyond.r2sl.ui.settings.SettingsFragment) {
                        frag.onActivityResult(requestCode, resultCode, data)
                    }
                }
            return
        }
        if (requestCode == RC_SIGN_IN) {
            authLogger.logAuthStep("MAIN_ACTIVITY_RC_SIGN_IN_MATCH", "Code de requête correspond à RC_SIGN_IN")
            
            if (resultCode == RESULT_OK && data != null) {
                authLogger.logAuthStep("MAIN_ACTIVITY_RESULT_OK", "Résultat OK, début du traitement")
                Log.d("MainActivity", "Authentification Google réussie, lancement Firebase...")
                
                // Lancer l'authentification Firebase
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        authLogger.logAuthStep("MAIN_ACTIVITY_HANDLE_SIGN_IN_START", "Début de handleSignInResult")
                        val account = authManager.handleSignInResult(data)
                        
                        if (account != null) {
                            authLogger.logAuthStep("MAIN_ACTIVITY_ACCOUNT_RECEIVED", "Compte Google récupéré avec succès: ${account.email}")
                            Log.d("MainActivity", "Compte Google récupéré: ${account.email}")
                            
                            authLogger.logAuthStep("MAIN_ACTIVITY_FIREBASE_AUTH_START", "Début de firebaseAuthWithGoogle")
                            authManager.firebaseAuthWithGoogle(account)
                        } else {
                            authLogger.logAuthError("MAIN_ACTIVITY_ACCOUNT_NULL", "Échec de la récupération du compte Google", null)
                            Log.e("MainActivity", "Échec de la récupération du compte Google")
                            Snackbar.make(binding.root, "Erreur lors de la récupération du compte", Snackbar.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        authLogger.logAuthError("MAIN_ACTIVITY_AUTH_EXCEPTION", "Exception lors de l'authentification", e, mapOf(
                            "exceptionType" to e.javaClass.simpleName,
                            "exceptionMessage" to (e.message ?: "null")
                        ))
                        Log.e("MainActivity", "Erreur lors de l'authentification: ${e.message}", e)
                        Snackbar.make(binding.root, "Erreur d'authentification: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } else {
                authLogger.logAuthStep("MAIN_ACTIVITY_RESULT_NOT_OK", "Résultat non OK ou data null")
                Log.w("MainActivity", "Authentification annulée ou échouée: resultCode=$resultCode, data=${data?.toString()}")
                
                val errorType = when (resultCode) {
                    RESULT_CANCELED -> "Utilisateur a annulé"
                    0 -> "Code 0 (annulation)"
                    else -> "Code d'erreur: $resultCode"
                }
                authLogger.logAuthError("MAIN_ACTIVITY_AUTH_FAILED", "Authentification échouée - $errorType", null, mapOf(
                    "resultCode" to resultCode,
                    "data" to (data?.toString() ?: "null"),
                    "dataNull" to (data == null),
                    "errorType" to errorType
                ))
                
                if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(binding.root, "Connexion annulée par l'utilisateur", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Échec de la connexion (code: $resultCode)", Snackbar.LENGTH_LONG).show()
                }
            }
        } else {
            authLogger.logAuthInfo("MAIN_ACTIVITY_REQUEST_CODE_MISMATCH", "Code de requête ne correspond pas: $requestCode (attendu: $RC_SIGN_IN)")
        }
    }
    
    // Gestion du menu 3 points
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                navigateToFragment(R.id.nav_home)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // Navigation vers un fragment
    private fun navigateToFragment(fragmentId: Int) {
        findNavController(R.id.nav_host_fragment_content_main).navigate(fragmentId)
    }
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
}