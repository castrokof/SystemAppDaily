package com.systemapp.daily.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.data.model.Medidor
import com.systemapp.daily.databinding.ActivityHomeBinding
import com.systemapp.daily.ui.lectura.LecturaActivity
import com.systemapp.daily.ui.login.LoginActivity
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var macroAdapter: MacroAdapter
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        cargarMedidores()
    }

    override fun onResume() {
        super.onResume()
        cargarMedidores()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Medidores"
        supportActionBar?.subtitle = "Bienvenido, ${sessionManager.userName}"

        binding.btnLogout.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun setupRecyclerView() {
        macroAdapter = MacroAdapter { medidor ->
            onMedidorSelected(medidor)
        }
        binding.rvMacros.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = macroAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            cargarMedidores()
        }
    }

    private fun cargarMedidores() {

        val usuario = sessionManager.userUsuario
        val apiToken = sessionManager.apiToken

        if (usuario != null  && apiToken != null) {
            viewModel.cargarMedidores(usuario, apiToken)
        } else {
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.medidores.observe(this) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val medidores = result.data
                    if (medidores.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvMacros.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvMacros.visibility = View.VISIBLE
                        macroAdapter.submitList(medidores)
                    }
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (result.code == 401) {
                        Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun onMedidorSelected(medidor: Medidor) {
        val intent = Intent(this, LecturaActivity::class.java).apply {
            putExtra(Constants.EXTRA_MACRO_ID, medidor.id)
            putExtra(Constants.EXTRA_MACRO_NOMBRE, medidor.nombreCompleto)
            putExtra(Constants.EXTRA_MACRO_CODIGO, medidor.refMedidor)
        }
        startActivity(intent)
    }

    private fun confirmarLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar tu sesión?")
            .setPositiveButton("Sí") { _, _ ->
                sessionManager.logout()
                navigateToLogin()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
