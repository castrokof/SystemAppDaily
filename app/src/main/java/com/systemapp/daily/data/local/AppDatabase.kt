package com.systemapp.daily.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 7,
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

        /**
         * 6 -> 7: campos del Formato C en ordenes_revision.
         * Solo agrega columnas: no borra revisiones pendientes de sincronizar.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val textos = listOf(
                    "numero_revision", "periodo_anio", "periodo_mes", "tipo_consumo",
                    "nombre_suscriptor", "direccion", "telefono", "ref_medidor",
                    "lectura_periodo", "lectura_anterior", "consumo_actual", "promedio",
                    "critica_original", "funcionario_nombre", "funcionario_cc",
                    "tipo_predio", "uso_predio", "tiempo_habitacion", "fecha_visita",
                    "medidor_movimiento", "causas_consumo", "causa_otro_cual", "firma_funcionario"
                )
                textos.forEach { col ->
                    db.execSQL("ALTER TABLE ordenes_revision ADD COLUMN `$col` TEXT")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "system_app_daily_db"
                )
                    .addMigrations(MIGRATION_6_7)
                    // Solo versiones muy viejas (sin datos de campo útiles) se recrean.
                    // Desde la 6 en adelante cada cambio de esquema necesita su Migration.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
