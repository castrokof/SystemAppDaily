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
import com.systemapp.daily.data.model.Medidor

class MacroAdapter(
    private val onMedidorClick: (Medidor) -> Unit
) : ListAdapter<Medidor, MacroAdapter.MedidorViewHolder>(MedidorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedidorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro, parent, false)
        return MedidorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedidorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedidorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardMacro: MaterialCardView = itemView.findViewById(R.id.cardMacro)
        private val tvCodigo: TextView = itemView.findViewById(R.id.tvMacroCodigo)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvMacroNombre)
        private val tvDireccion: TextView = itemView.findViewById(R.id.tvMacroDireccion)
        private val tvLecturasHoy: TextView = itemView.findViewById(R.id.tvLecturasHoy)
        private val tvUltimaLectura: TextView = itemView.findViewById(R.id.tvUltimaLectura)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)

        fun bind(medidor: Medidor) {
            tvCodigo.text = medidor.refMedidor
            tvNombre.text = medidor.nombreCompleto
            tvDireccion.text = medidor.direccion ?: "Sin dirección"

            // Mostrar lectura anterior y promedio
            tvUltimaLectura.text = "L.Ant: ${medidor.lecturaAnterior ?: 0} | Prom: ${medidor.promedio ?: 0}"

            // Mostrar info de ruta y suscriptor
            tvLecturasHoy.text = "Suscriptor: ${medidor.suscriptor ?: "-"}"

            // Estado del medidor
            tvEstado.text = "Disponible"
            tvEstado.setTextColor(itemView.context.getColor(R.color.success))

            cardMacro.setOnClickListener {
                onMedidorClick(medidor)
            }
        }
    }

    class MedidorDiffCallback : DiffUtil.ItemCallback<Medidor>() {
        override fun areItemsTheSame(oldItem: Medidor, newItem: Medidor): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Medidor, newItem: Medidor): Boolean =
            oldItem == newItem
    }
}
