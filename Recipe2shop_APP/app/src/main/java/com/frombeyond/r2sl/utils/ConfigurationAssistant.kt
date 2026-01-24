package com.frombeyond.r2sl.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Assistant de configuration automatique
 * Guide l'utilisateur à travers la configuration Firebase et Google API
 */
class ConfigurationAssistant(private val context: Context) {
    
    companion object {
        private const val TAG = "ConfigurationAssistant"
        private const val CONFIGURATION_GUIDE_FILE = "configuration_guide.txt"
    }
    
    private val configurationManager = ConfigurationManager(context)
    
    /**
     * Lance l'assistant de configuration complet
     */
    fun startConfigurationWizard() {
        val result = configurationManager.checkAndConfigureAll()
        
        if (result.isValid) {
            showConfigurationSuccess()
        } else {
            showConfigurationIssues(result)
        }
    }
    
    /**
     * Affiche les problèmes de configuration et propose des solutions
     */
    private fun showConfigurationIssues(result: ConfigurationManager.ConfigurationResult) {
        val issues = mutableListOf<String>()
        
        if (!result.googlePlayServices) {
            issues.add("Google Play Services n'est pas disponible ou n'est pas à jour")
        }
        if (!result.firebase) {
            issues.add("Firebase n'est pas correctement configuré")
        }
        if (!result.googleServicesJson) {
            issues.add("Le fichier google-services.json est manquant ou invalide")
        }
        if (result.sha1Fingerprint == null) {
            issues.add("Impossible d'obtenir le SHA-1 fingerprint")
        }
        if (result.permissions.values.any { !it }) {
            issues.add("Certaines permissions sont manquantes")
        }
        if (!result.oauthConfig) {
            issues.add("La configuration OAuth n'est pas complète")
        }
        
        val message = buildString {
            appendLine("❌ PROBLÈMES DE CONFIGURATION DÉTECTÉS")
            appendLine()
            issues.forEach { issue ->
                appendLine("• $issue")
            }
            appendLine()
            appendLine("Voulez-vous que je génère un guide de configuration détaillé ?")
        }
        
        // Créer un dialogue personnalisé avec ScrollView
        val dialog = AlertDialog.Builder(context)
            .setTitle("Configuration Requise")
            .setPositiveButton("Générer le Guide") { _, _ ->
                generateConfigurationGuide(result)
            }
            .setNegativeButton("Annuler", null)
            .setNeutralButton("Ouvrir Google Console") { _, _ ->
                openGoogleConsole()
            }
            .create()
        
        // Créer un ScrollView avec le message
        val scrollView = android.widget.ScrollView(context)
        val textView = android.widget.TextView(context)
        textView.text = message
        textView.textSize = 14f
        textView.setPadding(50, 20, 50, 20)
        textView.setTextColor(android.graphics.Color.BLACK)
        
        scrollView.addView(textView)
        dialog.setView(scrollView)
        dialog.show()
    }
    
    /**
     * Affiche le succès de la configuration
     */
    private fun showConfigurationSuccess() {
        val message = buildString {
            appendLine("✅ CONFIGURATION VALIDE")
            appendLine()
            appendLine("Tous les services Google sont correctement configurés :")
            appendLine("• Google Play Services : Disponible")
            appendLine("• Firebase : Configuré")
            appendLine("• Google Services JSON : Valide")
            appendLine("• SHA-1 : ${configurationManager.checkAndConfigureAll().sha1Fingerprint}")
            appendLine("• Permissions : Accordées")
            appendLine("• OAuth : Prêt")
            appendLine()
            appendLine("La sauvegarde Google Drive devrait fonctionner correctement !")
        }
        
        AlertDialog.Builder(context)
            .setTitle("Configuration Réussie")
            .setMessage(message)
            .setPositiveButton("Tester la Sauvegarde") { _, _ ->
                testGoogleDriveBackup()
            }
            .setNegativeButton("OK", null)
            .show()
    }
    
    /**
     * Génère un guide de configuration détaillé
     */
    private fun generateConfigurationGuide(result: ConfigurationManager.ConfigurationResult) {
        try {
            val guide = buildString {
                appendLine("=== GUIDE DE CONFIGURATION THERAPIA ===")
                appendLine("Généré le : ${java.util.Date()}")
                appendLine()
                
                appendLine("1. CONFIGURATION FIREBASE")
                appendLine("   URL : https://console.firebase.google.com/")
                appendLine("   Étapes :")
                appendLine("   - Créer un nouveau projet ou sélectionner le projet existant")
                appendLine("   - Ajouter une application Android")
                appendLine("   - Package name : com.frombeyond.r2sl")
                appendLine("   - SHA-1 : ${result.sha1Fingerprint ?: "À obtenir"}")
                appendLine("   - Télécharger google-services.json")
                appendLine("   - Placer le fichier dans app/")
                appendLine()
                
                appendLine("2. CONFIGURATION GOOGLE CLOUD CONSOLE")
                appendLine("   URL : https://console.cloud.google.com/")
                appendLine("   Étapes :")
                appendLine("   - Sélectionner le même projet que Firebase")
                appendLine("   - Aller dans APIs & Services > Library")
                appendLine("   - Activer Google Drive API")
                appendLine("   - Aller dans APIs & Services > Credentials")
                appendLine("   - Créer OAuth 2.0 Client ID (Android)")
                appendLine("   - Package name : com.frombeyond.r2sl")
                appendLine("   - SHA-1 : ${result.sha1Fingerprint ?: "À obtenir"}")
                appendLine()
                
                appendLine("3. CONFIGURATION OAUTH")
                appendLine("   - Aller dans APIs & Services > OAuth consent screen")
                appendLine("   - Ajouter le scope : https://www.googleapis.com/auth/drive.file")
                appendLine("   - Ajouter votre compte Google aux test users")
                appendLine()
                
                appendLine("4. PERMISSIONS ANDROID")
                appendLine("   Vérifier que ces permissions sont dans AndroidManifest.xml :")
                appendLine("   - android.permission.INTERNET")
                appendLine("   - android.permission.ACCESS_NETWORK_STATE")
                appendLine("   - android.permission.WRITE_EXTERNAL_STORAGE")
                appendLine("   - android.permission.READ_EXTERNAL_STORAGE")
                appendLine()
                
                appendLine("5. DÉPENDANCES GRADLE")
                appendLine("   Vérifier que ces dépendances sont dans build.gradle.kts :")
                appendLine("   - com.google.firebase:firebase-bom")
                appendLine("   - com.google.firebase:firebase-auth-ktx")
                appendLine("   - com.google.android.gms:play-services-auth")
                appendLine("   - com.google.apis:google-api-services-drive")
                appendLine("   - com.google.api-client:google-api-client-android")
                appendLine()
                
                appendLine("6. COMMANDES UTILES")
                appendLine("   Obtenir SHA-1 de debug :")
                appendLine("   keytool -list -v -keystore \"%USERPROFILE%\\.android\\debug.keystore\" -alias androiddebugkey -storepass android -keypass android")
                appendLine()
                appendLine("   Obtenir SHA-1 de production :")
                appendLine("   keytool -list -v -keystore \"chemin/vers/votre/keystore.keystore\" -alias votre_alias")
                appendLine()
                
                appendLine("7. VÉRIFICATION")
                appendLine("   - Redémarrer l'application")
                appendLine("   - Tester la connexion Google")
                appendLine("   - Tester la sauvegarde Google Drive")
                appendLine()
                
                appendLine("8. RÉSOLUTION DES PROBLÈMES")
                if (!result.googlePlayServices) {
                    appendLine("   • Google Play Services : Mettre à jour l'application")
                }
                if (!result.firebase) {
                    appendLine("   • Firebase : Vérifier la configuration dans build.gradle")
                }
                if (!result.googleServicesJson) {
                    appendLine("   • google-services.json : Placer le fichier dans app/")
                }
                if (result.sha1Fingerprint == null) {
                    appendLine("   • SHA-1 : Vérifier la signature de l'application")
                }
                if (result.permissions.values.any { !it }) {
                    appendLine("   • Permissions : Accorder les permissions manquantes")
                }
                if (!result.oauthConfig) {
                    appendLine("   • OAuth : Vérifier les dépendances Google")
                }
            }
            
            // Sauvegarder le guide
            val file = File(context.getExternalFilesDir(null), CONFIGURATION_GUIDE_FILE)
            FileWriter(file).use { writer ->
                writer.write(guide)
            }
            
            // Afficher le guide
            AlertDialog.Builder(context)
                .setTitle("Guide de Configuration Généré")
                .setMessage("Le guide de configuration a été généré et sauvegardé dans :\n${file.absolutePath}\n\nVoulez-vous l'ouvrir ?")
                .setPositiveButton("Ouvrir") { _, _ ->
                    openFile(file)
                }
                .setNegativeButton("OK", null)
                .show()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error generating configuration guide", e)
            Toast.makeText(context, "Erreur lors de la génération du guide", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Ouvre la Google Console dans le navigateur
     */
    private fun openGoogleConsole() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com/"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Google Console", e)
            Toast.makeText(context, "Impossible d'ouvrir Google Console", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Ouvre un fichier avec l'application par défaut
     */
    private fun openFile(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "text/plain")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file", e)
            Toast.makeText(context, "Impossible d'ouvrir le fichier", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Teste la sauvegarde Google Drive
     */
    private fun testGoogleDriveBackup() {
        // Cette méthode pourrait être implémentée pour tester la sauvegarde
        Toast.makeText(context, "Test de sauvegarde Google Drive...", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Vérifie rapidement la configuration
     */
    fun quickCheck(): Boolean {
        val result = configurationManager.checkAndConfigureAll()
        return result.isValid
    }
    
    /**
     * Obtient le rapport de configuration
     */
    fun getConfigurationReport(): String {
        return configurationManager.generateConfigurationReport()
    }
}
