package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listas")
data class ListaEntity(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "tipo_lista")
    val tipoLista: String,

    val codigo: String,

    val descripcion: String
)

object TipoLista {
    const val MOTIVOS = "MOTIVOS"
    const val ESTADOS = "ESTADOS"
    const val ACOMETIDA = "ACOMETIDA"
    const val SELLOS = "SELLOS"
    const val TIPO_DOCUMENTO = "TIPO_DOCUMENTO"
    const val TIPO_PUNTO = "TIPO_PUNTO"
    const val SURTE = "SURTE"

    // Formato C
    const val TIPO_PREDIO = "TIPO_PREDIO"
    const val USO_PREDIO = "USO_PREDIO"
    const val CAUSA_CONSUMO = "CAUSA_CONSUMO"
    const val MOVIMIENTO_MEDIDOR = "MOVIMIENTO_MEDIDOR"
}

/** Opción simple (código + descripción) para selectores. */
data class Opcion(val codigo: String, val descripcion: String)

/**
 * Respaldo local de las listas del Formato C, por si el servidor todavía no las envía
 * o no hay red en la primera sincronización. Si `listasParametros` trae la lista, se usa esa.
 */
object ListasRespaldo {
    const val CAUSA_OTRO = "OTRO"

    private val porTipo: Map<String, List<Opcion>> = mapOf(
        TipoLista.TIPO_PREDIO to listOf(
            Opcion("H", "Habitado"), Opcion("D", "Desocupado"), Opcion("C", "Cortado")
        ),
        TipoLista.USO_PREDIO to listOf(
            Opcion("R", "Residencial"), Opcion("C", "Comercial"), Opcion("I", "Industrial"),
            Opcion("O", "Oficial"), Opcion("E", "Especial")
        ),
        TipoLista.MOVIMIENTO_MEDIDOR to listOf(
            Opcion("SIN", "Sin movimiento"), Opcion("CON", "Con movimiento")
        ),
        TipoLista.CAUSA_CONSUMO to listOf(
            Opcion("AUMENTO_HABITANTES", "Aumento de habitantes"),
            Opcion("EN_CONSTRUCCION", "En construcción"),
            Opcion("DANO", "Daño"),
            Opcion("AUSENCIA_HABITANTES", "Ausencia de habitantes"),
            Opcion("FUGA_PERCEPTIBLE_BANO", "Fuga perceptible en baño"),
            Opcion("FUGA_PERCEPTIBLE_LLAVE_TERMINAL", "Fuga perceptible en llave terminal"),
            Opcion("FUGA_IMPERCEPTIBLE", "Fuga imperceptible"),
            Opcion(CAUSA_OTRO, "Otro")
        ),
        TipoLista.ACOMETIDA to listOf(
            Opcion("BUENO", "Bueno"), Opcion("REGULAR", "Regular"), Opcion("MALO", "Malo"), Opcion("NO_TIENE", "No tiene")
        ),
        TipoLista.SELLOS to listOf(
            Opcion("INTACTOS", "Intactos"), Opcion("ROTOS", "Rotos"), Opcion("SIN_SELLOS", "Sin sellos"), Opcion("NO_APLICA", "No aplica")
        ),
        TipoLista.TIPO_DOCUMENTO to listOf(
            Opcion("CC", "CC"), Opcion("TI", "TI"), Opcion("CE", "CE"), Opcion("NIT", "NIT"), Opcion("PAS", "Pasaporte")
        )
    )

    fun get(tipo: String): List<Opcion> = porTipo[tipo].orEmpty()
}
