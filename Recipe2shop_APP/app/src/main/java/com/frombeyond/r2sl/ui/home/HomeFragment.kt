package com.frombeyond.r2sl.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.graphics.BitmapFactory
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.auth.GoogleAuthManager
import com.frombeyond.r2sl.utils.AuthLogger
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var authManager: GoogleAuthManager
    
    // Éléments d'authentification
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var ivHomeLogo: ImageView
    
    // Raccourcis rapides R2SL
    private lateinit var btnQuickRecipes: LinearLayout
    private lateinit var btnQuickWeeklyMenu: LinearLayout
    private lateinit var btnQuickShoppingLists: LinearLayout
    private lateinit var btnQuickSettings: LinearLayout
    private lateinit var btnQuickAccessibility: LinearLayout
    private lateinit var btnQuickProfile: LinearLayout
    private lateinit var quickMenuIcon: ImageView
    private lateinit var quickListIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Initialiser l'authentification
        setupAuthentication(root)
        
        return root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Vérifier l'état d'authentification
        checkAuthState()
    }
    
    private fun setupAuthentication(root: View) {
        // Initialiser les éléments d'authentification
        tvWelcomeMessage = root.findViewById(R.id.tv_welcome_message)
        ivHomeLogo = root.findViewById(R.id.iv_home_logo)
        
        // Initialiser les boutons de raccourci rapide R2SL
        btnQuickRecipes = root.findViewById(R.id.btn_quick_recipes)
        btnQuickWeeklyMenu = root.findViewById(R.id.btn_quick_weekly_menu)
        btnQuickShoppingLists = root.findViewById(R.id.btn_quick_shopping_lists)
        btnQuickSettings = root.findViewById(R.id.btn_quick_settings)
        btnQuickAccessibility = root.findViewById(R.id.btn_quick_accessibility)
        btnQuickProfile = root.findViewById(R.id.btn_quick_profile)
        quickMenuIcon = root.findViewById(R.id.quick_menu_icon)
        quickListIcon = root.findViewById(R.id.quick_list_icon)
        
        // Configurer les listeners des boutons
        setupQuickAccessButtons()

        loadAppLogo()
        
        // Initialiser le gestionnaire d'authentification
        authManager = GoogleAuthManager(
            context = requireContext(),
            onAuthSuccess = { account ->
                updateHomeAuthUI(true, account)
                Snackbar.make(root, "Connexion réussie !", Snackbar.LENGTH_SHORT).show()
            },
            onAuthFailure = { exception ->
                updateHomeAuthUI(false, null)
                Snackbar.make(root, "Erreur d'authentification: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }
    
    private fun startGoogleSignIn() {
        val signInIntent = authManager.getSignInIntent()
        if (signInIntent != null) {
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } else {
            Snackbar.make(requireView(), "Erreur de configuration Google Sign-In", Snackbar.LENGTH_LONG).show()
        }
    }
    
    private fun checkAuthState() {
        if (authManager.isUserSignedIn()) {
            val user = authManager.getCurrentUser()
            if (user != null) {
                // Utilisateur connecté via Firebase
                val account = authManager.getGoogleSignInAccount()
                if (account != null) {
                    updateHomeAuthUI(true, account)
                } else {
                    // Créer un compte Google à partir des données Firebase
                    val mockAccount = createMockAccountFromFirebaseUser(user)
                    updateHomeAuthUI(true, mockAccount)
                }
            } else {
                updateHomeAuthUI(false, null)
            }
        } else {
            updateHomeAuthUI(false, null)
        }
    }
    
    private fun createMockAccountFromFirebaseUser(firebaseUser: FirebaseUser): GoogleSignInAccount {
        // Créer un compte Google temporaire à partir des données Firebase
        // Note: firebaseUser pourrait être utilisé pour personnaliser le compte
        return GoogleSignInAccount.createDefault()
    }
    
    private fun updateHomeAuthUI(isConnected: Boolean, account: GoogleSignInAccount?) {
        if (isConnected && account != null) {
            // Utiliser les données Firebase si disponibles
            val firebaseUser = authManager.getCurrentUser()
            if (firebaseUser != null) {
                // Personnaliser le message de bienvenue
                val userName = firebaseUser.displayName ?: "Utilisateur"
                tvWelcomeMessage.text = "Bienvenue, $userName !"
            } else {
                val userName = account.displayName ?: "Utilisateur"
                tvWelcomeMessage.text = "Bienvenue, $userName !"
            }
        } else {
            tvWelcomeMessage.text = "Bienvenue dans R2SL !"
        }
    }
    
    private fun setupQuickAccessButtons() {
        // Charger les icônes depuis assets
        loadButtonIconFromAsset(quickMenuIcon, "images/menu_icon.png")
        loadButtonIconFromAsset(quickListIcon, "images/list_icon.png")
        
        btnQuickRecipes.setOnClickListener {
            findNavController().navigate(R.id.nav_recipes)
        }
        
        btnQuickWeeklyMenu.setOnClickListener {
            findNavController().navigate(R.id.nav_weekly_menu)
        }
        
        btnQuickShoppingLists.setOnClickListener {
            findNavController().navigate(R.id.nav_shopping_lists)
        }
        
        btnQuickSettings.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
        
        btnQuickAccessibility.setOnClickListener {
            findNavController().navigate(R.id.accessibility_settings)
        }
        
        btnQuickProfile.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
    }
    
    private fun loadButtonIconFromAsset(imageView: ImageView, assetPath: String) {
        try {
            requireContext().assets.open(assetPath).use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    private fun loadAppLogo() {
        try {
            requireContext().assets.open("images/logo_app.png").use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                ivHomeLogo.setImageBitmap(bitmap)
            }
        } catch (_: Exception) {
            ivHomeLogo.visibility = View.GONE
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                // Lancer l'authentification Firebase
                CoroutineScope(Dispatchers.Main).launch {
                    val account = authManager.handleSignInResult(data)
                    if (account != null) {
                        authManager.firebaseAuthWithGoogle(account)
                    }
                }
            } else {
                val authLogger = AuthLogger.getInstance(requireContext())
                val errorType = when (resultCode) {
                    android.app.Activity.RESULT_CANCELED -> "Utilisateur a annulé"
                    0 -> {
                        // Code 0 indique généralement un problème de configuration OAuth
                        "Erreur de configuration OAuth (code 0). Vérifiez :\n" +
                        "1. Client ID configuré dans build.gradle.kts\n" +
                        "2. SHA-1 fingerprint dans Google Cloud Console\n" +
                        "3. Package name correspondant"
                    }
                    else -> "Code d'erreur: $resultCode"
                }
                authLogger.logAuthError("HOME_FRAGMENT_AUTH_FAILED", "Authentification échouée - $errorType", null, mapOf(
                    "resultCode" to resultCode,
                    "data" to (data?.toString() ?: "null"),
                    "errorType" to errorType
                ))
                val message = if (resultCode == 0) {
                    "Erreur de configuration OAuth. Consultez les logs pour plus de détails."
                } else {
                    "Connexion annulée (code: $resultCode)"
                }
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
}