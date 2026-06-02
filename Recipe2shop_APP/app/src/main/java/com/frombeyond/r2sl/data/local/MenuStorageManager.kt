package com.frombeyond.r2sl.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * * Stores menu assignments in a local JSON file.
 */
class MenuStorageManager(private val context: Context) {

    fun loadMenu(): MenuData {
        val file = getFile()
        if (!file.exists()) {
            return MenuData(null, emptyList())
        }
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val startDate = json.optString(KEY_START_DATE).takeIf { it.isNotEmpty() }
            val assignmentsArray = json.optJSONArray(KEY_ASSIGNMENTS) ?: JSONArray()
            val assignments = (0 until assignmentsArray.length()).mapNotNull { index ->
                MenuAssignment.fromJson(assignmentsArray.optJSONObject(index))
            }
            val updatedAt = json.optLong(KEY_UPDATED_AT, 0L)
            MenuData(startDate, assignments, updatedAt)
        } catch (_: Exception) {
            MenuData(null, emptyList(), 0L)
        }
    }

    fun saveMenu(menuData: MenuData) {
        val file = getFile()
        val updatedAt = System.currentTimeMillis()
        val json = JSONObject().apply {
            menuData.startDate?.let { put(KEY_START_DATE, it) }
            put(KEY_UPDATED_AT, updatedAt)
            put(KEY_ASSIGNMENTS, JSONArray().apply {
                menuData.assignments.forEach { put(it.toJson()) }
            })
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2), Charsets.UTF_8)
    }

    fun saveStartDate(date: LocalDate) {
        val current = loadMenu()
        saveMenu(current.copy(startDate = date.toString()))
    }

    fun assignDish(date: LocalDate, mealType: String, dishId: String) {
        val current = loadMenu()
        val updated = current.assignments.filterNot {
            it.date == date.toString() && it.mealType == mealType
        }.toMutableList()
        updated.add(MenuAssignment(date.toString(), mealType, dishId))
        saveMenu(current.copy(assignments = updated))
    }

    fun addDish(date: LocalDate, mealType: String, dishId: String, portions: Int = 1) {
        val current = loadMenu()
        val updated = current.assignments.toMutableList()
        updated.add(MenuAssignment(date.toString(), mealType, dishId, portions.coerceAtLeast(1)))
        saveMenu(current.copy(assignments = updated))
    }

    fun setPortions(date: LocalDate, mealType: String, dishId: String, portions: Int) {
        val current = loadMenu()
        val updated = current.assignments.map {
            if (it.date == date.toString() && it.mealType == mealType && it.dishId == dishId) {
                it.copy(portions = portions.coerceAtLeast(1))
            } else it
        }
        saveMenu(current.copy(assignments = updated))
    }

    fun removeAssignment(date: LocalDate, mealType: String, dishId: String) {
        val current = loadMenu()
        val updated = current.assignments.filterNot {
            it.date == date.toString() && it.mealType == mealType && it.dishId == dishId
        }
        saveMenu(current.copy(assignments = updated))
    }

    fun getAssignment(date: LocalDate, mealType: String): MenuAssignment? {
        return loadMenu().assignments.firstOrNull {
            it.date == date.toString() && it.mealType == mealType
        }
    }

    fun getAssignments(date: LocalDate, mealType: String): List<MenuAssignment> {
        return loadMenu().assignments.filter {
            it.date == date.toString() && it.mealType == mealType
        }
    }

    fun getAssignmentsBetween(start: LocalDate, end: LocalDate): List<MenuAssignment> {
        return loadMenu().assignments.filter { assignment ->
            val date = LocalDate.parse(assignment.date)
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    /**
     * * Copies all meal assignments from [sourceDate] to the next [dayCount] days.
     * Fails if any target day already has at least one assignment.
     */
    fun copyDayToFollowingDays(sourceDate: LocalDate, dayCount: Int = 7): MenuCopyResult {
        if (dayCount < 1) {
            return MenuCopyResult.InvalidDayCount
        }
        val sourceDateStr = sourceDate.toString()
        val sourceAssignments = loadMenu().assignments.filter { it.date == sourceDateStr }
        if (sourceAssignments.isEmpty()) {
            return MenuCopyResult.SourceDayEmpty
        }
        val allAssignments = loadMenu().assignments
        for (offset in 1..dayCount) {
            val targetDate = sourceDate.plusDays(offset.toLong()).toString()
            if (allAssignments.any { it.date == targetDate }) {
                return MenuCopyResult.Overlap(LocalDate.parse(targetDate))
            }
        }
        val updated = allAssignments.toMutableList()
        for (offset in 1..dayCount) {
            val targetDate = sourceDate.plusDays(offset.toLong()).toString()
            sourceAssignments.forEach { assignment ->
                updated.add(
                    assignment.copy(
                        date = targetDate
                    )
                )
            }
        }
        saveMenu(loadMenu().copy(assignments = updated))
        return MenuCopyResult.Success(dayCount)
    }

    sealed class MenuCopyResult {
        data object SourceDayEmpty : MenuCopyResult()
        data object InvalidDayCount : MenuCopyResult()
        data class Overlap(val date: LocalDate) : MenuCopyResult()
        data class Success(val daysCopied: Int) : MenuCopyResult()
    }

    private fun getFile(): File {
        return File(context.filesDir, FILE_NAME)
    }

    data class MenuData(
        val startDate: String?,
        val assignments: List<MenuAssignment>,
        val updatedAt: Long = 0L
    )

    data class MenuAssignment(
        val date: String,
        val mealType: String,
        val dishId: String,
        val portions: Int = 1
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("date", date)
                put("mealType", mealType)
                put("dishId", dishId)
                put("portions", portions)
            }
        }

        companion object {
            fun fromJson(json: JSONObject?): MenuAssignment? {
                if (json == null) {
                    return null
                }
                return MenuAssignment(
                    date = json.getString("date"),
                    mealType = json.getString("mealType"),
                    dishId = json.getString("dishId"),
                    portions = json.optInt("portions", 1).coerceAtLeast(1)
                )
            }
        }
    }

    companion object {
        private const val FILE_NAME = "menu_assignments.json"
        private const val KEY_START_DATE = "startDate"
        private const val KEY_ASSIGNMENTS = "assignments"
        private const val KEY_UPDATED_AT = "updatedAt"
        const val MEAL_LUNCH = "lunch"
        const val MEAL_DINNER = "dinner"
    }
}
