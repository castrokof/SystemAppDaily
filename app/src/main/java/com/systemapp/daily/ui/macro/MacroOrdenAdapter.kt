package com.systemapp.daily.ui.macro

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.EstadoOrden
import com.systemapp.daily.data.model.MacroEntity

class MacroOrdenAdapter(
    private val showSyncStatus: Boolean = false,
    private val onItemClick: (MacroEntity) -> Unit
) : ListAdapter<MacroOrdenAdapter.MacroItem, MacroOrdenAdapter.ViewHolder>(DiffCallback()) {

    data class MacroItem(
        val macro: MacroEntity,
        val syncStatus: String? = null // EstadoSync value or null
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro_orden, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        private val tvSyncIcon: TextView = itemView.findViewById(R.id.tvSyncIcon)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacion)
        private val tvLecturaAnterior: TextView = itemView.findViewById(R.id.tvLecturaAnterior)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)

        fun bind(item: MacroItem) {
            val macro = item.macro
            tvCodigo.text = macro.codigoMacro
            tvUbicacion.text = macro.ubicacion ?: "Sin ubicación"
            tvLecturaAnterior.text = "L.Ant: ${macro.lecturaAnterior ?: "-"}"

            // Estado badge
            val isEjecutado = macro.estado == EstadoOrden.EJECUTADO
            tvEstado.text = if (isEjecutado) "EJECUTADO" else "PENDIENTE"
            val bgDrawable = tvEstado.background as? GradientDrawable
            val badgeColor = if (isEjecutado) {
                itemView.context.getColor(R.color.success)
            } else {
                itemView.context.getColor(R.color.warning)
            }
            bgDrawable?.setColor(badgeColor)

            // Fecha
            tvFecha.text = macro.fechaLectura ?: ""

            // Sync status icon for ejecutados
            if (showSyncStatus && isEjecutado) {
                tvSyncIcon.visibility = View.VISIBLE
                when {
                    macro.sincronizado -> {
                        tvSyncIcon.text = "\uD83D\uDFE2" // green circle
                    }
                    item.syncStatus == "ERROR" -> {
                        tvSyncIcon.text = "\uD83D\uDD34" // red circle
                    }
                    else -> {
                        tvSyncIcon.text = "\uD83D\uDFE1" // yellow circle
                    }
                }
            } else {
                tvSyncIcon.visibility = View.GONE
            }

            itemView.findViewById<View>(R.id.cardOrden).setOnClickListener {
                onItemClick(macro)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MacroItem>() {
        override fun areItemsTheSame(oldItem: MacroItem, newItem: MacroItem) =
            oldItem.macro.idOrden == newItem.macro.idOrden
        override fun areContentsTheSame(oldItem: MacroItem, newItem: MacroItem) =
            oldItem == newItem
    }
}
