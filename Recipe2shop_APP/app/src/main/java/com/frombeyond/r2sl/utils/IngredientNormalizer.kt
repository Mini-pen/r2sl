package com.frombeyond.r2sl.utils

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object IngredientNormalizer {

    fun normalizeName(name: String): String {
        val normalized = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
        val noAccents = normalized.replace("\\p{Mn}+".toRegex(), "")
        val cleaned = noAccents.replace("[^a-z0-9 ]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()
        return if (cleaned.endsWith("s") && cleaned.length > 3) {
            cleaned.dropLast(1)
        } else {
            cleaned
        }
    }

    fun formatQuantity(value: Double): String {
        return if (abs(value % 1.0) < 0.0001) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    /**
     * Formats quantity with at most 1 decimal place (for info/portions display).
     * Rounds to 1 decimal; shows integer when value is whole.
     */
    fun formatQuantityOneDecimal(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (abs(rounded - rounded.toLong().toDouble()) < 0.001) {
            rounded.toLong().toString()
        } else {
            "%.1f".format(Locale.ROOT, rounded)
        }
    }

    fun normalizeUnit(unit: String): String {
        return unit.ifBlank { "piece" }
    }
}
