package com.systemapp.daily.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.systemapp.daily.data.model.Lectura
import com.systemapp.daily.data.model.Revision

/**
 * Base de datos local Room.
 * Almacena lecturas y revisiones pendientes de sincronizar cuando no hay conexión.
 */
@Database(entities = [Lectura::class, Revision::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lecturaDao(): LecturaDao
    abstract fun revisionDao(): RevisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "system_app_daily_db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
