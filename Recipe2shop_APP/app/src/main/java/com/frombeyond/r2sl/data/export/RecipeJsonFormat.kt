package com.frombeyond.r2sl.data.export

import com.frombeyond.r2sl.data.database.entities.Ingredient
import com.frombeyond.r2sl.data.database.entities.Recipe
import com.frombeyond.r2sl.data.database.entities.RecipeStep
import org.json.JSONArray
import org.json.JSONObject

/**
 * * Formal JSON format for recipe export/import.
 * This format is designed to be portable and can be shared between applications.
 * 
 * Format structure:
 * {
 *   "version": "1.0",
 *   "recipes": [
 *     {
 *       "id": "...",
 *       "name": "...",
 *       "description": "...",
 *       "servings": 4,
 *       "prepTime": 15,
 *       "cookTime": 30,
 *       "category": "...",
 *       "imageUrl": "...",
 *       "ingredients": [
 *         {
 *           "name": "...",
 *           "quantity": 2.5,
 *           "unit": "cups",
 *           "notes": "..."
 *         }
 *       ],
 *       "steps": [
 *         {
 *           "stepOrder": 1,
 *           "instruction": "...",
 *           "instructionFalc": "...",
 *           "duration": 10,
 *           "temperature": 180,
 *           "notes": "..."
 *         }
 *       ],
 *       "metadata": {
 *         "createdAt": 1234567890,
 *         "updatedAt": 1234567890,
 *         "exportedAt": 1234567890
 *       }
 *     }
 *   ]
 * }
 */
data class RecipeJsonFormat(
    val version: String = "1.0",
    val recipes: List<RecipeJson>
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("version", version)
            val recipesArray = JSONArray()
            recipes.forEach { recipe ->
                recipesArray.put(recipe.toJsonObject())
            }
            put("recipes", recipesArray)
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): RecipeJsonFormat {
            val version = json.optString("version", "1.0")
            val recipesArray = json.getJSONArray("recipes")
            val recipes = mutableListOf<RecipeJson>()
            
            for (i in 0 until recipesArray.length()) {
                recipes.add(RecipeJson.fromJsonObject(recipesArray.getJSONObject(i)))
            }
            
            return RecipeJsonFormat(version, recipes)
        }
    }
}

/**
 * * JSON representation of a single recipe.
 * Supports multiple types, tags, work time, and step ingredients.
 */
data class RecipeJson(
    val id: String,
    val name: String,
    val description: String?,
    val servings: Int,
    val workTime: Int?, // Travail préparatoire (en minutes)
    val prepTime: Int?, // Temps de préparation additionnel (en minutes)
    val cookTime: Int?, // Temps de cuisson (en minutes)
    val totalTime: Int?, // Temps total = workTime + prepTime + cookTime (en minutes)
    val types: List<String>?, // Types de recette: "entrée", "apéritif", "plat principal", "dessert", "encas"
    val tags: List<String>?, // Tags libres pour catégorisation
    val imageUrl: String?,
    val ingredients: List<IngredientJson>,
    val steps: List<RecipeStepJson>,
    val metadata: RecipeMetadataJson?
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            description?.let { put("description", it) }
            put("servings", servings)
            workTime?.let { put("workTime", it) }
            prepTime?.let { put("prepTime", it) }
            cookTime?.let { put("cookTime", it) }
            totalTime?.let { put("totalTime", it) }
            
            types?.let { typesList ->
                val typesArray = JSONArray()
                typesList.forEach { typesArray.put(it) }
                put("types", typesArray)
            }
            
            tags?.let { tagsList ->
                val tagsArray = JSONArray()
                tagsList.forEach { tagsArray.put(it) }
                put("tags", tagsArray)
            }
            
            imageUrl?.let { put("imageUrl", it) }
            
            val ingredientsArray = JSONArray()
            ingredients.forEach { ingredient ->
                ingredientsArray.put(ingredient.toJsonObject())
            }
            put("ingredients", ingredientsArray)
            
            val stepsArray = JSONArray()
            steps.forEach { step ->
                stepsArray.put(step.toJsonObject())
            }
            put("steps", stepsArray)
            
            metadata?.let { put("metadata", it.toJsonObject()) }
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): RecipeJson {
            // Parse types array
            val types = if (json.has("types")) {
                val typesArray = json.getJSONArray("types")
                (0 until typesArray.length()).map { typesArray.getString(it) }
            } else null
            
            // Parse tags array
            val tags = if (json.has("tags")) {
                val tagsArray = json.getJSONArray("tags")
                (0 until tagsArray.length()).map { tagsArray.getString(it) }
            } else null
            
            return RecipeJson(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description").takeIf { it.isNotEmpty() },
                servings = json.optInt("servings", 1).coerceAtLeast(1),
                workTime = json.optInt("workTime").takeIf { it > 0 },
                prepTime = json.optInt("prepTime").takeIf { it > 0 },
                cookTime = json.optInt("cookTime").takeIf { it > 0 },
                totalTime = json.optInt("totalTime").takeIf { it > 0 },
                types = types,
                tags = tags,
                imageUrl = json.optString("imageUrl").takeIf { it.isNotEmpty() },
                ingredients = parseIngredients(json.getJSONArray("ingredients")),
                steps = parseSteps(json.getJSONArray("steps")),
                metadata = if (json.has("metadata")) {
                    RecipeMetadataJson.fromJsonObject(json.getJSONObject("metadata"))
                } else null
            )
        }
        
        private fun parseIngredients(array: JSONArray): List<IngredientJson> {
            val ingredients = mutableListOf<IngredientJson>()
            for (i in 0 until array.length()) {
                ingredients.add(IngredientJson.fromJsonObject(array.getJSONObject(i)))
            }
            return ingredients
        }
        
        private fun parseSteps(array: JSONArray): List<RecipeStepJson> {
            val steps = mutableListOf<RecipeStepJson>()
            for (i in 0 until array.length()) {
                steps.add(RecipeStepJson.fromJsonObject(array.getJSONObject(i)))
            }
            return steps.sortedBy { it.stepOrder }
        }
    }
}

/**
 * * JSON representation of a quantity alternative (nb + unit).
 */
data class QuantityAlternative(
    val nb: Double,
    val unit: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("nb", nb)
            put("unit", unit)
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): QuantityAlternative {
            return QuantityAlternative(
                nb = json.getDouble("nb"),
                unit = json.getString("unit")
            )
        }
    }
}

/**
 * * JSON representation of an ingredient.
 * Supports multiple quantity alternatives (e.g., 3 pieces OR 300g).
 */
data class IngredientJson(
    val name: String,
    val category: String,
    val quantity: List<QuantityAlternative>, // List of quantity alternatives
    val notes: String?,
    val emoji: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("category", category)
            
            val quantityArray = JSONArray()
            quantity.forEach { alt ->
                quantityArray.put(alt.toJsonObject())
            }
            put("quantity", quantityArray)
            
            notes?.let { put("notes", it) }
            emoji?.let { put("emoji", it) }
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): IngredientJson {
            val name = json.getString("name")
            val category = json.optString("category", "").ifBlank { "Autres" }
            val notes = json.optString("notes").takeIf { it.isNotEmpty() }
            val emoji = json.optString("emoji").takeIf { it.isNotEmpty() }
            
            val quantityArray = json.getJSONArray("quantity")
            val alternatives = mutableListOf<QuantityAlternative>()
            for (i in 0 until quantityArray.length()) {
                alternatives.add(QuantityAlternative.fromJsonObject(quantityArray.getJSONObject(i)))
            }
            
            return IngredientJson(
                name = name,
                category = category,
                quantity = alternatives,
                notes = notes,
                emoji = emoji
            )
        }
    }
}

/**
 * * JSON representation of a sub-step within a recipe step.
 * Each sub-step contains an instruction with both standard and FALC versions.
 */
data class SubStepJson(
    val subStepOrder: Int, // Order of the sub-step within the step
    val instruction: String, // Standard instruction text
    val instructionFalc: String? // FALC version of the instruction
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("subStepOrder", subStepOrder)
            put("instruction", instruction)
            instructionFalc?.let { put("instructionFalc", it) }
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): SubStepJson {
            return SubStepJson(
                subStepOrder = json.getInt("subStepOrder"),
                instruction = json.getString("instruction"),
                instructionFalc = json.optString("instructionFalc").takeIf { it.isNotEmpty() }
            )
        }
    }
}

/**
 * * JSON representation of a recipe step.
 * Each step contains a title, duration, temperature, notes, ingredients, and a list of sub-steps.
 */
data class RecipeStepJson(
    val stepOrder: Int,
    val title: String?, // Title of the step
    val duration: Int?, // Duration in minutes for this step
    val temperature: String?, // Temperature (e.g., "180°C", "medium heat")
    val notes: String?, // Optional notes
    val ingredients: List<StepIngredientJson>?, // Ingredients used in this step with quantities
    val subSteps: List<SubStepJson> // List of sub-steps, each with instruction and instructionFalc
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("stepOrder", stepOrder)
            title?.let { put("title", it) }
            duration?.let { put("duration", it) }
            temperature?.let { put("temperature", it) }
            notes?.let { put("notes", it) }
            
            ingredients?.let { ingredientsList ->
                val ingredientsArray = JSONArray()
                ingredientsList.forEach { ingredient ->
                    ingredientsArray.put(ingredient.toJsonObject())
                }
                put("ingredients", ingredientsArray)
            }
            
            val subStepsArray = JSONArray()
            subSteps.forEach { subStep ->
                subStepsArray.put(subStep.toJsonObject())
            }
            put("subSteps", subStepsArray)
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): RecipeStepJson {
            // Parse step ingredients
            val stepIngredients = if (json.has("ingredients")) {
                val ingredientsArray = json.getJSONArray("ingredients")
                (0 until ingredientsArray.length()).map { 
                    StepIngredientJson.fromJsonObject(ingredientsArray.getJSONObject(it))
                }
            } else null
            
            // Parse sub-steps
            val subStepsArray = json.getJSONArray("subSteps")
            val subSteps = (0 until subStepsArray.length()).map {
                SubStepJson.fromJsonObject(subStepsArray.getJSONObject(it))
            }.sortedBy { it.subStepOrder }
            
            return RecipeStepJson(
                stepOrder = json.getInt("stepOrder"),
                title = json.optString("title").takeIf { it.isNotEmpty() },
                duration = json.optInt("duration").takeIf { it > 0 },
                temperature = json.optString("temperature").takeIf { it.isNotEmpty() },
                notes = json.optString("notes").takeIf { it.isNotEmpty() },
                ingredients = stepIngredients,
                subSteps = subSteps
            )
        }
    }
}

/**
 * * JSON representation of an ingredient used in a specific recipe step.
 */
data class StepIngredientJson(
    val ingredientName: String, // Name of the ingredient (must match an ingredient from recipe's ingredient list)
    val quantity: Double, // Quantity used in this step
    val unit: String, // Unit of measurement
    val notes: String? // Optional notes (e.g., "chopped", "diced")
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("ingredientName", ingredientName)
            put("quantity", quantity)
            put("unit", unit)
            notes?.let { put("notes", it) }
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): StepIngredientJson {
            return StepIngredientJson(
                ingredientName = json.getString("ingredientName"),
                quantity = json.getDouble("quantity"),
                unit = json.getString("unit"),
                notes = json.optString("notes").takeIf { it.isNotEmpty() }
            )
        }
    }
}

/**
 * * JSON representation of recipe metadata.
 * Metadata fields are not displayed in the UI but stored for tracking purposes.
 */
data class RecipeMetadataJson(
    val createdAt: Long,
    val updatedAt: Long,
    val exportedAt: Long?,
    val source: String = "manual_entry", // "manual_entry", "website", "book", "r2sl_recipes_pack"
    val author: String = "unknown", // Author name or "unknown"
    val favorite: Boolean = false, // Whether the recipe is favorited
    val rating: Int = 0 // Rating from 0 to 3
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            exportedAt?.let { put("exportedAt", it) }
            put("source", source)
            put("author", author)
            put("favorite", favorite)
            put("rating", rating)
        }
    }
    
    companion object {
        fun fromJsonObject(json: JSONObject): RecipeMetadataJson {
            return RecipeMetadataJson(
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                exportedAt = json.optLong("exportedAt").takeIf { it > 0 },
                source = json.optString("source", "manual_entry"),
                author = json.optString("author", "unknown"),
                favorite = json.optBoolean("favorite", false),
                rating = json.optInt("rating", 0).coerceIn(0, 3)
            )
        }
        
        // * Creates default metadata for a new recipe
        fun createDefault(): RecipeMetadataJson {
            val now = System.currentTimeMillis()
            return RecipeMetadataJson(
                createdAt = now,
                updatedAt = now,
                exportedAt = null,
                source = "manual_entry",
                author = "unknown",
                favorite = false,
                rating = 0
            )
        }
    }
}
