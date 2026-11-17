package com.systemapp.daily.ui.revision_v2

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.EstadoOrden
import com.systemapp.daily.data.model.EstadoSync
import com.systemapp.daily.data.model.RevisionEntity
import java.util.Collections

class RevOrdenAdapter(
    private val showSyncStatus: Boolean = false,
    private val permitirArrastre: Boolean = true,
    private val onItemClick: (RevisionEntity) -> Unit
) : RecyclerView.Adapter<RevOrdenAdapter.ViewHolder>() {

    data class RevItem(
        val revision: RevisionEntity,
        val syncStatus: String? = null
    )

    private val items = mutableListOf<RevItem>()

    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    init { setHasStableIds(true) }

    /** Actualiza solo lo que cambió (sin parpadeo ni perder el scroll). */
    fun submitList(newList: List<RevItem>) {
        val old = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = old[o].revision.idOrden == newList[n].revision.idOrden
            override fun areContentsTheSame(o: Int, n: Int) = old[o] == newList[n]
        })
        items.clear()
        items.addAll(newList)
        diff.dispatchUpdatesTo(this)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getItems(): List<RevItem> = items.toList()

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].revision.idOrden.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_revision_orden, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
        holder.dragHandle.visibility = if (permitirArrastre) View.VISIBLE else View.GONE
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag?.invoke(holder)
            false
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvCodigoPredio)
        private val tvSync: TextView = itemView.findViewById(R.id.tvSyncIcon)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvMotivo)
        private val tvDireccion: TextView = itemView.findViewById(R.id.tvDireccion)
        private val tvTipoConsumo: TextView = itemView.findViewById(R.id.tvTipoConsumo)
        private val tvDetalle: TextView = itemView.findViewById(R.id.tvNombreAtiende)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val vTipoConsumo: View = itemView.findViewById(R.id.vTipoConsumo)
        val dragHandle: ImageView = itemView.findViewById(R.id.ivDragHandle)

        fun bind(item: RevItem) {
            val rev = item.revision
            val ctx = itemView.context
            tvTitulo.text = "No. ${rev.numeroVisible}  ·  Cód. ${rev.codigoPredio}"
            tvNombre.text = rev.nombreSuscriptor ?: rev.nombreAtiende ?: "Suscriptor sin nombre"
            tvDireccion.text = rev.direccion ?: ""
            tvDireccion.visibility = if (rev.direccion.isNullOrBlank()) View.GONE else View.VISIBLE
            tvDetalle.text = rev.refMedidor?.let { "Medidor $it" } ?: ""
            tvFecha.text = rev.fechaCierre?.take(16) ?: ""

            val colorConsumo = when {
                rev.esAltoConsumo -> R.color.error
                rev.esBajoConsumo -> R.color.primary
                else -> null
            }
            if (colorConsumo != null) {
                tvTipoConsumo.visibility = View.VISIBLE
                tvTipoConsumo.text = rev.etiquetaConsumo
                (tvTipoConsumo.background.mutate() as? GradientDrawable)?.setColor(ctx.getColor(colorConsumo))
                vTipoConsumo.setBackgroundColor(ctx.getColor(colorConsumo))
            } else {
                tvTipoConsumo.visibility = View.GONE
                vTipoConsumo.setBackgroundColor(ctx.getColor(R.color.divider))
            }

            val isEjecutado = rev.estadoOrden == EstadoOrden.EJECUTADO
            tvEstado.text = if (isEjecutado) "EJECUTADA" else "PENDIENTE"
            (tvEstado.background.mutate() as? GradientDrawable)
                ?.setColor(ctx.getColor(if (isEjecutado) R.color.success else R.color.warning))

            if (showSyncStatus && isEjecutado) {
                tvSync.visibility = View.VISIBLE
                val (texto, color) = when {
                    rev.sincronizado -> "✓ ENVIADA" to R.color.success
                    item.syncStatus == EstadoSync.ERROR -> "✗ ERROR ENVÍO" to R.color.error
                    else -> "⟳ POR ENVIAR" to R.color.warning
                }
                tvSync.text = texto
                tvSync.setTextColor(ctx.getColor(color))
            } else {
                tvSync.visibility = View.GONE
            }

            itemView.findViewById<View>(R.id.cardOrden).setOnClickListener { onItemClick(rev) }
        }
    }
}
