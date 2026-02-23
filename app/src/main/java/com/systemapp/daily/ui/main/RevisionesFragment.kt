package com.systemapp.daily.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.data.model.Medidor
import com.systemapp.daily.databinding.FragmentRevisionesBinding
import com.systemapp.daily.ui.home.HomeViewModel
import com.systemapp.daily.ui.home.MacroAdapter
import com.systemapp.daily.ui.login.LoginActivity
import com.systemapp.daily.ui.revision.RevisionActivity
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager

class RevisionesFragment : Fragment() {

    private var _binding: FragmentRevisionesBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var macroAdapter: MacroAdapter
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRevisionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        cargarMedidores()
    }

    override fun onResume() {
        super.onResume()
        cargarMedidores()
    }

    private fun setupRecyclerView() {
        macroAdapter = MacroAdapter { medidor ->
            onMedidorSelected(medidor)
        }
        binding.rvMedidoresRevision.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = macroAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshRevision.setOnRefreshListener {
            cargarMedidores()
        }
    }

    private fun cargarMedidores() {
        val usuario = sessionManager.userUsuario
        if (usuario != null) {
            viewModel.cargarMedidores(usuario)
        } else {
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.medidores.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshRevision.isRefreshing = false
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBarRevision.visibility = View.VISIBLE
                    binding.tvEmptyRevision.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    binding.progressBarRevision.visibility = View.GONE
                    val medidores = result.data
                    if (medidores.isEmpty()) {
                        binding.tvEmptyRevision.visibility = View.VISIBLE
                        binding.rvMedidoresRevision.visibility = View.GONE
                    } else {
                        binding.tvEmptyRevision.visibility = View.GONE
                        binding.rvMedidoresRevision.visibility = View.VISIBLE
                        macroAdapter.submitList(medidores)
                    }
                }
                is NetworkResult.Error -> {
                    binding.progressBarRevision.visibility = View.GONE
                    if (result.code == 401) {
                        Toast.makeText(requireContext(), "Sesión expirada", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun onMedidorSelected(medidor: Medidor) {
        val intent = Intent(requireContext(), RevisionActivity::class.java).apply {
            putExtra(Constants.EXTRA_MACRO_ID, medidor.id)
            putExtra(Constants.EXTRA_MACRO_NOMBRE, medidor.nombreCompleto)
            putExtra(Constants.EXTRA_MACRO_CODIGO, medidor.refMedidor)
            putExtra(RevisionActivity.EXTRA_SUSCRIPTOR, medidor.suscriptor)
            putExtra(RevisionActivity.EXTRA_DIRECCION, medidor.direccion)
        }
        startActivity(intent)
    }

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
