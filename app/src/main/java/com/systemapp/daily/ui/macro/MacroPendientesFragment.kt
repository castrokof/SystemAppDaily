package com.systemapp.daily.ui.macro

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

class MacroPendientesFragment : Fragment() {

    private var _binding: FragmentMacroListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MacroViewModel by activityViewModels()
    private lateinit var adapter: MacroOrdenAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMacroListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MacroOrdenAdapter(showSyncStatus = false) { macro ->
            val intent = Intent(requireContext(), MacroLecturaActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDEN_ID, macro.idOrden)
            startActivity(intent)
        }

        binding.rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenes.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.pendientes.observe(viewLifecycleOwner) { list ->
            val items = list.map { MacroOrdenAdapter.MacroItem(it) }
            adapter.submitList(items)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvOrdenes.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
