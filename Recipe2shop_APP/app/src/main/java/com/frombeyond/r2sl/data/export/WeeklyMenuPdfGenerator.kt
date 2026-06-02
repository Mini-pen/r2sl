package com.frombeyond.r2sl.data.export

import android.content.Context
import com.frombeyond.r2sl.data.local.DishStorageManager
import com.frombeyond.r2sl.data.local.MenuStorageManager
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeeklyMenuPdfGenerator(private val context: Context) {

    fun generatePdf(
        days: List<LocalDate>,
        menuStorage: MenuStorageManager,
        dishStorage: DishStorageManager,
        outputStream: OutputStream
    ) {
        val writer = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        document.add(Paragraph(context.getString(com.frombeyond.r2sl.R.string.weekly_menu_title)).setBold().setFontSize(16f))

        val colCount = days.size.coerceAtLeast(1)
        val table = Table(UnitValue.createPercentArray(FloatArray(colCount) { 100f / colCount }))
            .useAllAvailableWidth()

        days.forEach { date ->
            val dayTitle = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                .replaceFirstChar { it.uppercase() } + "\n" + date.format(DATE_FORMAT)
            val lunchText = formatMealCell(date, MenuStorageManager.MEAL_LUNCH, menuStorage, dishStorage)
            val dinnerText = formatMealCell(date, MenuStorageManager.MEAL_DINNER, menuStorage, dishStorage)
            val cellContent = "$dayTitle\n\n${context.getString(com.frombeyond.r2sl.R.string.weekly_menu_lunch)}:\n$lunchText\n\n${context.getString(com.frombeyond.r2sl.R.string.weekly_menu_dinner)}:\n$dinnerText"
            table.addCell(
                Cell().add(Paragraph(cellContent).setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
            )
        }

        document.add(table)
        document.close()
    }

    private fun formatMealCell(
        date: LocalDate,
        mealType: String,
        menuStorage: MenuStorageManager,
        dishStorage: DishStorageManager
    ): String {
        val assignments = menuStorage.getAssignments(date, mealType)
        if (assignments.isEmpty()) {
            return "—"
        }
        return assignments.mapNotNull { assignment ->
            val dish = dishStorage.getDishById(assignment.dishId) ?: return@mapNotNull null
            val portionsSuffix = if (assignment.portions > 1) " (×${assignment.portions})" else ""
            dish.name + portionsSuffix
        }.joinToString("\n")
    }

    companion object {
        private val DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
    }
}
