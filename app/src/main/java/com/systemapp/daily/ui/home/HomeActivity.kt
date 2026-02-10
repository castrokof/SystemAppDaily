package com.systemapp.daily.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.data.model.Macro
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
        cargarMacros()
    }

    override fun onResume() {
        super.onResume()
        // Recargar al volver de la pantalla de lectura
        cargarMacros()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Macromedidores"
        supportActionBar?.subtitle = "Bienvenido, ${sessionManager.userName}"

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                android.R.id.home -> {
                    confirmarLogout()
                    true
                }
                else -> false
            }
        }

        binding.btnLogout.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun setupRecyclerView() {
        macroAdapter = MacroAdapter { macro ->
            onMacroSelected(macro)
        }
        binding.rvMacros.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = macroAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            cargarMacros()
        }
    }

    private fun cargarMacros() {
        val token = sessionManager.token
        if (token != null) {
            viewModel.cargarMacros(token)
        } else {
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.macros.observe(this) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val macros = result.data
                    if (macros.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvMacros.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvMacros.visibility = View.VISIBLE
                        macroAdapter.submitList(macros)
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

    private fun onMacroSelected(macro: Macro) {
        val puedeLeer = macro.lecturasHoy < Constants.MAX_LECTURAS_POR_DIA || macro.lecturaAutorizada
        if (!puedeLeer) {
            Toast.makeText(
                this,
                "Ya alcanzaste el límite de ${Constants.MAX_LECTURAS_POR_DIA} lecturas hoy para este macro. Solicita autorización desde la web.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, LecturaActivity::class.java).apply {
            putExtra(Constants.EXTRA_MACRO_ID, macro.id)
            putExtra(Constants.EXTRA_MACRO_NOMBRE, macro.nombre)
            putExtra(Constants.EXTRA_MACRO_CODIGO, macro.codigo)
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
