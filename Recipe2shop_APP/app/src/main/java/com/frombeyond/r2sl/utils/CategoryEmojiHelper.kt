package com.frombeyond.r2sl.utils

/**
 * Helper class to get emoji for ingredient categories
 */
object CategoryEmojiHelper {
    
    private val categoryEmojis = mapOf(
        "viandes" to "🥩",
        "viande" to "🥩",
        "poissons" to "🐟",
        "poisson" to "🐟",
        "fruits et légumes frais" to "🥬",
        "fruits" to "🍎",
        "légumes" to "🥕",
        "épicerie salée" to "🧂",
        "épicerie" to "🧂",
        "conserve" to "🥫",
        "conserves" to "🥫",
        "produits laitiers" to "🥛",
        "laitier" to "🥛",
        "crèmerie" to "🥛",
        "fromage" to "🧀",
        "fromages" to "🧀",
        "boulangerie" to "🍞",
        "pain" to "🍞",
        "boissons" to "🥤",
        "boisson" to "🥤",
        "épicerie sucrée" to "🍬",
        "sucré" to "🍬",
        "épices" to "🌶️",
        "épice" to "🌶️",
        "huiles" to "🫒",
        "huile" to "🫒",
        "surgelé" to "❄️",
        "surgelés" to "❄️",
        "autres" to "📦",
        "Autres" to "📦"
    )
    
    /**
     * Get emoji for a category name
     * @param category The category name (case-insensitive)
     * @return The emoji for the category, or "📦" for "Autres" if not found
     */
    fun getEmoji(category: String): String {
        if (category.isBlank()) {
            return categoryEmojis["autres"] ?: "📦"
        }
        val normalized = category.lowercase().trim()
        return categoryEmojis[normalized] ?: categoryEmojis["autres"] ?: "📦"
    }
    
    /**
     * Format category name with emoji
     * @param category The category name
     * @return Formatted string with emoji and category name
     */
    fun formatCategory(category: String): String {
        val emoji = getEmoji(category)
        val displayName = if (category.isBlank()) "Autres" else category
        return "$emoji $displayName"
    }
}
