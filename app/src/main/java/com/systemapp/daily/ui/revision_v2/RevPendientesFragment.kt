package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentRevisionListBinding
import com.systemapp.daily.utils.Constants

class RevPendientesFragment : Fragment() {

    private var _binding: FragmentRevisionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionesListViewModel by activityViewModels()
    private lateinit var adapter: RevOrdenAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var currentSort = SortOption.MANUAL
    private var fullList: List<RevOrdenAdapter.RevItem> = emptyList()

    enum class SortOption(val label: String) {
        MANUAL("Mi orden (arrastrar)"),
        NUMERO_ASC("N° revisión ascendente"),
        NOMBRE_ASC("Suscriptor (A-Z)"),
        DIRECCION_ASC("Dirección (A-Z)"),
        CONSUMO("Alto consumo primero")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRevisionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RevOrdenAdapter(showSyncStatus = false) { rev ->
            startActivity(Intent(requireContext(), RevisionWizardActivity::class.java)
                .putExtra(Constants.EXTRA_ORDEN_ID, rev.idOrden))
        }
        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.setHasFixedSize(true)
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones pendientes.\nSincronice desde el menú para descargar órdenes."

        setupDragAndDrop()
        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }
        binding.etBuscar.doAfterTextChanged { viewModel.buscarPendientes(it?.toString()?.trim().orEmpty()) }

        binding.btnOrdenar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Ordenar por")
                .setItems(SortOption.values().map { it.label }.toTypedArray()) { _, which ->
                    currentSort = SortOption.values()[which]
                    updateList(fullList)
                }
                .show()
        }

        viewModel.pendientes.observe(viewLifecycleOwner) { list ->
            fullList = list
            updateList(list)
        }
    }

    private fun setupDragAndDrop() {
        val dragHelper = RevDragHelper(adapter) {
            viewModel.guardarOrdenManual(adapter.getItems().map { it.revision })
        }
        itemTouchHelper = ItemTouchHelper(dragHelper).also { it.attachToRecyclerView(binding.rvOrdenes) }
        adapter.onStartDrag = { holder ->
            currentSort = SortOption.MANUAL
            itemTouchHelper?.startDrag(holder)
        }
    }

    private fun updateList(list: List<RevOrdenAdapter.RevItem>) {
        val sorted = when (currentSort) {
            SortOption.MANUAL -> list // la BD ya viene por orden_manual
            SortOption.NUMERO_ASC -> list.sortedBy { it.revision.numeroVisible.toLongOrNull() ?: Long.MAX_VALUE }
            SortOption.NOMBRE_ASC -> list.sortedBy { it.revision.nombreSuscriptor?.lowercase() ?: "~" }
            SortOption.DIRECCION_ASC -> list.sortedBy { it.revision.direccion?.lowercase() ?: "~" }
            SortOption.CONSUMO -> list.sortedBy { if (it.revision.esAltoConsumo) 0 else 1 }
        }
        adapter.submitList(sorted)
        binding.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrdenes.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
