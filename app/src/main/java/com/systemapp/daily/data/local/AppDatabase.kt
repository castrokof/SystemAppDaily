package com.systemapp.daily.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.systemapp.daily.data.model.*

@Database(
    entities = [
        Lectura::class,
        Revision::class,
        UserEntity::class,
        MacroEntity::class,
        RevisionEntity::class,
        HidraulicoEntity::class,
        ListaEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Legacy DAOs
    abstract fun lecturaDao(): LecturaDao
    abstract fun revisionDao(): RevisionDao

    // Nuevos DAOs
    abstract fun userDao(): UserDao
    abstract fun macroDao(): MacroDao
    abstract fun ordenRevisionDao(): OrdenRevisionDao
    abstract fun hidraulicoDao(): HidraulicoDao
    abstract fun listaDao(): ListaDao
    abstract fun syncQueueDao(): SyncQueueDao

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
