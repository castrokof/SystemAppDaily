package com.systemapp.daily.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.systemapp.daily.R
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.location.GpsLocationManager
import com.systemapp.daily.data.repository.SyncRepository
import com.systemapp.daily.data.sync.SyncWorker
import com.systemapp.daily.databinding.ActivityMainBinding
import com.systemapp.daily.ui.firma.FirmaActivity
import com.systemapp.daily.ui.home.HomeActivity
import com.systemapp.daily.ui.impresora.ImpresoraActivity
import com.systemapp.daily.ui.login.LoginActivity
import com.systemapp.daily.ui.macro.MacromedidoresActivity
import com.systemapp.daily.ui.revision_v2.RevisionesActivity
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var syncRepository: SyncRepository
    private lateinit var gpsManager: GpsLocationManager
    private lateinit var toggle: ActionBarDrawerToggle

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!fineGranted) {
            Toast.makeText(this, R.string.permiso_ubicacion_requerido, Toast.LENGTH_LONG).show()
        }
        checkGpsStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        syncRepository = SyncRepository(this, sessionManager.apiToken ?: "")
        gpsManager = GpsLocationManager.getInstance(this)

        setupToolbar()
        setupDrawer()
        setupDashboardCards()
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        checkGpsStatus()
        loadCounts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.subtitle = getString(R.string.bienvenido_formato, sessionManager.userName ?: "")
    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.nav_abrir, R.string.nav_cerrar
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvNavUserName).text = sessionManager.userName ?: ""
        headerView.findViewById<TextView>(R.id.tvNavUserEmail).text = sessionManager.userEmail ?: sessionManager.userUsuario ?: ""

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_macromedidores -> {
                    startActivity(Intent(this, MacromedidoresActivity::class.java))
                    true
                }
                R.id.nav_revisiones -> {
                    startActivity(Intent(this, RevisionesActivity::class.java))
                    true
                }
                R.id.nav_firma -> {
                    startActivity(Intent(this, FirmaActivity::class.java))
                    true
                }
                R.id.nav_impresora -> {
                    startActivity(Intent(this, ImpresoraActivity::class.java))
                    true
                }
                R.id.nav_sincronizar -> {
                    ejecutarSincronizacion()
                    true
                }
                R.id.nav_borrar_datos -> {
                    confirmarBorrarDatos()
                    true
                }
                R.id.nav_cerrar_sesion -> {
                    confirmarCerrarSesion()
                    true
                }
                else -> false
            }
        }
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea cerrar sesión? Esto también borrará su token de autenticación.")
            .setPositiveButton("Sí") { _, _ ->
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupDashboardCards() {
        binding.cardMacros.setOnClickListener {
            startActivity(Intent(this, MacromedidoresActivity::class.java))
        }

        binding.cardRevisiones.setOnClickListener {
            startActivity(Intent(this, RevisionesActivity::class.java))
        }

        binding.cardSync.setOnClickListener {
            ejecutarSincronizacion()
        }

        val lastSync = sessionManager.lastSyncDate
        if (lastSync != null) {
            binding.tvLastSync.text = getString(R.string.ultima_sync_formato, lastSync)
        }
    }

    private fun loadCounts() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val macrosPend = db.macroDao().contarPendientes()
                val macrosEjec = db.macroDao().contarEjecutados()
                val revPend = db.ordenRevisionDao().contarPendientes()
                val revEjec = db.ordenRevisionDao().contarEjecutados()
                val syncPend = db.syncQueueDao().contarPendientesSync()

                binding.tvMacrosPendientes.text = "$macrosPend pendientes"
                binding.tvMacrosEjecutados.text = "$macrosEjec ejecutados"
                binding.tvRevisionesPendientes.text = "$revPend pendientes"
                binding.tvRevisionesEjecutados.text = "$revEjec ejecutados"

                // Mostrar indicador de sync pendiente
                if (syncPend > 0) {
                    binding.tvLastSync.text = "$syncPend registros por sincronizar"
                    binding.tvLastSync.setTextColor(getColor(R.color.warning))
                } else {
                    val lastSync = sessionManager.lastSyncDate
                    if (lastSync != null) {
                        binding.tvLastSync.text = getString(R.string.ultima_sync_formato, lastSync)
                    }
                    binding.tvLastSync.setTextColor(getColor(R.color.text_secondary))
                }
            } catch (_: Exception) { }
        }
    }

    private fun ejecutarSincronizacion() {
        val apiToken = sessionManager.apiToken
        if (apiToken == null) {
            navigateToLogin()
            return
        }
        // ✅ Pasar token al crear el repository
        val syncRepository = SyncRepository(this, apiToken)

        binding.progressSync.visibility = View.VISIBLE
        binding.cardSync.isEnabled = false

        lifecycleScope.launch {
            val result = syncRepository.sincronizar()
            binding.progressSync.visibility = View.GONE
            binding.cardSync.isEnabled = true

            when (result) {
                is NetworkResult.Success -> {
                    val data = result.data
                    val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    sessionManager.lastSyncDate = now
                    binding.tvLastSync.text = getString(R.string.ultima_sync_formato, now)

                    val msg = buildString {
                        append("Sincronización completada\n")
                        if (data.macrosSubidos > 0) append("Macros subidos: ${data.macrosSubidos}\n")
                        if (data.revisionesSubidas > 0) append("Revisiones subidas: ${data.revisionesSubidas}\n")
                        if (data.macrosDescargados > 0) append("Macros descargados: ${data.macrosDescargados}\n")
                        if (data.revisionesDescargadas > 0) append("Revisiones descargadas: ${data.revisionesDescargadas}\n")
                        if (data.listasDescargadas > 0) append("Listas descargadas: ${data.listasDescargadas}\n")
                        if (data.errores.isNotEmpty()) {
                            append("\nAdvertencias:\n")
                            data.errores.forEach { append("- $it\n") }
                        }
                    }
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Sincronización")
                        .setMessage(msg)
                        .setPositiveButton("Aceptar", null)
                        .show()

                    loadCounts()

                    // También disparar SyncWorker para la cola pendiente
                    SyncWorker.ejecutarSincronizacionInmediata(this@MainActivity)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun confirmarBorrarDatos() {
        AlertDialog.Builder(this)
            .setTitle(R.string.borrar_datos_titulo)
            .setMessage(R.string.borrar_datos_mensaje)
            .setPositiveButton(R.string.si) { _, _ ->
                lifecycleScope.launch {
                    syncRepository.borrarDatosLocales()
                    loadCounts()
                    Toast.makeText(this@MainActivity, R.string.datos_borrados, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun requestLocationPermission() {
        if (!gpsManager.hasPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkGpsStatus() {
        if (gpsManager.hasPermission() && !gpsManager.isGpsEnabled()) {
            binding.cardGpsStatus.visibility = View.VISIBLE
        } else {
            binding.cardGpsStatus.visibility = View.GONE
        }
    }

    private fun navigateToLogin() {
        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
