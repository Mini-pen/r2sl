package com.frombeyond.r2sl.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.frombeyond.r2sl.data.database.dao.*
import com.frombeyond.r2sl.data.database.entities.*
import com.frombeyond.r2sl.data.database.converters.DateConverters
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        Therapist::class,
        Patient::class,
        TherapySession::class,
        UserPreferences::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class TherapiaDatabase : RoomDatabase() {
    
    abstract fun therapistDao(): TherapistDao
    abstract fun patientDao(): PatientDao
    abstract fun therapySessionDao(): TherapySessionDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    
    companion object {
        @Volatile
        private var INSTANCE: TherapiaDatabase? = null
        
        fun getDatabase(context: Context, passphrase: String): TherapiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TherapiaDatabase::class.java,
                    "therapia_database"
                )
                .openHelperFactory(SupportFactory(passphrase.toByteArray()))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
