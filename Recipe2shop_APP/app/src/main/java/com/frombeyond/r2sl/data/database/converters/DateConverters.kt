package com.frombeyond.r2sl.data.database.converters

import androidx.room.TypeConverter
import java.util.Date

class DateConverters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromString(value: String?): Date? {
        return value?.let { 
            try {
                java.time.LocalDate.parse(it).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli().let { timestamp ->
                    Date(timestamp)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun dateToString(date: Date?): String? {
        return date?.let {
            java.time.Instant.ofEpochMilli(it.time)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .toString()
        }
    }
}
