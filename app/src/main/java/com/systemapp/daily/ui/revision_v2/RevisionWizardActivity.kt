package com.systemapp.daily.ui.revision_v2

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.systemapp.daily.R
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.databinding.ActivityRevisionWizardBinding
import com.systemapp.daily.ui.revision_v2.RevisionWizardViewModel.Companion.PASO_CIERRE
import com.systemapp.daily.ui.revision_v2.RevisionWizardViewModel.Companion.TOTAL_PASOS
import com.systemapp.daily.ui.revision_v2.RevisionWizardViewModel.Estado
import com.systemapp.daily.utils.Constants

/**
 * Revisión de crítica de lectura (Formato C):
 * encabezado fijo + Predio → Visita → Causa → Adicional (opcional) → Cierre (firmas).
 */
class RevisionWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRevisionWizardBinding
    val viewModel: RevisionWizardViewModel by viewModels()

    private val pasos = listOf(
        "Predio" to R.drawable.ic_location,
        "Visita" to R.drawable.ic_meter,
        "Causa" to R.drawable.ic_checklist,
        "Adicional" to R.drawable.ic_camera,
        "Cierre" to R.drawable.ic_firma
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevisionWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idOrden = intent.getIntExtra(Constants.EXTRA_ORDEN_ID, -1)
        if (idOrden == -1) {
            Toast.makeText(this, "Orden no válida", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Revisión de crítica"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        configurarPasos()
        configurarBotones()

        binding.cardEncabezado.setOnClickListener { encabezadoContraidoPorUsuario = !encabezadoContraidoPorUsuario; aplicarEncabezado() }

        // Con el teclado abierto el encabezado se contrae para dejar espacio al campo
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val teclado = ViewCompat.getRootWindowInsets(binding.root)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (teclado != tecladoVisible) { tecladoVisible = teclado; binding.root.post { aplicarEncabezado() } }
        }

        viewModel.orden.observe(this) { it?.let(::mostrarEncabezado) }
        viewModel.estado.observe(this, ::mostrarEstado)
        viewModel.cargado.observe(this) { if (it) actualizarPaso(binding.viewPager.currentItem) }

        viewModel.cargarOrden(idOrden)
    }

    override fun onPause() {
        super.onPause()
        // Borrador: si Android cierra la app (p. ej. con la cámara abierta) no se pierde nada
        viewModel.guardarBorrador()
    }

    // ------------------------------------------------------------------

    private fun configurarPasos() {
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = TOTAL_PASOS - 1 // pasos vivos: cambio instantáneo y sin perder campos
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = TOTAL_PASOS
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> PasoPredioFragment()
                1 -> PasoVisitaFragment()
                2 -> PasoCausasFragment()
                3 -> PasoAdicionalFragment()
                else -> PasoCierreFragment()
            }
        }
        pasos.forEachIndexed { i, (nombre, icono) ->
            binding.tabLayoutSteps.addTab(binding.tabLayoutSteps.newTab().setText("${i + 1}. $nombre").setIcon(icono))
        }
        binding.tabLayoutSteps.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (binding.viewPager.currentItem != tab.position) binding.viewPager.setCurrentItem(tab.position, false)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayoutSteps.getTabAt(position)?.takeIf { !it.isSelected }?.select()
                ocultarTeclado()
                actualizarPaso(position)
            }
        })
    }

    private fun configurarBotones() {
        binding.btnAnterior.setOnClickListener {
            val actual = binding.viewPager.currentItem
            if (actual > 0) binding.viewPager.currentItem = actual - 1
        }
        binding.btnSiguiente.setOnClickListener {
            if (viewModel.estado.value is Estado.Guardado || viewModel.soloLectura && binding.viewPager.currentItem == PASO_CIERRE) {
                finish(); return@setOnClickListener
            }
            val actual = binding.viewPager.currentItem
            if (actual < PASO_CIERRE) {
                if (!viewModel.soloLectura) {
                    viewModel.validarPaso(actual)?.let { avisar(it); return@setOnClickListener }
                }
                binding.viewPager.currentItem = actual + 1
            } else {
                intentarFinalizar()
            }
        }
    }

    private fun intentarFinalizar() {
        viewModel.primerPasoInvalido()?.let { (paso, msg) ->
            binding.viewPager.currentItem = paso
            avisar(msg)
            return
        }
        val aviso = viewModel.advertenciaLectura()?.let { "\n\n⚠ $it" } ?: ""
        AlertDialog.Builder(this)
            .setTitle("Finalizar revisión")
            .setMessage("Se guardará la revisión con las firmas y se enviará al servidor junto con el acta.$aviso")
            .setPositiveButton("Finalizar") { _, _ -> viewModel.finalizar() }
            .setNegativeButton("Revisar", null)
            .show()
    }

    private fun mostrarEstado(estado: Estado) {
        val guardando = estado is Estado.Guardando
        binding.btnSiguiente.isEnabled = !guardando
        binding.btnAnterior.isEnabled = !guardando
        when (estado) {
            is Estado.Guardado -> {
                binding.tvBannerSoloLectura.visibility = View.VISIBLE
                binding.tvBannerSoloLectura.text = "Revisión finalizada — solo consulta e impresión"
            }
            is Estado.Error -> avisar(estado.mensaje, error = true)
            else -> binding.tvBannerSoloLectura.visibility = View.GONE
        }
        actualizarPaso(binding.viewPager.currentItem)
    }

    private fun actualizarPaso(paso: Int) {
        binding.progressSteps.max = TOTAL_PASOS
        binding.progressSteps.progress = paso + 1
        binding.btnAnterior.visibility = if (paso == 0) View.INVISIBLE else View.VISIBLE
        val finalizada = viewModel.estado.value is Estado.Guardado
        binding.btnSiguiente.text = when {
            paso < PASO_CIERRE -> "Siguiente"
            finalizada || viewModel.soloLectura -> "Cerrar"
            else -> "Finalizar"
        }
    }

    private fun mostrarEncabezado(r: RevisionEntity) {
        supportActionBar?.subtitle = r.nombreSuscriptor ?: "Predio ${r.codigoPredio}"
        binding.tvEncNumero.text = "No. ${r.numeroVisible}" +
            (listOfNotNull(r.periodoAnio, r.periodoMes?.padStart(2, '0')).joinToString("/").takeIf { it.isNotBlank() }?.let { "  ·  Periodo $it" } ?: "")
        binding.tvEncNombre.text = r.nombreSuscriptor ?: "Suscriptor sin nombre"
        binding.tvEncCodigo.text = "Código ${r.codigoPredio}" + (r.refMedidor?.let { "  ·  Medidor $it" } ?: "")
        binding.tvEncDireccion.text = r.direccion ?: ""
        binding.tvEncDireccion.visibility = if (r.direccion.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvEncLectActual.text = "Lect. act.\n${r.lecturaPeriodo ?: "-"}"
        binding.tvEncLectAnterior.text = "Lect. ant.\n${r.lecturaAnterior ?: "-"}"
        binding.tvEncConsumo.text = "Consumo\n${r.consumoActual ?: "-"}"
        binding.tvEncPromedio.text = "Promedio\n${r.promedio ?: "-"}"

        val color = when {
            r.esAltoConsumo -> R.color.error
            r.esBajoConsumo -> R.color.primary
            else -> null
        }
        if (color != null) {
            binding.tvEncTipoConsumo.visibility = View.VISIBLE
            binding.tvEncTipoConsumo.text = r.etiquetaConsumo
            (binding.tvEncTipoConsumo.background.mutate() as? GradientDrawable)?.setColor(getColor(color))
        } else binding.tvEncTipoConsumo.visibility = View.GONE
    }

    private var encabezadoContraidoPorUsuario = false
    private var tecladoVisible = false

    private fun aplicarEncabezado() {
        val contraido = encabezadoContraidoPorUsuario || tecladoVisible
        binding.layoutEncDetalle.visibility = if (contraido) View.GONE else View.VISIBLE
        binding.ivEncExpandir.setImageResource(if (contraido) android.R.drawable.arrow_down_float else android.R.drawable.arrow_up_float)
    }

    private fun avisar(msg: String, error: Boolean = false) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(if (error) R.color.error else R.color.warning))
            .setAnchorView(binding.btnSiguiente)
            .show()
    }

    private fun ocultarTeclado() {
        currentFocus?.let { v ->
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
        }
    }
}
