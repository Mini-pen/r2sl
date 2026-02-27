package com.frombeyond.r2sl.ui.shoppinglists

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.frombeyond.r2sl.ui.BaseFragment
import com.frombeyond.r2sl.R
import com.frombeyond.r2sl.data.export.ShoppingListPdfGenerator
import com.frombeyond.r2sl.data.local.IngredientEmojiManager
import com.frombeyond.r2sl.data.local.ShoppingListStorageManager
import com.frombeyond.r2sl.utils.CategoryEmojiHelper
import com.frombeyond.r2sl.utils.IngredientNormalizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ShoppingListDetailFragment : BaseFragment() {

    private lateinit var storageManager: ShoppingListStorageManager
    private lateinit var itemsContainer: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var datesText: TextView
    private lateinit var inputName: TextInputEditText
    private lateinit var inputQuantity: TextInputEditText
    private lateinit var inputCategory: TextInputEditText
    private lateinit var addButton: MaterialButton
    private lateinit var exportButton: MaterialButton
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    private var listId: String? = null
    private var listEntry: ShoppingListStorageManager.ShoppingListEntry? = null
    private val emojiManager by lazy { IngredientEmojiManager(requireContext()) }

    private val exportPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            exportPdfToUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_shopping_list_detail, container, false)
        storageManager = ShoppingListStorageManager(requireContext())
        itemsContainer = root.findViewById(R.id.shopping_list_items_container)
        titleText = root.findViewById(R.id.shopping_list_detail_title)
        datesText = root.findViewById(R.id.shopping_list_detail_dates)
        inputName = root.findViewById(R.id.input_manual_name)
        inputQuantity = root.findViewById(R.id.input_manual_quantity)
        inputCategory = root.findViewById(R.id.input_manual_category)
        addButton = root.findViewById(R.id.button_manual_add)
        exportButton = root.findViewById(R.id.button_export_shopping_list_pdf)

        listId = arguments?.getString(ARG_LIST_ID)
        loadList()

        addButton.setOnClickListener {
            addManualItem()
        }
        exportButton.setOnClickListener {
            exportPdf()
        }
        return root
    }

    private fun loadList() {
        val id = listId ?: return
        listEntry = storageManager.getListById(id)
        renderList()
    }

    private fun renderList() {
        itemsContainer.removeAllViews()
        val entry = listEntry ?: return
        titleText.text = getString(R.string.shopping_list_detail_title)

        // Afficher les dates
        val startDate = LocalDate.parse(entry.startDate)
        val endDate = LocalDate.parse(entry.endDate)
        val startDayName = startDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
        val endDayName = endDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
        datesText.text = "$startDayName ${startDate.format(dateFormatter)} - $endDayName ${endDate.format(dateFormatter)}"

        // Séparer les items annulés des autres
        val activeItems = entry.items.filter { !it.canceled }
        val canceledItems = entry.items.filter { it.canceled }

        // Grouper les items actifs par catégorie
        val grouped = activeItems.groupBy { it.category.ifBlank { "Autres" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

        grouped.forEach { (category, items) ->
            val categoryView = layoutInflater.inflate(
                R.layout.item_shopping_list_category,
                itemsContainer,
                false
            ) as TextView
            categoryView.text = CategoryEmojiHelper.formatCategory(category)
            itemsContainer.addView(categoryView)

            items.forEach { item ->
                addItemView(item)
            }
        }

        // Ajouter la catégorie "Annulés" à la fin si nécessaire
        if (canceledItems.isNotEmpty()) {
            val categoryView = layoutInflater.inflate(
                R.layout.item_shopping_list_category,
                itemsContainer,
                false
            ) as TextView
            val canceledCategoryName = getString(R.string.shopping_list_canceled_category)
            categoryView.text = CategoryEmojiHelper.formatCategory(canceledCategoryName)
            itemsContainer.addView(categoryView)

            canceledItems.forEach { item ->
                addItemView(item)
            }
        }
    }

    private fun addItemView(item: ShoppingListStorageManager.ShoppingListItem) {
        val itemView = layoutInflater.inflate(
            R.layout.item_shopping_list_item,
            itemsContainer,
            false
        )
        val checkbox = itemView.findViewById<CheckBox>(R.id.shopping_item_checkbox)
        val nameText = itemView.findViewById<TextView>(R.id.shopping_item_name)
        val quantityText = itemView.findViewById<TextView>(R.id.shopping_item_quantity)
        val removeButton = itemView.findViewById<MaterialButton>(R.id.shopping_item_remove)

        checkbox.isChecked = item.checked
        val ingredientEmoji = emojiManager.getSuggestions(item.name).firstOrNull()
        nameText.text = if (!ingredientEmoji.isNullOrEmpty()) "$ingredientEmoji ${item.name}" else item.name

        val displayQty = if (item.mealSources.isNotEmpty()) {
            kotlin.math.ceil(item.quantity).toInt().toString()
        } else {
            IngredientNormalizer.formatQuantity(item.quantity)
        }
        quantityText.text = "$displayQty ${IngredientNormalizer.normalizeUnit(item.unit)}"

        val infoPortionsView = itemView.findViewById<TextView>(R.id.shopping_item_info_portions)
        val hasFractionalPortions = item.mealSources.any { source ->
            val q = source.quantityNeeded ?: 0.0
            kotlin.math.abs(q - q.toLong().toDouble()) > 1e-6
        }
        infoPortionsView.visibility = if (hasFractionalPortions) View.VISIBLE else View.GONE
        infoPortionsView.setOnClickListener {
            showPortionsInfoDialog(item)
        }

        val isCanceled = item.canceled
        checkbox.isEnabled = !isCanceled
        nameText.paint.isStrikeThruText = isCanceled

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (!isCanceled) {
                updateItemChecked(item, isChecked)
            }
        }
        
        // * Ajouter un listener sur la quantité pour saisir la quantité restante
        quantityText.setOnClickListener {
            if (!isCanceled) {
                showQuantityRemainingDialog(item)
            }
        }
        
        // Changer le bouton selon l'état de l'item
        if (isCanceled) {
            // Bouton de restauration pour les items annulés
            removeButton.setIconResource(R.drawable.ic_restore_24)
            removeButton.contentDescription = getString(R.string.shopping_list_restore)
            removeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.secondary_green)
            ))
            removeButton.setOnClickListener {
                restoreItem(item)
            }
        } else {
            // Bouton de suppression pour les items actifs
            removeButton.setIconResource(R.drawable.ic_close_24)
            removeButton.contentDescription = getString(R.string.shopping_lists_cancel)
            removeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.error_color)
            ))
            removeButton.setOnClickListener {
                cancelItem(item)
            }
        }

        // Ajouter un listener sur le nom pour afficher les repas sources
        if (item.mealSources.isNotEmpty()) {
            nameText.setOnClickListener {
                showMealSourcesDialog(item)
            }
            nameText.setTextColor(requireContext().getColor(R.color.primary_blue))
            nameText.paintFlags = nameText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        }

        itemsContainer.addView(itemView)
    }

    private fun showMealSourcesDialog(item: ShoppingListStorageManager.ShoppingListItem) {
        val unit = IngredientNormalizer.normalizeUnit(item.unit)
        val mealSourcesText = item.mealSources.map { source ->
            val date = LocalDate.parse(source.date)
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
            val dateShort = date.format(DateTimeFormatter.ofPattern("dd-MM"))
            val mealTypeLabel = if (source.mealType == "lunch") {
                getString(R.string.weekly_menu_lunch)
            } else {
                getString(R.string.weekly_menu_dinner)
            }
            val qtyStr = source.quantityNeeded?.let { IngredientNormalizer.formatQuantityOneDecimal(it) } ?: ""
            val qtyPart = if (qtyStr.isNotEmpty()) "$qtyStr $unit – " else ""
            "$qtyPart${source.recipeName} ($dayName $dateShort – $mealTypeLabel)"
        }.joinToString("\n")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setMessage(mealSourcesText)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showPortionsInfoDialog(item: ShoppingListStorageManager.ShoppingListItem) {
        val unit = IngredientNormalizer.normalizeUnit(item.unit)
        val lines = item.mealSources.map { source ->
            val qty = source.quantityNeeded ?: 0.0
            getString(R.string.shopping_list_portions_info_line, IngredientNormalizer.formatQuantityOneDecimal(qty), unit, source.recipeName)
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setMessage(getString(R.string.shopping_list_portions_info_title) + "\n\n" + lines.joinToString("\n"))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun updateItemChecked(item: ShoppingListStorageManager.ShoppingListItem, checked: Boolean) {
        val entry = listEntry ?: return
        val updatedItems = entry.items.map {
            // Comparer aussi les mealSources pour distinguer les items manuels des items de recettes
            if (it.name == item.name && it.category == item.category && it.unit == item.unit && 
                it.mealSources == item.mealSources) {
                it.copy(checked = checked)
            } else {
                it
            }
        }
        val updated = entry.copy(items = updatedItems)
        listEntry = updated
        storageManager.updateList(updated)
    }

    private fun addManualItem() {
        val entry = listEntry ?: return
        val name = inputName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            return
        }
        val quantity = inputQuantity.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 1.0
        val category = inputCategory.text?.toString()?.trim().orEmpty().ifEmpty { "Autres" }
        val unit = "piece"

        val updatedItems = entry.items + ShoppingListStorageManager.ShoppingListItem(
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            checked = false,
            canceled = false,
            mealSources = emptyList()
        )
        val updated = entry.copy(items = updatedItems)
        listEntry = updated
        storageManager.updateList(updated)
        inputName.setText("")
        inputQuantity.setText("")
        inputCategory.setText("")
        renderList()
    }

    private fun cancelItem(item: ShoppingListStorageManager.ShoppingListItem) {
        val entry = listEntry ?: return
        val updatedItems = entry.items.map {
            // Comparer aussi les mealSources pour distinguer les items manuels des items de recettes
            if (it.name == item.name && it.category == item.category && it.unit == item.unit && 
                it.mealSources == item.mealSources) {
                it.copy(canceled = true, checked = false)
            } else {
                it
            }
        }
        val updated = entry.copy(items = updatedItems)
        listEntry = updated
        storageManager.updateList(updated)
        renderList()
    }

    private fun restoreItem(item: ShoppingListStorageManager.ShoppingListItem) {
        val entry = listEntry ?: return
        val updatedItems = entry.items.map {
            // Comparer aussi les mealSources pour distinguer les items manuels des items de recettes
            if (it.name == item.name && it.category == item.category && it.unit == item.unit && 
                it.mealSources == item.mealSources) {
                it.copy(canceled = false, checked = false)
            } else {
                it
            }
        }
        val updated = entry.copy(items = updatedItems)
        listEntry = updated
        storageManager.updateList(updated)
        renderList()
    }
    
    // * Affiche un dialogue pour saisir la quantité restante à la maison
    private fun showQuantityRemainingDialog(item: ShoppingListStorageManager.ShoppingListItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quantity_remaining, null)
        val inputQuantity = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_quantity_remaining)
        
        // Pré-remplir avec la quantité actuelle
        inputQuantity.setText(IngredientNormalizer.formatQuantity(item.quantity))
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Quantité restante: ${item.name}")
            .setMessage("Indiquez la quantité que vous avez déjà à la maison (en ${IngredientNormalizer.normalizeUnit(item.unit)})")
            .setView(dialogView)
            .setPositiveButton("Valider") { _, _ ->
                val remainingQuantity = inputQuantity.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                updateQuantityWithRemaining(item, remainingQuantity)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    // * Met à jour la quantité en soustrayant la quantité restante
    private fun updateQuantityWithRemaining(item: ShoppingListStorageManager.ShoppingListItem, remainingQuantity: Double) {
        val entry = listEntry ?: return
        
        // Calculer la nouvelle quantité (quantité nécessaire - quantité restante)
        val newQuantity = item.quantity - remainingQuantity
        
        // Si le résultat est < 0, mettre 0 et annuler l'item
        val finalQuantity = if (newQuantity < 0) 0.0 else newQuantity
        val shouldCancel = newQuantity <= 0
        
        val updatedItems = entry.items.map {
            // Comparer aussi les mealSources pour distinguer les items manuels des items de recettes
            if (it.name == item.name && it.category == item.category && it.unit == item.unit && 
                it.mealSources == item.mealSources) {
                it.copy(
                    quantity = finalQuantity,
                    canceled = shouldCancel,
                    checked = false
                )
            } else {
                it
            }
        }
        
        val updated = entry.copy(items = updatedItems)
        listEntry = updated
        storageManager.updateList(updated)
        renderList()
        
        // Afficher un message informatif
        val message = if (shouldCancel) {
            "Quantité mise à jour. L'item a été annulé car vous avez déjà assez à la maison."
        } else {
            "Quantité mise à jour: ${IngredientNormalizer.formatQuantity(finalQuantity)} ${IngredientNormalizer.normalizeUnit(item.unit)} restant à acheter."
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun exportPdf() {
        val entry = listEntry ?: return
        val fileName = "liste_courses_${entry.startDate}_${entry.endDate}.pdf"
        exportPdfLauncher.launch(fileName)
    }

    private fun exportPdfToUri(uri: Uri) {
        val entry = listEntry ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                val generator = ShoppingListPdfGenerator(requireContext())
                generator.generatePdf(entry, output)
            }
            Toast.makeText(requireContext(), R.string.shopping_list_export_success, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), R.string.shopping_list_export_error, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_LIST_ID = "shoppingListId"
    }
}
