package com.frombeyond.r2sl.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.frombeyond.r2sl.ui.BaseFragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.auth.GoogleAuthManager
import com.frombeyond.r2sl.data.AppSettingsManager
import com.frombeyond.r2sl.data.local.AppDataBackupManager
import com.frombeyond.r2sl.data.local.IngredientEmojiManager
import com.frombeyond.r2sl.data.local.RayonsManager
import com.frombeyond.r2sl.ui.recipes.RecipesLocalFileManager
import com.frombeyond.r2sl.utils.AuthLogger
import com.frombeyond.r2sl.utils.ErrorLogger
import com.frombeyond.r2sl.utils.GoogleDriveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsFragment : BaseFragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var authManager: GoogleAuthManager
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var googleDriveManager: GoogleDriveManager
    
    // Éléments d'authentification
    private lateinit var settingsNotConnected: LinearLayout
    private lateinit var settingsConnected: LinearLayout
    private lateinit var btnSettingsLogin: Button
    private lateinit var btnSettingsLogout: Button
    private lateinit var tvSettingsUserName: TextView
    private lateinit var tvSettingsUserEmail: TextView
    
    // Éléments des paramètres
    private lateinit var cbDevFeatures: MaterialCheckBox
    private lateinit var btnAccessibilitySettings: MaterialButton
    private lateinit var btnBackupLocalZip: MaterialButton
    private lateinit var btnRestoreLocalZip: MaterialButton
    private lateinit var btnImportRecipePack: MaterialButton
    private lateinit var btnLoadDefaultEmojiDict: MaterialButton
    private lateinit var btnRefreshRecipesIndex: MaterialButton
    private lateinit var btnLoadDefaultRecipes: MaterialButton
    private lateinit var btnUpdateRecipesMetadata: MaterialButton
    private lateinit var btnFixRecipesSourcesIngredients: MaterialButton
    private lateinit var btnClearAppData: MaterialButton
    private lateinit var backupManager: AppDataBackupManager
    private lateinit var recipesFileManager: RecipesLocalFileManager
    private lateinit var rayonsManager: RayonsManager
    private lateinit var rayonsListContainer: LinearLayout
    private lateinit var btnRayonsPrefill: MaterialButton
    private lateinit var btnRayonsAdd: MaterialButton
    
    // Éléments des logs d'erreurs (mode développeur)
    private lateinit var errorLogsContainer: LinearLayout
    private lateinit var tvErrorLogs: TextView
    private lateinit var tvLogFilePath: TextView
    private lateinit var btnCopyErrorLogs: MaterialButton
    private lateinit var btnClearErrorLogs: MaterialButton
    
    // Éléments des logs d'authentification (mode développeur)
    private lateinit var authLogsContainer: LinearLayout
    private lateinit var tvAuthLogs: TextView
    private lateinit var tvAuthLogFilePath: TextView
    private lateinit var btnCopyAuthLogs: MaterialButton
    private lateinit var btnClearAuthLogs: MaterialButton
    
    // Éléments Google Drive
    private lateinit var tvDriveConnectionStatus: TextView
    private lateinit var btnConnectGoogleDrive: MaterialButton
    private lateinit var btnDisconnectGoogleDrive: MaterialButton
    private lateinit var sectionProfileBackup: LinearLayout
    private lateinit var sectionSettingsBackup: LinearLayout
    
    // Boutons de sauvegarde
    private lateinit var btnBackupProfileToDrive: MaterialButton
    private lateinit var btnRestoreProfileFromDrive: MaterialButton
    private lateinit var btnBackupSettingsToDrive: MaterialButton
    private lateinit var btnRestoreSettingsFromDrive: MaterialButton

    private val backupExportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { exportBackupToUri(it) }
        }

    private val backupImportLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importBackupFromUri(it) }
        }

    private val recipePackImportLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importRecipePackFromUri(it) }
        }

    /** Pending Drive action after user grants Drive scopes: "profile" or "settings". */
    private var pendingDriveAction: String? = null

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        
        // Initialiser les paramètres
        setupSettings(rootView)
        
        // Initialiser l'authentification
        setupAuthentication(rootView)
        
        return rootView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Vérifier l'état d'authentification
        checkAuthState()
        
        // Charger les paramètres
        loadSettings()
    }
    
    override fun onResume() {
        super.onResume()
        // Rafraîchir les logs si le mode développeur est activé
        if (appSettingsManager.isDevFeaturesEnabled()) {
            refreshErrorLogs()
            refreshAuthLogs()
        }
    }
    
    private fun setupSettings(root: View) {
        // Initialiser les gestionnaires
        appSettingsManager = AppSettingsManager(requireContext())
        googleDriveManager = GoogleDriveManager(requireContext())
        backupManager = AppDataBackupManager(requireContext())
        recipesFileManager = RecipesLocalFileManager(requireContext())
        
        // Initialiser les éléments des paramètres
        cbDevFeatures = root.findViewById(R.id.cb_dev_features)
        btnAccessibilitySettings = root.findViewById(R.id.btn_accessibility_settings)
        btnBackupLocalZip = root.findViewById(R.id.btn_backup_local_zip)
        btnRestoreLocalZip = root.findViewById(R.id.btn_restore_local_zip)
        btnImportRecipePack = root.findViewById(R.id.btn_import_recipe_pack)
        btnLoadDefaultEmojiDict = root.findViewById(R.id.btn_load_default_emoji_dict)
        rayonsListContainer = root.findViewById(R.id.rayons_list_container)
        btnRayonsPrefill = root.findViewById(R.id.btn_rayons_prefill)
        btnRayonsAdd = root.findViewById(R.id.btn_rayons_add)
        btnRefreshRecipesIndex = root.findViewById(R.id.btn_refresh_recipes_index)
        rayonsManager = RayonsManager(requireContext())
        btnLoadDefaultRecipes = root.findViewById(R.id.btn_load_default_recipes)
        btnUpdateRecipesMetadata = root.findViewById(R.id.btn_update_recipes_metadata)
        btnFixRecipesSourcesIngredients = root.findViewById(R.id.btn_fix_recipes_sources_ingredients)
        btnClearAppData = root.findViewById(R.id.btn_clear_app_data)
        
        // Initialiser les éléments des logs d'erreurs
        errorLogsContainer = root.findViewById(R.id.dev_error_logs_container)
        tvErrorLogs = root.findViewById(R.id.tv_error_logs)
        tvLogFilePath = root.findViewById(R.id.tv_log_file_path)
        btnCopyErrorLogs = root.findViewById(R.id.btn_copy_error_logs)
        btnClearErrorLogs = root.findViewById(R.id.btn_clear_error_logs)
        
        // Initialiser les éléments des logs d'authentification
        authLogsContainer = root.findViewById(R.id.dev_auth_logs_container)
        tvAuthLogs = root.findViewById(R.id.tv_auth_logs)
        tvAuthLogFilePath = root.findViewById(R.id.tv_auth_log_file_path)
        btnCopyAuthLogs = root.findViewById(R.id.btn_copy_auth_logs)
        btnClearAuthLogs = root.findViewById(R.id.btn_clear_auth_logs)
        
        // Initialiser les éléments Google Drive
        tvDriveConnectionStatus = root.findViewById(R.id.tv_drive_connection_status)
        btnConnectGoogleDrive = root.findViewById(R.id.btn_connect_google_drive)
        btnDisconnectGoogleDrive = root.findViewById(R.id.btn_disconnect_google_drive)
        sectionProfileBackup = root.findViewById(R.id.section_profile_backup)
        sectionSettingsBackup = root.findViewById(R.id.section_settings_backup)
        
        // Initialiser les boutons de sauvegarde
        btnBackupProfileToDrive = root.findViewById(R.id.btn_backup_profile_to_drive)
        btnRestoreProfileFromDrive = root.findViewById(R.id.btn_restore_profile_from_drive)
        btnBackupSettingsToDrive = root.findViewById(R.id.btn_backup_settings_to_drive)
        btnRestoreSettingsFromDrive = root.findViewById(R.id.btn_restore_settings_from_drive)
        
        // Configurer la case à cocher avec sauvegarde automatique
        cbDevFeatures.setOnCheckedChangeListener { _, isChecked ->
            appSettingsManager.setDevFeaturesEnabled(isChecked)
            // Notifier l'activité principale pour mettre à jour le menu
            notifyMenuUpdate()
            updateDevOptionsVisibility(isChecked)
        }
        
        // Configurer le bouton pour copier les logs
        btnCopyErrorLogs.setOnClickListener {
            val logText = tvErrorLogs.text.toString()
            if (logText.isNotEmpty() && logText != "Aucune erreur enregistrée") {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Logs d'erreurs", logText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Logs copiés dans le presse-papier", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Aucun log à copier", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Configurer le bouton pour effacer les logs
        btnClearErrorLogs.setOnClickListener {
            ErrorLogger.getInstance().clearLogs()
            refreshErrorLogs()
        }
        
        // Configurer le bouton pour copier les logs d'authentification
        btnCopyAuthLogs.setOnClickListener {
            val logText = tvAuthLogs.text.toString()
            if (logText.isNotEmpty() && logText != "Aucun log d'authentification enregistré") {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Logs d'authentification", logText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Logs d'authentification copiés dans le presse-papier", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Aucun log d'authentification à copier", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Configurer le bouton pour effacer les logs d'authentification
        btnClearAuthLogs.setOnClickListener {
            AuthLogger.getInstance(requireContext()).clearLog()
            refreshAuthLogs()
        }

        btnAccessibilitySettings.setOnClickListener {
            findNavController().navigate(R.id.accessibility_settings)
        }

        btnBackupLocalZip.setOnClickListener {
            val fileName = buildBackupFileName()
            backupExportLauncher.launch(fileName)
        }

        btnRestoreLocalZip.setOnClickListener {
            backupImportLauncher.launch(arrayOf("application/zip"))
        }

        btnImportRecipePack.setOnClickListener {
            recipePackImportLauncher.launch(arrayOf("application/zip"))
        }

        btnLoadDefaultEmojiDict.setOnClickListener {
            val emojiManager = IngredientEmojiManager(requireContext())
            val count = emojiManager.loadDefaultFromAssets()
            Toast.makeText(requireContext(), getString(R.string.settings_emoji_dict_loaded, count), Toast.LENGTH_SHORT).show()
        }

        btnRefreshRecipesIndex.setOnClickListener {
            recipesFileManager.refreshIndex()
            Toast.makeText(requireContext(), R.string.recipes_refresh_list_done, Toast.LENGTH_SHORT).show()
        }

        btnLoadDefaultRecipes.setOnClickListener {
            showLoadDefaultRecipesDialog()
        }

        btnUpdateRecipesMetadata.setOnClickListener {
            showUpdateRecipesMetadataDialog()
        }

        btnFixRecipesSourcesIngredients.setOnClickListener {
            fixRecipesSourcesAndIngredients()
        }

        btnClearAppData.setOnClickListener {
            showClearDataDialog()
        }
        
        // Configurer les boutons Google Drive
        btnConnectGoogleDrive.setOnClickListener {
            startGoogleSignIn()
        }
        
        btnDisconnectGoogleDrive.setOnClickListener {
            disconnectGoogleDrive()
        }
        
        // Configurer les boutons de sauvegarde du profil
        btnBackupProfileToDrive.setOnClickListener {
            backupProfileToDrive()
        }
        
        btnRestoreProfileFromDrive.setOnClickListener {
            showProfileFileSelectionDialog()
        }
        
        // Configurer les boutons de sauvegarde des paramètres
        btnBackupSettingsToDrive.setOnClickListener {
            backupSettingsToDrive()
        }
        
        btnRestoreSettingsFromDrive.setOnClickListener {
            showSettingsFileSelectionDialog()
        }
    }
    
    private fun loadSettings() {
        // Charger l'état de la case à cocher
        val devFeaturesEnabled = appSettingsManager.isDevFeaturesEnabled()
        cbDevFeatures.isChecked = devFeaturesEnabled
        updateDevOptionsVisibility(devFeaturesEnabled)
    }

    private fun updateDevOptionsVisibility(enabled: Boolean) {
        btnClearAppData.visibility = if (enabled) View.VISIBLE else View.GONE
        btnRefreshRecipesIndex.visibility = if (enabled) View.VISIBLE else View.GONE
        btnLoadDefaultRecipes.visibility = if (enabled) View.VISIBLE else View.GONE
        btnUpdateRecipesMetadata.visibility = if (enabled) View.VISIBLE else View.GONE
        btnFixRecipesSourcesIngredients.visibility = if (enabled) View.VISIBLE else View.GONE
        rootView.findViewById<View>(R.id.dev_backup_section).visibility = if (enabled) View.VISIBLE else View.GONE
        errorLogsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        authLogsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            refreshErrorLogs()
            refreshAuthLogs()
        }
    }
    
    private fun refreshErrorLogs() {
        try {
            val logs = ErrorLogger.getInstance().getFormattedLogs()
            tvErrorLogs.text = if (logs.isNotEmpty()) {
                logs
            } else {
                "Aucune erreur enregistrée"
            }
            
            // Afficher le chemin du fichier de log
            val logFilePath = ErrorLogger.getInstance().getLogFilePath()
            tvLogFilePath.text = logFilePath ?: "Fichier de log non disponible"
        } catch (e: Exception) {
            ErrorLogger.getInstance().logError("Erreur lors du rafraîchissement des logs", e, "SettingsFragment")
            tvErrorLogs.text = "Erreur lors du chargement des logs: ${e.message}"
        }
    }
    
    // * Rafraîchit les logs d'authentification
    private fun refreshAuthLogs() {
        try {
            val authLogger = AuthLogger.getInstance(requireContext())
            val logs = authLogger.getLogContent()
            tvAuthLogs.text = if (logs.isNotEmpty() && logs != "Aucun fichier de log trouvé") {
                logs
            } else {
                "Aucun log d'authentification enregistré"
            }
            
            // Afficher le chemin du fichier de log
            val logFilePath = authLogger.getLogFilePath()
            tvAuthLogFilePath.text = logFilePath ?: "Fichier de log non disponible"
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Erreur lors du rafraîchissement des logs d'authentification", e)
            tvAuthLogs.text = "Erreur lors du chargement des logs: ${e.message}"
        }
    }
    
    private fun notifyMenuUpdate() {
        // Notifier l'activité principale pour mettre à jour la visibilité du menu diagnostic
        try {
            val activity = requireActivity()
            if (activity is com.frombeyond.r2sl.MainActivity) {
                activity.updateDiagnosticMenuVisibility()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Erreur lors de la notification de mise à jour du menu", e)
        }
    }
    
    private fun setupAuthentication(root: View) {
        // Initialiser les éléments d'authentification
        settingsNotConnected = root.findViewById(R.id.settings_not_connected)
        settingsConnected = root.findViewById(R.id.settings_connected)
        btnSettingsLogin = root.findViewById(R.id.btn_settings_login)
        btnSettingsLogout = root.findViewById(R.id.btn_settings_logout)
        tvSettingsUserName = root.findViewById(R.id.tv_settings_user_name)
        tvSettingsUserEmail = root.findViewById(R.id.tv_settings_user_email)
        
        // Configurer les boutons
        btnSettingsLogin.setOnClickListener {
            // Lancer l'authentification Google réelle
            startGoogleSignIn()
        }
        
        btnSettingsLogout.setOnClickListener {
            logout()
        }
        
        // Initialiser le gestionnaire d'authentification
        authManager = GoogleAuthManager(
            context = requireContext(),
            onAuthSuccess = { account ->
                updateSettingsAuthUI(true, account)
                Snackbar.make(root, "Connexion réussie !", Snackbar.LENGTH_SHORT).show()
            },
            onAuthFailure = { exception ->
                updateSettingsAuthUI(false, null)
                Snackbar.make(root, "Erreur d'authentification: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun exportBackupToUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resolver = requireContext().contentResolver
            val result = try {
                resolver.openOutputStream(uri)?.use { output ->
                    backupManager.createBackupZip(output)
                } ?: 0
            } catch (_: Exception) {
                -1
            }
            withContext(Dispatchers.Main) {
                if (result >= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.settings_local_backup_success, result),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), R.string.settings_local_backup_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importBackupFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resolver = requireContext().contentResolver
            val result = try {
                resolver.openInputStream(uri)?.use { input ->
                    backupManager.restoreBackupZip(input)
                } ?: -1
            } catch (_: Exception) {
                -1
            }
            withContext(Dispatchers.Main) {
                if (result >= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.settings_local_restore_success, result),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), R.string.settings_local_restore_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showClearDataDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_dev_clear_data_title)
            .setMessage(R.string.settings_dev_clear_data_message)
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .setPositiveButton(R.string.settings_dev_clear_data_confirm) { _, _ ->
                clearAllAppData()
            }
            .show()
    }

    private fun clearAllAppData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val deleted = backupManager.clearAllData()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_dev_clear_data_done, deleted),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoadDefaultRecipesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_load_default_recipes_title)
            .setMessage(R.string.settings_load_default_recipes_message)
            .setPositiveButton(R.string.settings_load_default_recipes_confirm) { _, _ ->
                loadDefaultRecipes()
            }
            .setNegativeButton(R.string.recipes_edit_duplicate_cancel, null)
            .show()
    }

    private fun loadDefaultRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = recipesFileManager.ensureSamplesSeeded()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_load_default_recipes_done, count),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showUpdateRecipesMetadataDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Mettre à jour les métadonnées")
            .setMessage("Choisissez la source à attribuer aux recettes existantes :")
            .setItems(arrayOf("r2sl_recipes_pack", "debug_pack")) { _, which ->
                val source = if (which == 0) "r2sl_recipes_pack" else "debug_pack"
                updateRecipesMetadata(source)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun updateRecipesMetadata(source: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = recipesFileManager.updateAllRecipesMetadata(
                source = source,
                author = "Cyrille Baudouin"
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Métadonnées mises à jour pour $count recette(s)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // * Corrige les sources et vérifie la cohérence des ingrédients
    private fun fixRecipesSourcesAndIngredients() {
        lifecycleScope.launch(Dispatchers.IO) {
            val (sourceFixed, ingredientsFixed) = recipesFileManager.fixRecipesSourcesAndIngredients()
            withContext(Dispatchers.Main) {
                val message = buildString {
                    append("Correction terminée :\n")
                    append("• Sources corrigées : $sourceFixed\n")
                    append("• Ingrédients corrigés : $ingredientsFixed")
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // * Importe un pack de recettes depuis un fichier ZIP
    private fun importRecipePackFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolver = requireContext().contentResolver
                resolver.openInputStream(uri)?.use { input ->
                    val imported = importRecipePackFromZip(input)
                    withContext(Dispatchers.Main) {
                        if (imported >= 0) {
                            Toast.makeText(
                                requireContext(),
                                "$imported recette(s) importée(s) avec succès",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Erreur lors de l'import du pack de recettes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Erreur: impossible d'ouvrir le fichier ZIP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur lors de l'import du pack de recettes", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erreur lors de l'import: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // * Importe les recettes depuis un ZIP
    private fun importRecipePackFromZip(inputStream: java.io.InputStream): Int {
        var importedCount = 0
        val existingRecipes = recipesFileManager.listRecipeEntries().associateBy { it.name.lowercase() }
        
        java.util.zip.ZipInputStream(java.io.BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase().endsWith(".json")) {
                    try {
                        // Lire le contenu du fichier JSON depuis le ZIP
                        val jsonContent = zip.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        
                        val jsonObject = org.json.JSONObject(jsonContent)
                        val format = com.frombeyond.r2sl.data.export.RecipeJsonFormat.fromJsonObject(jsonObject)
                        
                        format.recipes.forEach { recipeJson ->
                            // Vérifier si une recette avec le même nom existe déjà
                            val recipeName = recipeJson.name
                            val existingRecipe = existingRecipes[recipeName.lowercase()]
                            
                            val finalRecipeName = if (existingRecipe != null) {
                                // Recette existe déjà, ajouter "bis" au nom
                                "$recipeName bis"
                            } else {
                                recipeName
                            }
                            
                            // Générer un nom de fichier unique
                            val baseFileName = finalRecipeName.replace(Regex("[^a-zA-Z0-9]"), "_")
                            var fileName = "$baseFileName.json"
                            var counter = 2
                            while (recipesFileManager.getRecipeFile(fileName) != null) {
                                fileName = "${baseFileName}_$counter.json"
                                counter++
                            }
                            
                            // Créer la recette avec le nouveau nom si nécessaire
                            val recipeToSave = if (existingRecipe != null) {
                                recipeJson.copy(name = finalRecipeName)
                            } else {
                                recipeJson
                            }
                            
                            // Sauvegarder la recette
                            val formatToSave = com.frombeyond.r2sl.data.export.RecipeJsonFormat(
                                version = format.version,
                                recipes = listOf(recipeToSave)
                            )
                            
                            recipesFileManager.saveRecipeFile(fileName, formatToSave)
                            recipesFileManager.upsertRecipeIndexEntry(fileName, finalRecipeName)
                            importedCount++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsFragment", "Erreur lors de l'import de ${entry.name}", e)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        
        return importedCount
    }

    private fun buildBackupFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        return "recipe2shop_backup_$timestamp.zip"
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
        try {
            if (authManager.isUserSignedIn()) {
                val user = authManager.getCurrentUser()
                if (user != null) {
                    // Utilisateur connecté via Firebase
                    val account = authManager.getGoogleSignInAccount()
                    if (account != null) {
                        // Compte Google disponible, utiliser les vraies données
                        updateSettingsAuthUI(true, account)
                    } else {
                        // Pas de compte Google disponible, mais utilisateur Firebase connecté
                        // Afficher un état de connexion partiel avec les données Firebase
                        android.util.Log.i("SettingsFragment", "Utilisateur Firebase connecté mais pas Google Sign-In")
                        updateSettingsAuthUIWithFirebaseUser(user)
                    }
                } else {
                    updateSettingsAuthUI(false, null)
                }
            } else {
                updateSettingsAuthUI(false, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Erreur lors de la vérification de l'état d'authentification", e)
            // En cas d'erreur, considérer comme déconnecté
            updateSettingsAuthUI(false, null)
        }
        
        // Vérifier l'état des boutons Google Drive
        checkGoogleDriveButtonsStatus()
    }
    
    private fun updateSettingsAuthUIWithFirebaseUser(firebaseUser: FirebaseUser) {
        try {
            // Afficher l'état connecté avec les données Firebase
            settingsNotConnected.visibility = View.GONE
            settingsConnected.visibility = View.VISIBLE
            
            // Afficher les informations utilisateur
            tvSettingsUserName.text = firebaseUser.displayName ?: "Utilisateur"
            tvSettingsUserEmail.text = firebaseUser.email ?: "Email non disponible"
            
            // Note: La photo de profil sera gérée dans updateSettingsAuthUI
            
            android.util.Log.d("SettingsFragment", "Interface mise à jour avec les données Firebase: ${firebaseUser.email}")
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Erreur lors de la mise à jour de l'interface avec Firebase", e)
            // En cas d'erreur, afficher l'état déconnecté
            updateSettingsAuthUI(false, null)
        }
    }
    
    private fun updateSettingsAuthUI(isConnected: Boolean, account: GoogleSignInAccount?) {
        if (isConnected && account != null) {
            // Afficher l'état connecté
            settingsNotConnected.visibility = View.GONE
            settingsConnected.visibility = View.VISIBLE
            
            // Utiliser les données Firebase si disponibles
            val firebaseUser = authManager.getCurrentUser()
            if (firebaseUser != null) {
                tvSettingsUserName.text = firebaseUser.displayName ?: "Utilisateur"
                tvSettingsUserEmail.text = firebaseUser.email ?: "email@example.com"
            } else {
                tvSettingsUserName.text = account.displayName ?: "Utilisateur"
                tvSettingsUserEmail.text = account.email ?: "email@example.com"
            }
        } else {
            // Afficher l'état non connecté
            settingsNotConnected.visibility = View.VISIBLE
            settingsConnected.visibility = View.GONE
        }
        
        // Mettre à jour l'état Google Drive
        checkGoogleDriveButtonsStatus()
    }
    
    private fun logout() {
        authManager.signOut()
        updateSettingsAuthUI(false, null)
        Snackbar.make(requireView(), "Déconnexion réussie", Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_DRIVE_PERMISSIONS) {
            val action = pendingDriveAction
            pendingDriveAction = null
            if (resultCode == android.app.Activity.RESULT_OK) {
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account != null && GoogleSignIn.hasPermissions(account, *DRIVE_SCOPES.toTypedArray())) {
                    when (action) {
                        "profile" -> doBackupProfileToDrive(account)
                        "settings" -> doBackupSettingsToDrive(account)
                        else -> doBackupProfileToDrive(account)
                    }
                } else {
                    Toast.makeText(context, "Autorisations Drive refusées ou compte indisponible", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
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
                    0 -> "Code 0 (annulation)"
                    else -> "Code d'erreur: $resultCode"
                }
                authLogger.logAuthError("SETTINGS_FRAGMENT_AUTH_FAILED", "Authentification échouée - $errorType", null, mapOf(
                    "resultCode" to resultCode,
                    "data" to (data?.toString() ?: "null"),
                    "errorType" to errorType
                ))
                Snackbar.make(requireView(), "Connexion annulée (code: $resultCode)", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Vérifie l'état de connexion Google et met à jour l'interface
     */
    private fun checkGoogleDriveButtonsStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        val isConnected = account != null
        
        // Mettre à jour le statut de connexion
        if (isConnected) {
            tvDriveConnectionStatus.text = "🟢 Connecté"
            tvDriveConnectionStatus.setTextColor(requireContext().getColor(R.color.success_color))
            btnConnectGoogleDrive.visibility = View.GONE
            btnDisconnectGoogleDrive.visibility = View.VISIBLE
            sectionProfileBackup.visibility = View.VISIBLE
            sectionSettingsBackup.visibility = View.VISIBLE
        } else {
            tvDriveConnectionStatus.text = "🔴 Non connecté"
            tvDriveConnectionStatus.setTextColor(requireContext().getColor(R.color.error_color))
            btnConnectGoogleDrive.visibility = View.VISIBLE
            btnDisconnectGoogleDrive.visibility = View.GONE
            sectionProfileBackup.visibility = View.GONE
            sectionSettingsBackup.visibility = View.GONE
        }
        
        android.util.Log.d("SettingsFragment", "État de connexion Google pour Drive: $isConnected")
    }
    
    /**
     * Déconnecte Google Drive
     */
    private fun disconnectGoogleDrive() {
        authManager.signOut()
        checkGoogleDriveButtonsStatus()
        Snackbar.make(requireView(), "Déconnexion Google Drive réussie", Snackbar.LENGTH_SHORT).show()
    }
    
    /**
     * Sauvegarde le profil sur Google Drive.
     * Requests Drive scopes incrementally if not already granted (avoids "app not verified" at sign-in).
     */
    private fun backupProfileToDrive() {
        lifecycleScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    Toast.makeText(context, "Veuillez vous connecter avec Google", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (!GoogleSignIn.hasPermissions(account, *DRIVE_SCOPES.toTypedArray())) {
                    pendingDriveAction = "profile"
                    GoogleSignIn.requestPermissions(
                        requireActivity(),
                        RC_DRIVE_PERMISSIONS,
                        account,
                        *DRIVE_SCOPES.toTypedArray()
                    )
                    return@launch
                }
                doBackupProfileToDrive(account)
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur avant sauvegarde Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doBackupProfileToDrive(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            try {
                val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    listOf(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/drive.metadata.readonly"
                    )
                )
                credential.selectedAccount = account.account
                googleDriveManager.initialize(credential)
                if (!googleDriveManager.isInitialized()) {
                    Toast.makeText(context, "Erreur: Google Drive non initialisé", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val connectionTest = withContext(Dispatchers.IO) {
                    googleDriveManager.testConnection()
                }
                if (!connectionTest) {
                    Toast.makeText(context, googleDriveManager.getLastDriveError() ?: getString(R.string.drive_connection_error_fallback), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val success = withContext(Dispatchers.IO) {
                    googleDriveManager.backupConfigurationFiles()
                }
                if (success) {
                    Toast.makeText(context, "Profil sauvegardé sur Google Drive avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = googleDriveManager.getLastDriveError()
                        ?: "Erreur lors de la sauvegarde sur Google Drive"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur sauvegarde profil Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner un fichier de profil
     */
    private fun showProfileFileSelectionDialog() {
        lifecycleScope.launch {
            try {
                // Vérifier la connexion Google
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    Toast.makeText(context, "Veuillez vous connecter avec Google", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Initialiser Google Drive Manager
                val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    listOf(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/drive.metadata.readonly"
                    )
                )
                credential.selectedAccount = account.account
                googleDriveManager.initialize(credential)
                
                // Lister les fichiers de sauvegarde
                val backupFiles = withContext(Dispatchers.IO) {
                    googleDriveManager.listBackupFiles()
                }
                
                // Filtrer les fichiers de profil
                val profileFiles = backupFiles.filter { 
                    it.name.contains("therapist_profile") && it.name.endsWith(".json")
                }
                
                if (profileFiles.isEmpty()) {
                    Toast.makeText(context, "Aucun fichier de profil trouvé sur Google Drive", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Afficher la boîte de dialogue de sélection
                showFileSelectionDialog(profileFiles, "profil")
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur lors de la récupération des fichiers de profil", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner un fichier de paramètres
     */
    private fun showSettingsFileSelectionDialog() {
        lifecycleScope.launch {
            try {
                // Vérifier la connexion Google
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    Toast.makeText(context, "Veuillez vous connecter avec Google", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Initialiser Google Drive Manager
                val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    listOf(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/drive.metadata.readonly"
                    )
                )
                credential.selectedAccount = account.account
                googleDriveManager.initialize(credential)
                
                // Lister les fichiers de sauvegarde
                val backupFiles = withContext(Dispatchers.IO) {
                    googleDriveManager.listBackupFiles()
                }
                
                // Filtrer les fichiers de paramètres
                val settingsFiles = backupFiles.filter { 
                    it.name.contains("app_preferences") && it.name.endsWith(".json")
                }
                
                if (settingsFiles.isEmpty()) {
                    Toast.makeText(context, "Aucun fichier de paramètres trouvé sur Google Drive", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Afficher la boîte de dialogue de sélection
                showFileSelectionDialog(settingsFiles, "paramètres")
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur lors de la récupération des fichiers de paramètres", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner un fichier
     */
    private fun showFileSelectionDialog(files: List<com.google.api.services.drive.model.File>, type: String) {
        val fileNames = files.map { 
            "${it.name} (${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.modifiedTime?.value ?: 0L))})"
        }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Sélectionner un fichier de $type")
            .setItems(fileNames) { _, which ->
                val selectedFile = files[which]
                if (type == "profil") {
                    restoreProfileFromDrive(selectedFile)
                } else {
                    restoreSettingsFromDrive(selectedFile)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    /**
     * Restaure le profil depuis un fichier sélectionné
     */
    private fun restoreProfileFromDrive(selectedFile: com.google.api.services.drive.model.File) {
        lifecycleScope.launch {
            try {
                // Télécharger le fichier
                val localFile = java.io.File(requireContext().filesDir, "therapist_profile_restored.json")
                val downloadSuccess = withContext(Dispatchers.IO) {
                    googleDriveManager.downloadFile(selectedFile.id, localFile)
                }
                
                if (downloadSuccess) {
                    // Charger le profil restauré
                    val profileStorageManager = com.frombeyond.r2sl.data.ProfileStorageManager(requireContext())
                    val restoredProfile = profileStorageManager.loadProfileFromFile(localFile)
                    if (restoredProfile != null) {
                        Toast.makeText(context, "Profil restauré depuis Google Drive avec succès !", Toast.LENGTH_SHORT).show()
                        android.util.Log.d("SettingsFragment", "Profil restauré depuis Google Drive")
                    } else {
                        Toast.makeText(context, "Erreur lors du chargement du profil restauré", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Erreur lors du téléchargement depuis Google Drive", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur lors de la restauration du profil depuis Google Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Restaure les paramètres depuis un fichier sélectionné
     */
    private fun restoreSettingsFromDrive(selectedFile: com.google.api.services.drive.model.File) {
        lifecycleScope.launch {
            try {
                // Télécharger le fichier
                val localFile = java.io.File(requireContext().filesDir, "app_preferences_restored.json")
                val downloadSuccess = withContext(Dispatchers.IO) {
                    googleDriveManager.downloadFile(selectedFile.id, localFile)
                }
                
                if (downloadSuccess) {
                    // Charger les paramètres restaurés
                    val restoredSettings = appSettingsManager.loadSettingsFromFile(localFile)
                    if (restoredSettings) {
                        loadSettings()
                        Toast.makeText(context, "Paramètres restaurés depuis Google Drive avec succès !", Toast.LENGTH_SHORT).show()
                        android.util.Log.d("SettingsFragment", "Paramètres restaurés depuis Google Drive")
                    } else {
                        Toast.makeText(context, "Erreur lors du chargement des paramètres restaurés", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Erreur lors du téléchargement depuis Google Drive", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur lors de la restauration des paramètres depuis Google Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Sauvegarde les paramètres sur Google Drive. Requests Drive scopes if missing.
     */
    private fun backupSettingsToDrive() {
        lifecycleScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    Toast.makeText(context, "Veuillez vous connecter avec Google", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (!GoogleSignIn.hasPermissions(account, *DRIVE_SCOPES.toTypedArray())) {
                    pendingDriveAction = "settings"
                    GoogleSignIn.requestPermissions(
                        requireActivity(),
                        RC_DRIVE_PERMISSIONS,
                        account,
                        *DRIVE_SCOPES.toTypedArray()
                    )
                    return@launch
                }
                doBackupSettingsToDrive(account)
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur avant sauvegarde paramètres Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doBackupSettingsToDrive(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            try {
                val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    listOf(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/drive.metadata.readonly"
                    )
                )
                credential.selectedAccount = account.account
                googleDriveManager.initialize(credential)
                if (!googleDriveManager.isInitialized()) {
                    Toast.makeText(context, "Erreur: Google Drive non initialisé", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val connectionTest = withContext(Dispatchers.IO) { googleDriveManager.testConnection() }
                if (!connectionTest) {
                    val msg = googleDriveManager.getLastDriveError()
                        ?: "Impossible de se connecter à Google Drive. Activez l'API Google Drive dans Google Cloud Console."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    return@launch
                }
                val success = withContext(Dispatchers.IO) { googleDriveManager.backupConfigurationFiles() }
                if (success) {
                    Toast.makeText(context, "Paramètres sauvegardés sur Google Drive avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = googleDriveManager.getLastDriveError()
                        ?: "Erreur lors de la sauvegarde sur Google Drive"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Erreur sauvegarde paramètres Drive", e)
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    
    companion object {
        private const val RC_SIGN_IN = 9001
        const val RC_DRIVE_PERMISSIONS = 9002
        private val DRIVE_SCOPES = listOf(
            Scope(DriveScopes.DRIVE_FILE),
            Scope("https://www.googleapis.com/auth/drive.metadata.readonly")
        )
    }
}
