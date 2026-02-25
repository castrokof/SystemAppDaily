package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.data.model.EstadoPunto
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

        censoAdapter = CensoAdapter { index -> viewModel.eliminarPuntoHidraulico(index) }
        binding.rvCenso.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCenso.adapter = censoAdapter

        viewModel.censoItems.observe(viewLifecycleOwner) { items ->
            censoAdapter.submitList(items.toList())
        }

        binding.btnAgregarPunto.setOnClickListener {
            mostrarDialogoAgregarPunto()
        }
    }

    private fun mostrarDialogoAgregarPunto() {
        val tipos = listOf(TipoPunto.GRIFO, TipoPunto.SANITARIO, TipoPunto.DUCHA, TipoPunto.LAVAMANOS,
            TipoPunto.LAVAPLATOS, TipoPunto.LAVADERO, TipoPunto.TANQUE, TipoPunto.CALENTADOR, TipoPunto.OTRO)
        val estados = listOf(EstadoPunto.BUENO, EstadoPunto.MALO)

        val dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null)
        // Build custom dialog
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

    // Simple inline adapter for censo items
    inner class CensoAdapter(
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<CensoAdapter.VH>() {
        private var items: List<com.systemapp.daily.data.model.HidraulicoEntity> = emptyList()

        fun submitList(list: List<com.systemapp.daily.data.model.HidraulicoEntity>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                setPadding(32, 24, 32, 24)
                textSize = 15f
            }
            return VH(tv)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            (holder.itemView as TextView).text = "${item.tipoPunto} x${item.cantidad} - ${item.estado}"
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar")
                    .setMessage("¿Eliminar ${item.tipoPunto}?")
                    .setPositiveButton("Sí") { _, _ -> onDelete(position) }
                    .setNegativeButton("No", null)
                    .show()
                true
            }
        }

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
