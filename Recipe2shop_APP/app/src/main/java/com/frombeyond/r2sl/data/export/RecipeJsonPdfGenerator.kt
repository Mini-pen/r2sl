package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.utils.IngredientNormalizer
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * * Generates PDF from RecipeJson format.
 * Includes all recipe elements and metadata footer.
 */
class RecipeJsonPdfGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)

    fun generatePdf(recipe: RecipeJson, outputStream: OutputStream) {
        try {
            val writer = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)

            // Recipe title
            val recipeTitle = Paragraph(recipe.name)
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10f)
            document.add(recipeTitle)

            // Description
            recipe.description?.let { description ->
                val descParagraph = Paragraph(description)
                    .setFontSize(12f)
                    .setMarginBottom(15f)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(descParagraph)
            }

            // Recipe metadata (servings, times, types, tags)
            val metadataText = buildString {
                append("Portions: ${recipe.servings}")
                recipe.workTime?.let { append(" | Travail: ${it} min") }
                recipe.prepTime?.let { append(" | Préparation: ${it} min") }
                recipe.cookTime?.let { append(" | Cuisson: ${it} min") }
                recipe.totalTime?.let { append(" | Total: ${it} min") }
                recipe.types?.let { types ->
                    if (types.isNotEmpty()) {
                        append(" | Types: ${types.joinToString(", ")}")
                    }
                }
                recipe.tags?.let { tags ->
                    if (tags.isNotEmpty()) {
                        append(" | Tags: ${tags.joinToString(", ")}")
                    }
                }
            }

            val metadataParagraph = Paragraph(metadataText)
                .setFontSize(11f)
                .setMarginBottom(20f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(metadataParagraph)

            // Ingredients section
            val ingredientsTitle = Paragraph("Ingrédients")
                .setFontSize(16f)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
            document.add(ingredientsTitle)

            // Ingredients table
            val ingredientsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
                .useAllAvailableWidth()
                .setMarginBottom(20f)

            recipe.ingredients.forEach { ingredient ->
                // Category and name
                val nameCell = Paragraph("${ingredient.category} - ${ingredient.name}")
                    .setFontSize(11f)
                ingredientsTable.addCell(nameCell)

                // Quantity alternatives (join with "OU")
                val quantityText = ingredient.quantity.joinToString(" OU ") { alt ->
                    "${IngredientNormalizer.formatQuantity(alt.nb)} ${IngredientNormalizer.normalizeUnit(alt.unit)}"
                }
                ingredientsTable.addCell(Paragraph(quantityText).setFontSize(11f).setTextAlignment(TextAlignment.RIGHT))

                // Notes
                val notesText = ingredient.notes ?: ""
                ingredientsTable.addCell(Paragraph(notesText).setFontSize(10f).setItalic())
            }

            document.add(ingredientsTable)

            // Steps section
            val stepsTitle = Paragraph("Étapes")
                .setFontSize(16f)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
            document.add(stepsTitle)

            // Steps list
            recipe.steps.sortedBy { it.stepOrder }.forEach { step ->
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

                // Sub-steps
                step.subSteps.sortedBy { it.subStepOrder }.forEach { subStep ->
                    val instructionParagraph = Paragraph("${subStep.subStepOrder}. ${subStep.instruction}")
                        .setFontSize(11f)
                        .setMarginLeft(10f)
                        .setMarginBottom(5f)
                    document.add(instructionParagraph)

                    // FALC version if available
                    subStep.instructionFalc?.let { falc ->
                        val falcParagraph = Paragraph("   (FALC: $falc)")
                            .setFontSize(10f)
                            .setItalic()
                            .setFontColor(ColorConstants.BLUE)
                            .setMarginBottom(5f)
                        document.add(falcParagraph)
                    }
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

                // Step ingredients if any
                step.ingredients?.let { stepIngredients ->
                    if (stepIngredients.isNotEmpty()) {
                        val stepIngTitle = Paragraph("Ingrédients pour cette étape:")
                            .setFontSize(10f)
                            .setBold()
                            .setMarginTop(5f)
                            .setMarginBottom(5f)
                        document.add(stepIngTitle)

                        stepIngredients.forEach { stepIng ->
                            val stepIngText = "${stepIng.ingredientName}: ${IngredientNormalizer.formatQuantity(stepIng.quantity)} ${IngredientNormalizer.normalizeUnit(stepIng.unit)}"
                            stepIng.notes?.let { stepIngText + " ($it)" }
                            val stepIngParagraph = Paragraph(stepIngText)
                                .setFontSize(10f)
                                .setMarginLeft(15f)
                                .setMarginBottom(3f)
                            document.add(stepIngParagraph)
                        }
                    }
                }
            }

            // Metadata footer
            recipe.metadata?.let { metadata ->
                document.add(Paragraph("\n\n").setFontSize(12f))
                
                val metadataBox = Paragraph()
                    .setBorder(com.itextpdf.layout.borders.SolidBorder(ColorConstants.GRAY, 1f))
                    .setPadding(10f)
                    .setMarginTop(20f)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)

                val metadataTitle = Paragraph("Métadonnées")
                    .setFontSize(12f)
                    .setBold()
                    .setMarginBottom(8f)
                metadataBox.add(metadataTitle)

                metadataBox.add(Paragraph("Date de création: ${dateFormat.format(Date(metadata.createdAt))}").setFontSize(10f))
                metadataBox.add(Paragraph("Dernière modification: ${dateFormat.format(Date(metadata.updatedAt))}").setFontSize(10f))
                metadataBox.add(Paragraph("Source: ${metadata.source}").setFontSize(10f))
                metadataBox.add(Paragraph("Auteur: ${metadata.author}").setFontSize(10f))
                metadataBox.add(Paragraph("Favoris: ${if (metadata.favorite) "Oui" else "Non"}").setFontSize(10f))
                metadataBox.add(Paragraph("Note: ${metadata.rating}/3").setFontSize(10f))
                metadata.exportedAt?.let {
                    metadataBox.add(Paragraph("Date d'export: ${dateFormat.format(Date(it))}").setFontSize(10f))
                }

                document.add(metadataBox)
            }

            document.close()
        } catch (e: Exception) {
            android.util.Log.e("RecipeJsonPdfGenerator", "Error generating PDF", e)
            throw e
        }
    }
}
