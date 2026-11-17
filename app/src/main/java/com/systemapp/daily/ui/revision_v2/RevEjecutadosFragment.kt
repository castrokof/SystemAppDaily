package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.data.sync.SyncWorker
import com.systemapp.daily.databinding.FragmentRevisionListBinding
import com.systemapp.daily.utils.Constants

class RevEjecutadosFragment : Fragment() {

    private var _binding: FragmentRevisionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionesListViewModel by activityViewModels()
    private lateinit var adapter: RevOrdenAdapter
    private var currentSort = SortOption.FECHA_DESC
    private var fullList: List<RevOrdenAdapter.RevItem> = emptyList()

    enum class SortOption(val label: String) {
        FECHA_DESC("Fecha (recientes primero)"),
        FECHA_ASC("Fecha (antiguos primero)"),
        SIN_ENVIAR("Sin enviar primero"),
        NOMBRE_ASC("Suscriptor (A-Z)")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRevisionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RevOrdenAdapter(showSyncStatus = true, permitirArrastre = false) { rev ->
            val opciones = if (rev.sincronizado) arrayOf("Ver / reimprimir acta", "Retomar (volver a ejecutar)")
            else arrayOf("Ver / reimprimir acta", "Retomar (corregir)", "Reintentar envío")
            AlertDialog.Builder(requireContext())
                .setTitle("Revisión No. ${rev.numeroVisible}")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> startActivity(Intent(requireContext(), RevisionWizardActivity::class.java)
                            .putExtra(Constants.EXTRA_ORDEN_ID, rev.idOrden))
                        1 -> confirmarRetomar(rev.idOrden)
                        2 -> {
                            SyncWorker.ejecutarSincronizacionInmediata(requireContext())
                            Toast.makeText(requireContext(), "Reintentando envío…", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.setHasFixedSize(true)
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones ejecutadas"

        binding.swipeRefresh.setOnRefreshListener {
            SyncWorker.ejecutarSincronizacionInmediata(requireContext())
            binding.swipeRefresh.isRefreshing = false
        }
        binding.etBuscar.doAfterTextChanged { viewModel.buscarEjecutados(it?.toString()?.trim().orEmpty()) }

        binding.btnOrdenar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Ordenar por")
                .setItems(SortOption.values().map { it.label }.toTypedArray()) { _, which ->
                    currentSort = SortOption.values()[which]
                    updateList(fullList)
                }
                .show()
        }

        viewModel.ejecutados.observe(viewLifecycleOwner) { list ->
            fullList = list
            updateList(list)
        }
    }

    private fun confirmarRetomar(idOrden: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Retomar revisión")
            .setMessage("La revisión vuelve a Pendientes con los datos ya digitados para corregirla y finalizarla de nuevo. ¿Continuar?")
            .setPositiveButton("Retomar") { _, _ ->
                viewModel.retomarRevision(idOrden)
                Toast.makeText(requireContext(), "Revisión devuelta a pendientes", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateList(list: List<RevOrdenAdapter.RevItem>) {
        val sorted = when (currentSort) {
            SortOption.FECHA_DESC -> list.sortedByDescending { it.revision.fechaCierre ?: "" }
            SortOption.FECHA_ASC -> list.sortedBy { it.revision.fechaCierre ?: "" }
            SortOption.SIN_ENVIAR -> list.sortedBy { if (it.revision.sincronizado) 1 else 0 }
            SortOption.NOMBRE_ASC -> list.sortedBy { it.revision.nombreSuscriptor?.lowercase() ?: "~" }
        }
        adapter.submitList(sorted)
        binding.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrdenes.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
