package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.local.IngredientEmojiManager
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
    private val emojiManager by lazy { IngredientEmojiManager(context) }

    fun generatePdf(entry: ShoppingListStorageManager.ShoppingListEntry, outputStream: OutputStream) {
        val writer = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        document.add(Paragraph(context.getString(R.string.shopping_list_detail_title)).setBold())

        val startDate = LocalDate.parse(entry.startDate)
        val endDate = LocalDate.parse(entry.endDate)
        val startDayName = startDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
        val endDayName = endDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
        document.add(Paragraph("$startDayName ${startDate.format(dateFormatter)} - $endDayName ${endDate.format(dateFormatter)}"))

        val activeItems = entry.items.filter { !it.canceled }
        val canceledItems = entry.items.filter { it.canceled }

        val grouped = activeItems.groupBy { it.category.ifBlank { "Autres" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

        grouped.forEach { (category, items) ->
            document.add(Paragraph(CategoryEmojiHelper.formatCategory(category)).setBold())
            items.forEach { item ->
                document.add(buildItemParagraph(item, strikethrough = false))
            }
        }

        if (canceledItems.isNotEmpty()) {
            val canceledCategoryName = context.getString(R.string.shopping_list_canceled_category)
            document.add(Paragraph(CategoryEmojiHelper.formatCategory(canceledCategoryName)).setBold())
            canceledItems.forEach { item ->
                document.add(buildItemParagraph(item, strikethrough = true))
            }
        }

        document.close()
    }

    private fun buildItemParagraph(
        item: ShoppingListStorageManager.ShoppingListItem,
        strikethrough: Boolean
    ): Paragraph {
        val unit = IngredientNormalizer.normalizeUnit(item.unit)
        val displayQty = if (item.mealSources.isNotEmpty()) {
            kotlin.math.ceil(item.quantity).toInt().toString()
        } else {
            IngredientNormalizer.formatQuantity(item.quantity)
        }
        val checkbox = if (item.checked) "☑" else "☐"
        val emoji = emojiManager.getSuggestions(item.name).firstOrNull()?.takeIf { it.isNotEmpty() }
        val namePart = if (!emoji.isNullOrEmpty()) "$emoji ${item.name}" else item.name
        val leftPart = "$checkbox $namePart : $displayQty $unit"
        val sourcesPart = formatMealSourcesSummary(item)

        val paragraph = Paragraph().setTextAlignment(TextAlignment.LEFT)
        if (sourcesPart.isNotEmpty()) {
            val tabWidth = 48
            val padding = " ".repeat(tabWidth.coerceAtLeast(leftPart.length + 2) - leftPart.length)
            val line = leftPart + padding + sourcesPart
            val text = Text(line)
            if (strikethrough) {
                text.setUnderline(1.5f, 3.5f)
            }
            paragraph.add(text)
        } else {
            val text = Text(leftPart)
            if (strikethrough) {
                text.setUnderline(1.5f, 3.5f)
            }
            paragraph.add(text)
        }
        return paragraph
    }

    private fun formatMealSourcesSummary(item: ShoppingListStorageManager.ShoppingListItem): String {
        if (item.mealSources.isEmpty()) {
            return ""
        }
        val unit = IngredientNormalizer.normalizeUnit(item.unit)
        return item.mealSources.joinToString(" · ") { source ->
            val date = LocalDate.parse(source.date)
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).replaceFirstChar { it.uppercase() }
            val dateShort = date.format(DateTimeFormatter.ofPattern("dd-MM"))
            val mealLabel = if (source.mealType == "lunch") {
                context.getString(R.string.weekly_menu_lunch)
            } else {
                context.getString(R.string.weekly_menu_dinner)
            }
            val qtyStr = source.quantityNeeded?.let { IngredientNormalizer.formatQuantityOneDecimal(it) } ?: ""
            val qtyPart = if (qtyStr.isNotEmpty()) "$qtyStr $unit – " else ""
            "$qtyPart${source.recipeName} ($dayName $dateShort, $mealLabel)"
        }
    }
}
