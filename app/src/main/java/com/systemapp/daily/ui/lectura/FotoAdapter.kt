package com.systemapp.daily.ui.lectura

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.systemapp.daily.R
import java.io.File

class FotoAdapter(
    private val fotos: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        holder.bind(fotos[position], position)
    }

    override fun getItemCount(): Int = fotos.size

    fun updateFotos(newFotos: List<String>) {
        fotos.clear()
        fotos.addAll(newFotos)
        notifyDataSetChanged()
    }

    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFoto: ImageView = itemView.findViewById(R.id.ivFoto)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteFoto)
        private val tvNumero: TextView = itemView.findViewById(R.id.tvFotoNumero)

        fun bind(fotoPath: String, position: Int) {
            tvNumero.text = "Foto ${position + 1}"

            Glide.with(itemView.context)
                .load(Uri.fromFile(File(fotoPath)))
                .centerCrop()
                .into(ivFoto)

            btnDelete.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
}
