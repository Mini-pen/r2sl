package com.frombeyond.r2sl.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Gestionnaire de stockage local pour les données de profil
 */
class ProfileStorageManager(private val context: Context) {
    
    private val profileFile: File by lazy {
        try {
            val filesDir = context.filesDir
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            File(filesDir, "therapist_profile.json")
        } catch (e: Exception) {
            android.util.Log.e("ProfileStorageManager", "Erreur lors de la création du fichier de profil", e)
            // Fallback vers le cache directory
            File(context.cacheDir, "therapist_profile.json")
        }
    }
    
    /**
     * Sauvegarde les données de profil dans un fichier JSON local
     */
    fun saveProfile(profile: ProfileData): Boolean {
        return try {
            val json = JSONObject().apply {
                put("firstName", profile.firstName)
                put("lastName", profile.lastName)
                put("profession", profile.profession)
                put("apiKey", profile.apiKey)
                put("createdAt", profile.createdAt)
                put("updatedAt", profile.updatedAt)
            }
            
            // S'assurer que le répertoire parent existe
            profileFile.parentFile?.mkdirs()
            
            FileWriter(profileFile).use { writer ->
                writer.write(json.toString())
            }
            android.util.Log.d("ProfileStorageManager", "Profil sauvegardé avec succès")
            true
        } catch (e: Exception) {
            android.util.Log.e("ProfileStorageManager", "Erreur lors de la sauvegarde du profil", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Charge les données de profil depuis le fichier JSON local
     */
    fun loadProfile(): ProfileData? {
        return try {
            if (profileFile.exists() && profileFile.length() > 0) {
                FileReader(profileFile).use { reader ->
                    val jsonString = reader.readText()
                    
                    // Vérifier que le fichier n'est pas vide
                    if (jsonString.isBlank()) {
                        android.util.Log.w("ProfileStorageManager", "Fichier de profil vide")
                        return null
                    }
                    
                    val json = JSONObject(jsonString)
                    
                    // Vérifier que le JSON est valide et contient les champs requis
                    val profile = ProfileData(
                        firstName = json.optString("firstName", ""),
                        lastName = json.optString("lastName", ""),
                        profession = json.optString("profession", ""),
                        apiKey = json.optString("apiKey", ""),
                        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                    )
                    
                    android.util.Log.d("ProfileStorageManager", "Profil chargé avec succès: ${profile.getFullName()}")
                    profile
                }
            } else {
                android.util.Log.d("ProfileStorageManager", "Aucun fichier de profil trouvé ou fichier vide")
                null
            }
        } catch (e: org.json.JSONException) {
            android.util.Log.e("ProfileStorageManager", "Erreur JSON lors du chargement du profil", e)
            // Le fichier JSON est corrompu, le supprimer
            try {
                profileFile.delete()
                android.util.Log.i("ProfileStorageManager", "Fichier de profil corrompu supprimé")
            } catch (deleteException: Exception) {
                android.util.Log.e("ProfileStorageManager", "Erreur lors de la suppression du fichier corrompu", deleteException)
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("ProfileStorageManager", "Erreur lors du chargement du profil", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Vérifie si un profil existe
     */
    fun profileExists(): Boolean {
        return profileFile.exists()
    }
    
    /**
     * Supprime le fichier de profil
     */
    fun deleteProfile(): Boolean {
        return try {
            if (profileFile.exists()) {
                profileFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Crée un profil par défaut
     */
    fun createDefaultProfile(): ProfileData {
        return ProfileData(
            firstName = "Prénom",
            lastName = "Nom",
            profession = "Thérapeute",
            apiKey = ""
        )
    }
    
    /**
     * Charge un profil depuis un fichier spécifique
     */
    fun loadProfileFromFile(file: File): ProfileData? {
        return try {
            if (!file.exists()) {
                android.util.Log.w("ProfileStorageManager", "Fichier de profil n'existe pas: ${file.absolutePath}")
                return null
            }
            
            val content = file.readText()
            if (content.isBlank()) {
                android.util.Log.w("ProfileStorageManager", "Fichier de profil vide: ${file.absolutePath}")
                return null
            }
            
            val json = JSONObject(content)
            ProfileData(
                firstName = json.optString("firstName", ""),
                lastName = json.optString("lastName", ""),
                profession = json.optString("profession", ""),
                apiKey = json.optString("apiKey", ""),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            android.util.Log.e("ProfileStorageManager", "Erreur lors du chargement du profil depuis le fichier: ${file.absolutePath}", e)
            null
        }
    }
}
