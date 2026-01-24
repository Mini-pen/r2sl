package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.data.local.ShoppingListStorageManager
import com.frombeyond.r2sl.utils.CategoryEmojiHelper
import com.frombeyond.r2sl.utils.IngredientNormalizer
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ShoppingListPdfGenerator(private val context: Context) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun generatePdf(entry: ShoppingListStorageManager.ShoppingListEntry, outputStream: OutputStream) {
        try {
            val writer = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)

            document.add(Paragraph("Liste de courses"))
            
            // Formater les dates
            val startDate = LocalDate.parse(entry.startDate)
            val endDate = LocalDate.parse(entry.endDate)
            val startDayName = startDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
            val endDayName = endDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
            document.add(Paragraph("$startDayName ${startDate.format(dateFormatter)} - $endDayName ${endDate.format(dateFormatter)}"))

            // Séparer les items annulés des autres
            val activeItems = entry.items.filter { !it.canceled }
            val canceledItems = entry.items.filter { it.canceled }

            // Grouper les items actifs par catégorie
            val grouped = activeItems.groupBy { it.category.ifBlank { "Autres" } }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)

            grouped.forEach { (category, items) ->
                document.add(Paragraph(category).setBold())
                items.forEach { item ->
                    val quantity = IngredientNormalizer.formatQuantity(item.quantity)
                    val unit = IngredientNormalizer.normalizeUnit(item.unit)
                    val checkbox = if (item.checked) "☑" else "☐"
                    val itemName = item.name
                    val line = "$checkbox $itemName : $quantity $unit"
                    document.add(Paragraph(line))
                }
            }

            // Ajouter la catégorie "Annulés" à la fin si nécessaire
            if (canceledItems.isNotEmpty()) {
                val canceledCategoryName = context.getString(com.frombeyond.r2sl.R.string.shopping_list_canceled_category)
                document.add(Paragraph(CategoryEmojiHelper.formatCategory(canceledCategoryName)).setBold())
                canceledItems.forEach { item ->
                    val quantity = IngredientNormalizer.formatQuantity(item.quantity)
                    val unit = IngredientNormalizer.normalizeUnit(item.unit)
                    val checkbox = if (item.checked) "☑" else "☐"
                    val itemName = item.name
                    // Créer un texte barré pour les items annulés
                    // Utiliser setUnderline avec une valeur positive pour créer un strikethrough
                    val text = Text("$checkbox $itemName : $quantity $unit")
                    text.setUnderline(1.5f, 3.5f) // 1.5f = épaisseur, 3.5f = position positive pour strikethrough
                    document.add(Paragraph().add(text))
                }
            }

            document.close()
        } catch (e: Exception) {
            android.util.Log.e("ShoppingListPdfGenerator", "Error generating PDF", e)
            throw e
        }
    }
}
