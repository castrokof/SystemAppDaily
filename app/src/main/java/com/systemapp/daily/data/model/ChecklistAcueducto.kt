package com.systemapp.daily.data.model

/**
 * Checklist predefinido para revisión de servicios de acueducto.
 * Estos son los ítems que el técnico debe verificar en campo.
 */
object ChecklistAcueducto {

    fun getChecklist(): List<ChecklistItem> = listOf(
        // === ACOMETIDA ===
        ChecklistItem("acom_estado", "Acometida", "Estado general de la acometida"),
        ChecklistItem("acom_fuga", "Acometida", "Presencia de fugas en la acometida"),
        ChecklistItem("acom_material", "Acometida", "Estado del material de la tubería"),
        ChecklistItem("acom_valvula", "Acometida", "Válvula de corte funcionando"),

        // === MEDIDOR ===
        ChecklistItem("med_estado", "Medidor", "Estado físico del medidor"),
        ChecklistItem("med_lectura", "Medidor", "Lectura visible y legible"),
        ChecklistItem("med_sello", "Medidor", "Sello de seguridad presente"),
        ChecklistItem("med_caja", "Medidor", "Estado de la caja del medidor"),
        ChecklistItem("med_tapa", "Medidor", "Tapa de la caja en buen estado"),

        // === RED INTERNA ===
        ChecklistItem("red_fuga", "Red Interna", "Fugas visibles en la red interna"),
        ChecklistItem("red_presion", "Red Interna", "Presión del agua adecuada"),
        ChecklistItem("red_conexion", "Red Interna", "Conexiones en buen estado"),

        // === SERVICIO ===
        ChecklistItem("serv_continuidad", "Servicio", "Continuidad del servicio de agua"),
        ChecklistItem("serv_calidad", "Servicio", "Calidad del agua (color, olor)"),
        ChecklistItem("serv_fraude", "Servicio", "Posible conexión fraudulenta"),

        // === ENTORNO ===
        ChecklistItem("ent_acceso", "Entorno", "Acceso al punto de revisión"),
        ChecklistItem("ent_riesgo", "Entorno", "Riesgos o peligros en la zona")
    )
}
