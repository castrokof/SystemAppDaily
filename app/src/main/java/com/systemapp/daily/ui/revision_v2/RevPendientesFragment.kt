package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentRevisionListBinding
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.utils.Constants

class RevPendientesFragment : Fragment() {

    private var _binding: FragmentRevisionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var adapter: RevOrdenAdapter
    private var currentSort = SortOption.ID_ASC
    private var fullList: List<RevisionEntity> = emptyList()

    enum class SortOption(val label: String) {
        ID_ASC("Orden (ID ascendente)"),
        ID_DESC("Orden (ID descendente)"),
        PREDIO_ASC("Predio (A-Z)"),
        PREDIO_DESC("Predio (Z-A)")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRevisionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RevOrdenAdapter(showSyncStatus = false) { rev ->
            val intent = Intent(requireContext(), RevisionWizardActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDEN_ID, rev.idOrden)
            startActivity(intent)
        }

        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones pendientes"

        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }

        // Busqueda
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.buscarPendientes(s?.toString()?.trim() ?: "")
            }
        })

        // Ordenar
        binding.btnOrdenar.setOnClickListener {
            val options = SortOption.values().map { it.label }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Ordenar por")
                .setItems(options) { _, which ->
                    currentSort = SortOption.values()[which]
                    updateList(fullList)
                }
                .show()
        }

        viewModel.pendientesFiltrados.observe(viewLifecycleOwner) { list ->
            fullList = list
            updateList(list)
        }
    }

    private fun updateList(list: List<RevisionEntity>) {
        val sorted = when (currentSort) {
            SortOption.ID_ASC -> list.sortedBy { it.idOrden }
            SortOption.ID_DESC -> list.sortedByDescending { it.idOrden }
            SortOption.PREDIO_ASC -> list.sortedBy { it.codigoPredio.lowercase() }
            SortOption.PREDIO_DESC -> list.sortedByDescending { it.codigoPredio.lowercase() }
        }
        adapter.submitList(sorted.map { RevOrdenAdapter.RevItem(it) })
        binding.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrdenes.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
