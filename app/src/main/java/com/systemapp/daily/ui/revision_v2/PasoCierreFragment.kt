package com.systemapp.daily.ui.revision_v2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.systemapp.daily.R
import com.systemapp.daily.data.acta.ActaFormatoC
import com.systemapp.daily.data.acta.ImpresoraTermica
import com.systemapp.daily.databinding.FragmentPasoCierreBinding
import com.systemapp.daily.ui.firma.FirmaActivity
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PasoCierreFragment : PasoFragment() {

    private var _binding: FragmentPasoCierreBinding? = null
    private val binding get() = _binding!!
    override val contenido get() = _binding?.contenido
    private val session by lazy { SessionManager(requireContext()) }

    private val permisosBt = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) elegirImpresoraEImprimir()
        else Toast.makeText(requireContext(), "Se necesitan permisos Bluetooth para imprimir", Toast.LENGTH_LONG).show()
    }

    private val firmarCliente = registerForActivityResult(FirmaClienteActivity.Contrato()) { firmo ->
        if (firmo) {
            viewModel.firmaClienteCapturada()
            mostrarFirmaCliente()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasoCierreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardFirmaCliente.setOnClickListener { abrirFirmaCliente() }
        binding.btnLimpiarFirma.setOnClickListener { abrirFirmaCliente() }
        binding.btnConfigurarFirma.setOnClickListener { startActivity(Intent(requireContext(), FirmaActivity::class.java)) }

        // Papel 58 / 80 mm (se recuerda)
        binding.togglePapelAncho.check(if (session.anchoPapelMm == 58) R.id.btnPapel58 else R.id.btnPapel80)
        binding.togglePapelAncho.addOnButtonCheckedListener { _, id, checked ->
            if (checked) session.anchoPapelMm = if (id == R.id.btnPapel58) 58 else 80
        }
        binding.btnImprimirActa.setOnClickListener { pedirPermisosEImprimir() }
        binding.btnVistaPrevia.setOnClickListener { vistaPrevia() }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progressBar.visibility = if (estado is RevisionWizardViewModel.Estado.Guardando) View.VISIBLE else View.GONE
            binding.cardGuardado.visibility = if (estado is RevisionWizardViewModel.Estado.Guardado) View.VISIBLE else View.GONE
            mostrarFirmaCliente()
        }
        viewModel.orden.observe(viewLifecycleOwner) { r ->
            if (r == null) return@observe
            binding.tvEstadoEnvio.text = if (r.sincronizado) "✓ Revisión enviada al servidor (con acta PDF)"
            else "⟳ Guardada en el teléfono. Se envía al servidor automáticamente."
            binding.tvEstadoEnvio.setTextColor(requireContext().getColor(if (r.sincronizado) R.color.success else R.color.warning))
        }
    }

    override fun onResume() {
        super.onResume()
        mostrarFuncionario() // por si acaba de registrar la firma en el menú
    }

    override fun onListo(listas: RevisionWizardViewModel.Listas) {
        binding.etObservacion.setText(viewModel.observacion)
        binding.etObservacion.doAfterTextChanged { viewModel.observacion = it?.toString().orEmpty() }
        binding.etDocumento.setText(viewModel.documento)
        binding.etDocumento.doAfterTextChanged { viewModel.documento = it?.toString()?.trim().orEmpty() }
        llenarLista(binding.actvTipoDocumento, listas.tiposDocumento, viewModel.tipoDocumento) { viewModel.tipoDocumento = it ?: "CC" }
        mostrarFuncionario()
        mostrarFirmaCliente()
    }

    override fun aplicarSoloLectura() {
        super.aplicarSoloLectura()
        mostrarFirmaCliente()
    }

    private fun mostrarFuncionario() {
        val b = _binding ?: return
        b.tvFuncionario.text = viewModel.nombreFuncionario.ifBlank { "Funcionario" } +
            (viewModel.orden.value?.funcionarioCc?.takeIf { it.isNotBlank() }?.let { "  ·  C.C. $it" } ?: "")
        // En consulta se muestra la firma que quedó en la revisión
        val path = viewModel.orden.value?.firmaFuncionario?.takeIf { viewModel.soloLectura && File(it).exists() }
            ?: viewModel.firmaFuncionarioPath
        val bmp = path?.let { BitmapFactory.decodeFile(it) }
        b.ivFirmaFuncionario.setImageBitmap(bmp)
        b.ivFirmaFuncionario.visibility = if (bmp != null) View.VISIBLE else View.GONE
        b.tvSinFirmaFuncionario.visibility = if (bmp == null) View.VISIBLE else View.GONE
        b.btnConfigurarFirma.visibility = if (bmp == null && !viewModel.soloLectura) View.VISIBLE else View.GONE
    }

    /** Abre la firma del usuario a pantalla completa (horizontal). */
    private fun abrirFirmaCliente() {
        if (viewModel.soloLectura || viewModel.estado.value is RevisionWizardViewModel.Estado.Guardando) return
        val destino = viewModel.archivoFirmaCliente() ?: return
        val nombre = viewModel.orden.value?.nombreSuscriptor
        firmarCliente.launch(destino.absolutePath to nombre)
    }

    /** Recuadro de firma: vacío ("toque para firmar") o con la firma capturada. */
    private fun mostrarFirmaCliente() {
        val b = _binding ?: return
        val bmp = viewModel.firmaClienteGuardada?.let { BitmapFactory.decodeFile(it) }
        val editable = !viewModel.soloLectura
        b.ivFirmaClienteGuardada.setImageBitmap(bmp)
        b.ivFirmaClienteGuardada.visibility = if (bmp != null) View.VISIBLE else View.GONE
        b.layoutTocarParaFirmar.visibility = if (bmp == null && editable) View.VISIBLE else View.GONE
        b.btnLimpiarFirma.visibility = if (bmp != null && editable) View.VISIBLE else View.GONE
        b.btnLimpiarFirma.text = "Volver a firmar"
        b.tvAyudaFirma.visibility = if (bmp != null && editable) View.VISIBLE else View.GONE
        b.cardFirmaCliente.isClickable = editable
    }

    // ------------------------------------------------------------------
    // Acta: vista previa e impresión
    // ------------------------------------------------------------------

    private val papel get() = ActaFormatoC.Papel.from(session.anchoPapelMm)

    private fun vistaPrevia() {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = viewModel.datosActa() ?: return@launch
            val bmp = withContext(Dispatchers.Default) { ActaFormatoC.renderizar(datos, papel) }
            val ctx = context ?: return@launch
            val iv = ImageView(ctx).apply { setImageBitmap(bmp); adjustViewBounds = true }
            AlertDialog.Builder(ctx)
                .setTitle("Acta ${papel.etiqueta}")
                .setView(ScrollView(ctx).apply { addView(iv) })
                .setPositiveButton("Imprimir") { _, _ -> pedirPermisosEImprimir() }
                .setNegativeButton("Cerrar", null)
                .setOnDismissListener { bmp.recycle() }
                .show()
        }
    }

    private fun pedirPermisosEImprimir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permisos = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            if (permisos.any { ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }) {
                permisosBt.launch(permisos); return
            }
        }
        elegirImpresoraEImprimir()
    }

    @SuppressLint("MissingPermission")
    private fun elegirImpresoraEImprimir() {
        ImpresoraTermica.buscarPorDireccion(session.printerAddress)?.let { imprimir(it); return }
        val impresoras = ImpresoraTermica.impresorasPareadas()
        if (impresoras.isEmpty()) {
            Toast.makeText(requireContext(), "No hay impresoras Bluetooth vinculadas. Vincúlela en Ajustes > Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccione la impresora")
            .setItems(impresoras.map { "${it.name ?: "Impresora"} (${it.address})" }.toTypedArray()) { _, i ->
                session.printerName = impresoras[i].name
                session.printerAddress = impresoras[i].address
                imprimir(impresoras[i])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun imprimir(device: BluetoothDevice) {
        val b = _binding ?: return
        b.btnImprimirActa.isEnabled = false
        b.progressBar.visibility = View.VISIBLE
        val papelActual = papel
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                val datos = viewModel.datosActa() ?: return@withContext Result.failure(Exception("Sin datos"))
                val bmp = ActaFormatoC.renderizar(datos, papelActual)
                try { ImpresoraTermica.imprimir(device, bmp) } finally { bmp.recycle() }
            }
            val bb = _binding ?: return@launch
            bb.btnImprimirActa.isEnabled = true
            bb.progressBar.visibility = View.GONE
            resultado.onSuccess {
                Toast.makeText(requireContext(), "Acta impresa (${papelActual.etiqueta})", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                AlertDialog.Builder(requireContext())
                    .setTitle("No se pudo imprimir")
                    .setMessage("${e.message}\n\n¿Desea elegir otra impresora?")
                    .setPositiveButton("Elegir otra") { _, _ ->
                        session.printerAddress = null
                        elegirImpresoraEImprimir()
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
