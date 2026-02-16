package com.systemapp.daily.ui.revision

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck

class ChecklistAdapter(
    private val items: List<ChecklistItem>
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun getItems(): List<ChecklistItem> = items

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCheckCategoria)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvCheckDescripcion)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroupEstado)
        private val rbBueno: RadioButton = itemView.findViewById(R.id.rbBueno)
        private val rbMalo: RadioButton = itemView.findViewById(R.id.rbMalo)
        private val rbNoAplica: RadioButton = itemView.findViewById(R.id.rbNoAplica)

        fun bind(item: ChecklistItem) {
            tvCategoria.text = item.categoria
            tvDescripcion.text = item.descripcion

            // Limpiar listener antes de setear estado
            radioGroup.setOnCheckedChangeListener(null)

            when (item.estado) {
                EstadoCheck.BUENO -> rbBueno.isChecked = true
                EstadoCheck.MALO -> rbMalo.isChecked = true
                EstadoCheck.NO_APLICA -> rbNoAplica.isChecked = true
                EstadoCheck.NO_REVISADO -> radioGroup.clearCheck()
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                item.estado = when (checkedId) {
                    R.id.rbBueno -> EstadoCheck.BUENO
                    R.id.rbMalo -> EstadoCheck.MALO
                    R.id.rbNoAplica -> EstadoCheck.NO_APLICA
                    else -> EstadoCheck.NO_REVISADO
                }
            }
        }
    }
}
