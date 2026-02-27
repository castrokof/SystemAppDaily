package com.systemapp.daily.ui.revision_v2

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.EstadoOrden
import com.systemapp.daily.data.model.RevisionEntity
import java.util.Collections

class RevOrdenAdapter(
    private val showSyncStatus: Boolean = false,
    private val onItemClick: (RevisionEntity) -> Unit
) : RecyclerView.Adapter<RevOrdenAdapter.ViewHolder>() {

    data class RevItem(
        val revision: RevisionEntity,
        val syncStatus: String? = null
    )

    private val items = mutableListOf<RevItem>()

    // Callback para iniciar drag desde el handle
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun submitList(newList: List<RevItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getItems(): List<RevItem> = items.toList()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_revision_orden, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])

        // Iniciar drag al tocar el handle
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag?.invoke(holder)
            }
            false
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCodigoPredio: TextView = itemView.findViewById(R.id.tvCodigoPredio)
        private val tvSyncIcon: TextView = itemView.findViewById(R.id.tvSyncIcon)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvMotivo: TextView = itemView.findViewById(R.id.tvMotivo)
        private val tvNombreAtiende: TextView = itemView.findViewById(R.id.tvNombreAtiende)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val dragHandle: ImageView = itemView.findViewById(R.id.ivDragHandle)

        fun bind(item: RevItem) {
            val rev = item.revision
            tvCodigoPredio.text = "Predio: ${rev.codigoPredio}"
            tvMotivo.text = rev.motivoRevision ?: "Sin motivo asignado"
            tvNombreAtiende.text = rev.nombreAtiende ?: ""
            tvFecha.text = rev.fechaCierre ?: ""

            val isEjecutado = rev.estadoOrden == EstadoOrden.EJECUTADO
            tvEstado.text = if (isEjecutado) "EJECUTADA" else "PENDIENTE"
            val bgDrawable = tvEstado.background as? GradientDrawable
            bgDrawable?.setColor(itemView.context.getColor(if (isEjecutado) R.color.success else R.color.warning))

            if (showSyncStatus && isEjecutado) {
                tvSyncIcon.visibility = View.VISIBLE
                tvSyncIcon.text = when {
                    rev.sincronizado -> "\uD83D\uDFE2"
                    item.syncStatus == "ERROR" -> "\uD83D\uDD34"
                    else -> "\uD83D\uDFE1"
                }
            } else {
                tvSyncIcon.visibility = View.GONE
            }

            itemView.findViewById<View>(R.id.cardOrden).setOnClickListener { onItemClick(rev) }
        }
    }
}
