package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentRevisionListBinding
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RevEjecutadosFragment : Fragment() {

    private var _binding: FragmentRevisionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var adapter: RevOrdenAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var currentSort = SortOption.FECHA_DESC
    private var fullList: List<RevisionEntity> = emptyList()

    enum class SortOption(val label: String) {
        FECHA_DESC("Fecha (recientes primero)"),
        FECHA_ASC("Fecha (antiguos primero)"),
        PREDIO_ASC("Predio (A-Z)"),
        PREDIO_DESC("Predio (Z-A)"),
        MANUAL("Manual (arrastrar)")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRevisionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RevOrdenAdapter(showSyncStatus = true) { rev ->
            AlertDialog.Builder(requireContext())
                .setTitle("Revisión ${rev.codigoPredio}")
                .setMessage("¿Qué desea hacer con esta revisión?")
                .setPositiveButton("Retomar") { _, _ ->
                    viewModel.retomarRevision(rev.idOrden)
                    Toast.makeText(requireContext(), "Revisión devuelta a pendientes", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Abrir") { _, _ ->
                    val intent = Intent(requireContext(), RevisionWizardActivity::class.java)
                    intent.putExtra(Constants.EXTRA_ORDEN_ID, rev.idOrden)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones ejecutadas"

        // Setup drag-and-drop
        setupDragAndDrop()

        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }

        // Busqueda
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.buscarEjecutados(s?.toString()?.trim() ?: "")
            }
        })

        // Ordenar
        binding.btnOrdenar.setOnClickListener {
            val options = SortOption.values().map { it.label }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Ordenar por")
                .setItems(options) { _, which ->
                    currentSort = SortOption.values()[which]
                    if (currentSort == SortOption.MANUAL) {
                        return@setItems
                    }
                    lifecycleScope.launch { updateList(fullList) }
                }
                .show()
        }

        viewModel.ejecutadosFiltrados.observe(viewLifecycleOwner) { list ->
            fullList = list
            if (currentSort != SortOption.MANUAL || list.size != adapter.getItems().size) {
                lifecycleScope.launch {
                    updateList(list)
                }
            }
        }
    }

    private fun setupDragAndDrop() {
        val dragHelper = RevDragHelper(adapter)
        itemTouchHelper = ItemTouchHelper(dragHelper)
        itemTouchHelper?.attachToRecyclerView(binding.rvOrdenes)

        adapter.onStartDrag = { holder ->
            itemTouchHelper?.startDrag(holder)
        }
    }

    private suspend fun updateList(list: List<RevisionEntity>) {
        val sorted = when (currentSort) {
            SortOption.FECHA_DESC -> list.sortedByDescending { it.fechaCierre ?: "" }
            SortOption.FECHA_ASC -> list.sortedBy { it.fechaCierre ?: "" }
            SortOption.PREDIO_ASC -> list.sortedBy { it.codigoPredio.lowercase() }
            SortOption.PREDIO_DESC -> list.sortedByDescending { it.codigoPredio.lowercase() }
            SortOption.MANUAL -> list
        }
        val items = sorted.map { rev ->
            val syncStatus = withContext(Dispatchers.IO) { viewModel.getSyncStatus(rev.idOrden) }
            RevOrdenAdapter.RevItem(rev, syncStatus)
        }
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrdenes.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
