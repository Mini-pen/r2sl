package com.frombeyond.r2sl.utils

import java.text.Normalizer
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

    fun normalizeUnit(unit: String): String {
        return unit.ifBlank { "piece" }
    }
}
