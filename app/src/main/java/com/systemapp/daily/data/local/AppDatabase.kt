package com.systemapp.daily.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.systemapp.daily.data.model.Lectura

/**
 * Base de datos local Room.
 * Almacena lecturas pendientes de sincronizar cuando no hay conexión.
 */
@Database(entities = [Lectura::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lecturaDao(): LecturaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "system_app_daily_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
