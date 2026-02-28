package com.systemapp.daily.ui.revision_v2

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.model.EstadoPunto
import com.systemapp.daily.data.model.HidraulicoEntity
import com.systemapp.daily.data.model.TipoPunto
import com.systemapp.daily.databinding.FragmentStep4MedidorBinding

class Step4MedidorFragment : Fragment() {

    private var _binding: FragmentStep4MedidorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var censoAdapter: CensoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep4MedidorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lectura del medidor
        if (viewModel.lecturaActual.isNotBlank()) {
            binding.etLecturaActual.setText(viewModel.lecturaActual)
        }

        censoAdapter = CensoAdapter { index ->
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar")
                .setMessage("¿Eliminar este punto hidráulico?")
                .setPositiveButton("Sí") { _, _ -> viewModel.eliminarPuntoHidraulico(index) }
                .setNegativeButton("No", null)
                .show()
        }
        binding.rvCenso.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCenso.adapter = censoAdapter

        viewModel.censoItems.observe(viewLifecycleOwner) { items ->
            censoAdapter.submitList(items.toList())
            binding.tvCensoCount.text = "Puntos registrados: ${items.size}"
            binding.tvCensoCount.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnAgregarPunto.setOnClickListener {
            mostrarDialogoAgregarPunto()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.lecturaActual = binding.etLecturaActual.text?.toString()?.trim() ?: ""
    }

    private fun mostrarDialogoAgregarPunto() {
        val tipos = listOf(TipoPunto.GRIFO, TipoPunto.SANITARIO, TipoPunto.DUCHA, TipoPunto.LAVAMANOS,
            TipoPunto.LAVAPLATOS, TipoPunto.LAVADERO, TipoPunto.TANQUE, TipoPunto.CALENTADOR, TipoPunto.OTRO)
        val estados = listOf(EstadoPunto.BUENO, EstadoPunto.MALO)

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val spTipo = Spinner(requireContext())
        spTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)
        layout.addView(TextView(requireContext()).apply { text = "Tipo de punto:" })
        layout.addView(spTipo)

        val etCantidad = EditText(requireContext()).apply {
            hint = "Cantidad"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }
        layout.addView(TextView(requireContext()).apply { text = "Cantidad:"; setPadding(0, 24, 0, 0) })
        layout.addView(etCantidad)

        val spEstado = Spinner(requireContext())
        spEstado.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, estados)
        layout.addView(TextView(requireContext()).apply { text = "Estado:"; setPadding(0, 24, 0, 0) })
        layout.addView(spEstado)

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Punto Hidráulico")
            .setView(layout)
            .setPositiveButton("Agregar") { _, _ ->
                val tipo = spTipo.selectedItem.toString()
                val cantidad = etCantidad.text.toString().toIntOrNull() ?: 1
                val estado = spEstado.selectedItem.toString()
                viewModel.agregarPuntoHidraulico(tipo, cantidad, estado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // Adapter con layout propio y boton eliminar visible
    inner class CensoAdapter(
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<CensoAdapter.VH>() {
        private var items: List<HidraulicoEntity> = emptyList()

        fun submitList(list: List<HidraulicoEntity>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_censo_hidraulico, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvTipoPunto.text = item.tipoPunto
            holder.tvCantidad.text = "x${item.cantidad}"
            holder.tvEstado.text = item.estado

            val colorRes = if (item.estado == EstadoPunto.BUENO) R.color.success else R.color.error
            (holder.tvEstado.background as? GradientDrawable)?.setColor(holder.itemView.context.getColor(colorRes))

            holder.btnEliminar.setOnClickListener { onDelete(position) }
        }

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTipoPunto: TextView = itemView.findViewById(R.id.tvTipoPunto)
            val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
            val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
            val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
        }
    }
}
