package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentMacroListBinding
import com.systemapp.daily.utils.Constants

class RevPendientesFragment : Fragment() {

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

        adapter = RevOrdenAdapter(showSyncStatus = false) { rev ->
            val intent = Intent(requireContext(), RevisionWizardActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDEN_ID, rev.idOrden)
            startActivity(intent)
        }

        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.adapter = adapter
        binding.tvEmpty.text = "No hay revisiones pendientes"

        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }

        viewModel.pendientes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.map { RevOrdenAdapter.RevItem(it) })
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvOrdenes.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
