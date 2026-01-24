package com.frombeyond.r2sl.data

/**
 * Classe de données pour le profil du thérapeute
 */
data class ProfileData(
    val firstName: String = "",
    val lastName: String = "",
    val profession: String = "",
    val apiKey: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Vérifie si le profil est complet
     */
    fun isComplete(): Boolean {
        return firstName.isNotBlank() && 
               lastName.isNotBlank() && 
               profession.isNotBlank() && 
               apiKey.isNotBlank()
    }
    
    /**
     * Retourne le nom complet
     */
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }
    
    /**
     * Masque la clé API pour l'affichage
     */
    fun getMaskedApiKey(): String {
        return if (apiKey.length > 8) {
            "${apiKey.take(4)}...${apiKey.takeLast(4)}"
        } else {
            "sk-...xxxxxxxx"
        }
    }
}
