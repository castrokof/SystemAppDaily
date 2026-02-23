package com.systemapp.daily.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.systemapp.daily.R
import com.systemapp.daily.databinding.ActivityMainBinding
import com.systemapp.daily.ui.login.LoginActivity
import com.systemapp.daily.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        setupBottomNavigation()

        // Cargar fragment inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LecturasFragment())
                .commit()
            binding.bottomNav.selectedItemId = R.id.nav_lecturas
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "SystemApp Lecturas"
        supportActionBar?.subtitle = "Bienvenido, ${sessionManager.userName}"

        binding.btnLogout.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_lecturas -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LecturasFragment())
                        .commit()
                    supportActionBar?.title = "Lecturas"
                    true
                }
                R.id.nav_revisiones -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RevisionesFragment())
                        .commit()
                    supportActionBar?.title = "Revisiones"
                    true
                }
                else -> false
            }
        }
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
