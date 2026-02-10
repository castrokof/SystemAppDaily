package com.systemapp.daily.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.Macro
import com.systemapp.daily.utils.Constants

class MacroAdapter(
    private val onMacroClick: (Macro) -> Unit
) : ListAdapter<Macro, MacroAdapter.MacroViewHolder>(MacroDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro, parent, false)
        return MacroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MacroViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MacroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardMacro: MaterialCardView = itemView.findViewById(R.id.cardMacro)
        private val tvCodigo: TextView = itemView.findViewById(R.id.tvMacroCodigo)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvMacroNombre)
        private val tvDireccion: TextView = itemView.findViewById(R.id.tvMacroDireccion)
        private val tvLecturasHoy: TextView = itemView.findViewById(R.id.tvLecturasHoy)
        private val tvUltimaLectura: TextView = itemView.findViewById(R.id.tvUltimaLectura)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)

        fun bind(macro: Macro) {
            tvCodigo.text = macro.codigo
            tvNombre.text = macro.nombre
            tvDireccion.text = macro.direccion ?: "Sin dirección"
            tvUltimaLectura.text = if (macro.ultimaLectura != null)
                "Última: ${macro.ultimaLectura}" else "Sin lecturas previas"

            val lecturasHoy = macro.lecturasHoy
            val maxLecturas = if (macro.lecturaAutorizada) "Ilimitado" else "${Constants.MAX_LECTURAS_POR_DIA}"
            tvLecturasHoy.text = "Hoy: $lecturasHoy / $maxLecturas"

            // Color de estado según lecturas
            val puedeLeer = lecturasHoy < Constants.MAX_LECTURAS_POR_DIA || macro.lecturaAutorizada
            if (puedeLeer) {
                tvEstado.text = "Disponible"
                tvEstado.setTextColor(itemView.context.getColor(R.color.success))
            } else {
                tvEstado.text = "Límite alcanzado"
                tvEstado.setTextColor(itemView.context.getColor(R.color.error))
            }

            cardMacro.setOnClickListener {
                onMacroClick(macro)
            }
        }
    }

    class MacroDiffCallback : DiffUtil.ItemCallback<Macro>() {
        override fun areItemsTheSame(oldItem: Macro, newItem: Macro): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Macro, newItem: Macro): Boolean =
            oldItem == newItem
    }
}
