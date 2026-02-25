package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentMacroListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RevEjecutadosFragment : Fragment() {

    private var _binding: FragmentMacroListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var adapter: RevOrdenAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMacroListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RevOrdenAdapter(showSyncStatus = true) { _ -> }
        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones ejecutadas"

        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }

        viewModel.ejecutados.observe(viewLifecycleOwner) { list ->
            lifecycleScope.launch {
                val items = list.map { rev ->
                    val syncStatus = withContext(Dispatchers.IO) { viewModel.getSyncStatus(rev.idOrden) }
                    RevOrdenAdapter.RevItem(rev, syncStatus)
                }
                adapter.submitList(items)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.rvOrdenes.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
