package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.data.database.Recipe2shopDatabase
import com.frombeyond.r2sl.data.database.entities.Ingredient
import com.frombeyond.r2sl.data.database.entities.Recipe
import com.frombeyond.r2sl.data.database.entities.RecipeStep
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * * Service for generating PDF cookbooks from recipes.
 * Supports FALC (Facile À Lire et à Comprendre) mode for simplified, easy-to-read format.
 */
class RecipePdfGenerator(
    private val context: Context,
    private val database: Recipe2shopDatabase
) {
    
    /**
     * * Generates a PDF cookbook containing one or more recipes.
     * @param recipeIds List of recipe IDs to include in the PDF
     * @param outputFile File to write the PDF to
     * @param falcMode If true, uses FALC versions of instructions
     * @param title Title of the cookbook
     * @return true if generation was successful, false otherwise
     */
    suspend fun generateCookbookPdf(
        recipeIds: List<String>,
        outputFile: File,
        falcMode: Boolean = false,
        title: String = "Livre de Recettes"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val recipeDao = database.recipeDao()
            val ingredientDao = database.ingredientDao()
            val stepDao = database.recipeStepDao()
            val subStepDao = database.subStepDao()
            
            val recipes = recipeIds.mapNotNull { recipeDao.getRecipeById(it) }
            
            if (recipes.isEmpty()) {
                android.util.Log.e("RecipePdfGenerator", "No recipes to generate PDF")
                return@withContext false
            }
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            val writer = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)
            
            // Title page
            addTitlePage(document, title, falcMode)
            
            // Add each recipe
            recipes.forEachIndexed { index, recipe ->
                if (index > 0) {
                    document.add(Paragraph("\n").setFontSize(12f))
                }
                
                val ingredients = ingredientDao.getIngredientsByRecipeSync(recipe.id)
                val steps = stepDao.getStepsByRecipeSync(recipe.id)
                
                // Get all sub-steps for all steps
                val stepsWithSubSteps = steps.map { step ->
                    val subSteps = subStepDao.getSubStepsByStepSync(step.id)
                    step to subSteps
                }
                
                addRecipeToDocument(document, recipe, ingredients, stepsWithSubSteps, falcMode)
            }
            
            document.close()
            true
        } catch (e: Exception) {
            android.util.Log.e("RecipePdfGenerator", "Error generating PDF", e)
            false
        }
    }
    
    private fun addTitlePage(document: Document, title: String, falcMode: Boolean) {
        val titleParagraph = Paragraph(title)
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        
        document.add(titleParagraph)
        
        if (falcMode) {
            val falcLabel = Paragraph("Mode FALC - Facile À Lire et à Comprendre")
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(40f)
                .setFontColor(ColorConstants.BLUE)
            
            document.add(falcLabel)
        }
        
        document.add(Paragraph("\n\n"))
    }
    
    private fun addRecipeToDocument(
        document: Document,
        recipe: Recipe,
        ingredients: List<Ingredient>,
        stepsWithSubSteps: List<Pair<RecipeStep, List<com.frombeyond.r2sl.data.database.entities.SubStep>>>,
        falcMode: Boolean
    ) {
        // Recipe title
        val recipeTitle = Paragraph(recipe.name)
            .setFontSize(20f)
            .setBold()
            .setMarginBottom(10f)
        
        document.add(recipeTitle)
        
        // Description
        recipe.description?.let { description ->
            val descParagraph = Paragraph(description)
                .setFontSize(12f)
                .setMarginBottom(15f)
                .setItalic()
            
            document.add(descParagraph)
        }
        
        // Recipe metadata
        val metadataText = buildString {
            append("Portions: ${recipe.servings}")
            recipe.workTime?.let { append(" | Travail: ${it} min") }
            recipe.prepTime?.let { append(" | Préparation: ${it} min") }
            recipe.cookTime?.let { append(" | Cuisson: ${it} min") }
            recipe.totalTime?.let { append(" | Total: ${it} min") }
            recipe.types?.let { types ->
                val typesList = types.split(",").map { it.trim() }
                if (typesList.isNotEmpty()) {
                    append(" | Types: ${typesList.joinToString(", ")}")
                }
            }
        }
        
        val metadataParagraph = Paragraph(metadataText)
            .setFontSize(11f)
            .setMarginBottom(20f)
        
        document.add(metadataParagraph)
        
        // Ingredients section
        val ingredientsTitle = Paragraph("Ingrédients")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(15f)
            .setMarginBottom(10f)
        
        document.add(ingredientsTitle)
        
        // Ingredients table
        val ingredientsTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 20f, 30f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        ingredients.forEach { ingredient ->
            ingredientsTable.addCell(Paragraph(ingredient.name).setFontSize(11f))
            ingredientsTable.addCell(Paragraph("${ingredient.quantity}").setFontSize(11f).setTextAlignment(TextAlignment.RIGHT))
            ingredientsTable.addCell(Paragraph(ingredient.unit).setFontSize(11f))
            ingredient.notes?.let { notes ->
                val notesCell = Cell(1, 3).add(Paragraph("Notes: $notes").setFontSize(10f).setItalic())
                ingredientsTable.addCell(notesCell)
            }
        }
        
        document.add(ingredientsTable)
        
        // Steps section
        val stepsTitle = Paragraph(if (falcMode) "Étapes (FALC)" else "Étapes")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(15f)
            .setMarginBottom(10f)
        
        if (falcMode) {
            stepsTitle.setFontColor(ColorConstants.BLUE)
        }
        
        document.add(stepsTitle)
        
        // Steps list
        stepsWithSubSteps.sortedBy { it.first.stepOrder }.forEach { (step, subSteps) ->
            val stepTitle = if (step.title != null) {
                "Étape ${step.stepOrder}: ${step.title}"
            } else {
                "Étape ${step.stepOrder}:"
            }
            
            val stepNumber = Paragraph(stepTitle)
                .setFontSize(12f)
                .setBold()
                .setMarginTop(10f)
            
            document.add(stepNumber)
            
            // Use sorted sub-steps
            val sortedSubSteps = subSteps.sortedBy { it.subStepOrder }
            
            sortedSubSteps.forEach { subStep ->
                // Use FALC version if available and FALC mode is enabled
                val instructionText = if (falcMode && !subStep.instructionFalc.isNullOrBlank()) {
                    subStep.instructionFalc!!
                } else {
                    subStep.instruction
                }
                
                val instructionParagraph = Paragraph(instructionText)
                    .setFontSize(11f)
                    .setMarginLeft(10f)
                    .setMarginBottom(5f)
                
                if (falcMode && !subStep.instructionFalc.isNullOrBlank()) {
                    instructionParagraph.setFontColor(ColorConstants.BLUE)
                }
                
                document.add(instructionParagraph)
            }
            
            // Step metadata
            val stepMetadata = buildString {
                step.duration?.let { append("Durée: ${it} min") }
                step.temperature?.let {
                    if (isNotEmpty()) append(" | ")
                    append("Température: ${it}")
                }
            }
            
            if (stepMetadata.isNotEmpty()) {
                val stepMetaParagraph = Paragraph(stepMetadata)
                    .setFontSize(10f)
                    .setItalic()
                    .setMarginBottom(10f)
                
                document.add(stepMetaParagraph)
            }
            
            step.notes?.let { notes ->
                val notesParagraph = Paragraph("Note: $notes")
                    .setFontSize(10f)
                    .setItalic()
                    .setMarginBottom(10f)
                
                document.add(notesParagraph)
            }
        }
        
        // Page break between recipes
        document.add(Paragraph("\n").setFontSize(12f))
    }
}
