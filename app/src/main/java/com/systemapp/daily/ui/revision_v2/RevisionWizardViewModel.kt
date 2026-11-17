package com.systemapp.daily.ui.revision_v2

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.acta.ActaFormatoC
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.*
import com.systemapp.daily.data.sync.SyncWorker
import com.systemapp.daily.utils.AppScope
import com.systemapp.daily.utils.FotoUtils
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estado del wizard de revisión (Formato C).
 *
 * - Los campos se escriben en el ViewModel en cuanto el usuario los cambia (no en onPause),
 *   así Finalizar nunca pierde lo que está en la pantalla visible.
 * - Mientras la orden está PENDIENTE se guarda un borrador en Room al pausar la pantalla
 *   (p. ej. al abrir la cámara). Si Android mata el proceso, al volver se recupera todo:
 *   predio, causas, censo, fotos.
 */
class RevisionWizardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.ordenRevisionDao()
    private val hidraulicoDao = db.hidraulicoDao()
    private val syncQueueDao = db.syncQueueDao()
    private val listaDao = db.listaDao()
    private val session = SessionManager(application)

    // ---------------- Orden ----------------
    private val idOrden = MutableLiveData<Int>()

    /** Orden en BD (se actualiza sola, p. ej. cuando el worker la marca sincronizada). */
    val orden: LiveData<RevisionEntity?> = idOrden.switchMap { dao.observar(it).asLiveData() }

    /** true cuando los campos del formulario ya se llenaron desde la BD. */
    private val _cargado = MutableLiveData(false)
    val cargado: LiveData<Boolean> = _cargado

    var soloLectura = false
        private set

    // ---------------- Listas ----------------
    data class Listas(
        val tiposPredio: List<Opcion>,
        val usos: List<Opcion>,
        val movimientos: List<Opcion>,
        val causas: List<Opcion>,
        val acometida: List<Opcion>,
        val sellos: List<Opcion>,
        val tiposDocumento: List<Opcion>
    )

    private val _listas = MutableLiveData<Listas>()
    val listas: LiveData<Listas> = _listas

    // ---------------- Formulario ----------------
    // Paso 1 — Predio
    var tipoPredio: String? = null
    var usoPredio: String? = null
    var habitantes: String = ""
    var tiempoHabitacion: String = ""

    // Paso 2 — Visita
    var lecturaVisita: String = ""
    var fechaVisita: String? = null
    var medidorMovimiento: String? = null

    // Paso 3 — Causas
    val causas = linkedSetOf<String>()
    var causaOtroCual: String = ""

    // Paso 4 — Adicional (opcional)
    var estadoAcometida: String? = null
    var estadoSellos: String? = null
    var nombreAtiende: String = ""
    var numFamilias: String = ""
    var gpsLatitud: Double? = null
    var gpsLongitud: Double? = null

    private val _censo = MutableLiveData<List<HidraulicoEntity>>(emptyList())
    val censo: LiveData<List<HidraulicoEntity>> = _censo

    private val _fotos = MutableLiveData<List<String>>(emptyList())
    val fotos: LiveData<List<String>> = _fotos
    private val fotosAntesDeCargar = mutableListOf<String>()

    // Paso 5 — Cierre
    var observacion: String = ""
    var tipoDocumento: String = "CC"
    var documento: String = ""
    /** PNG de la firma del cliente (se captura a pantalla completa en FirmaClienteActivity). */
    var firmaClienteGuardada: String? = null
        private set

    /** Dónde guarda FirmaClienteActivity la firma de esta orden. */
    fun archivoFirmaCliente(): File? {
        val id = idOrden.value ?: return null
        return File(File(getApplication<Application>().filesDir, "firmas"), "firma_cliente_$id.png")
    }

    /** La firma quedó escrita en disco: se asocia y se guarda en el borrador de una vez. */
    fun firmaClienteCapturada() {
        val f = archivoFirmaCliente()?.takeIf { it.exists() } ?: return
        firmaClienteGuardada = f.absolutePath
        guardarBorrador()
    }

    val firmaFuncionarioPath: String?
        get() = session.firmaPath?.takeIf { File(it).exists() }

    val nombreFuncionario: String
        get() = orden.value?.funcionarioNombre?.takeIf { it.isNotBlank() } ?: session.userName ?: session.userUsuario ?: ""

    // ---------------- Resultado ----------------
    sealed class Estado {
        object Editando : Estado()
        object Guardando : Estado()
        data class Guardado(val revision: RevisionEntity) : Estado()
        data class Error(val mensaje: String) : Estado()
    }

    private val _estado = MutableLiveData<Estado>(Estado.Editando)
    val estado: LiveData<Estado> = _estado

    private val fechaFmt get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // =====================================================================
    // Carga
    // =====================================================================

    fun cargarOrden(id: Int) {
        if (idOrden.value == id) return
        idOrden.value = id
        viewModelScope.launch(Dispatchers.IO) {
            _listas.postValue(cargarListas())
            val r = dao.getById(id) ?: return@launch
            val censoBd = hidraulicoDao.getByRevision(id)
            withContext(Dispatchers.Main) { llenarDesde(r, censoBd) }
        }
    }

    private suspend fun cargarListas(): Listas {
        suspend fun l(tipo: String): List<Opcion> =
            listaDao.getByTipo(tipo).map { Opcion(it.codigo, it.descripcion) }.ifEmpty { ListasRespaldo.get(tipo) }

        // El servidor ordena por descripción; las causas se muestran como en el formato, "Otro" al final.
        val causas = l(TipoLista.CAUSA_CONSUMO).sortedBy { if (it.codigo == ListasRespaldo.CAUSA_OTRO) 1 else 0 }
        return Listas(
            tiposPredio = ordenarComo(l(TipoLista.TIPO_PREDIO), TipoLista.TIPO_PREDIO),
            usos = ordenarComo(l(TipoLista.USO_PREDIO), TipoLista.USO_PREDIO),
            movimientos = ordenarComo(l(TipoLista.MOVIMIENTO_MEDIDOR), TipoLista.MOVIMIENTO_MEDIDOR),
            causas = causas,
            acometida = l(TipoLista.ACOMETIDA),
            sellos = l(TipoLista.SELLOS),
            tiposDocumento = l(TipoLista.TIPO_DOCUMENTO)
        )
    }

    /** Mantiene el orden del papel (H, D, C / R, C, I, O, E) aunque el servidor ordene alfabético. */
    private fun ordenarComo(lista: List<Opcion>, tipo: String): List<Opcion> {
        val ref = ListasRespaldo.get(tipo).map { it.codigo }
        return lista.sortedBy { ref.indexOf(it.codigo).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    private fun llenarDesde(r: RevisionEntity, censoBd: List<HidraulicoEntity>) {
        // Una revisión ya finalizada se abre solo para consulta/reimpresión.
        // Para corregirla se usa "Retomar" desde la lista de ejecutadas.
        soloLectura = r.estadoOrden == EstadoOrden.EJECUTADO
        tipoPredio = r.tipoPredio
        usoPredio = r.usoPredio
        habitantes = r.numPersonas?.toString().orEmpty()
        tiempoHabitacion = r.tiempoHabitacion.orEmpty()
        lecturaVisita = r.lecturaActual.orEmpty()
        // Hora de la visita: la del teléfono al abrir por primera vez (editable)
        fechaVisita = r.fechaVisita ?: fechaFmt.format(Date())
        medidorMovimiento = r.medidorMovimiento
        causas.clear(); causas.addAll(CausasJson.parse(r.causasConsumo))
        causaOtroCual = r.causaOtroCual.orEmpty()
        estadoAcometida = r.estadoAcometida
        estadoSellos = r.estadoSellos
        nombreAtiende = r.nombreAtiende.orEmpty()
        numFamilias = r.numFamilias?.toString().orEmpty()
        gpsLatitud = r.gpsLatitudPredio
        gpsLongitud = r.gpsLongitudPredio
        observacion = r.generalidades.orEmpty()
        tipoDocumento = r.tipoDocumento ?: "CC"
        documento = r.documento.orEmpty()
        // Si la firma llegó antes de terminar de cargar (p. ej. tras recrear el proceso), se respeta
        firmaClienteGuardada = firmaClienteGuardada ?: r.firmaCliente?.takeIf { File(it).exists() }
        _censo.value = censoBd
        // Fotos del borrador + las que llegaron de la cámara antes de terminar de cargar
        _fotos.value = (r.fotos + fotosAntesDeCargar).distinct().filter { File(it).exists() }
        fotosAntesDeCargar.clear()
        if (r.estadoOrden == EstadoOrden.EJECUTADO) _estado.value = Estado.Guardado(r)
        _cargado.value = true
    }

    // =====================================================================
    // Edición
    // =====================================================================

    fun toggleCausa(codigo: String, marcada: Boolean) {
        if (marcada) causas.add(codigo) else causas.remove(codigo)
    }

    fun agregarPuntoHidraulico(tipoPunto: String, cantidad: Int, estado: String) {
        _censo.value = _censo.value.orEmpty() + HidraulicoEntity(
            idRevision = idOrden.value ?: 0, tipoPunto = tipoPunto, cantidad = cantidad, estado = estado
        )
    }

    fun eliminarPuntoHidraulico(index: Int) {
        val l = _censo.value.orEmpty()
        if (index in l.indices) _censo.value = l.filterIndexed { i, _ -> i != index }
    }

    /** Comprime la foto en segundo plano y la agrega (también si llega tras recrear el proceso). */
    fun agregarFoto(path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FotoUtils.comprimir(path) }
            if (_cargado.value == true) _fotos.value = (_fotos.value.orEmpty() + path).distinct()
            else fotosAntesDeCargar += path
        }
    }

    fun eliminarFoto(index: Int) {
        val l = _fotos.value.orEmpty()
        if (index !in l.indices) return
        val path = l[index]
        _fotos.value = l.filterIndexed { i, _ -> i != index }
        AppScope.launch { runCatching { File(path).delete() } }
    }

    // =====================================================================
    // Validación
    // =====================================================================

    /** @return mensaje de error del paso, o null si está completo. */
    fun validarPaso(paso: Int): String? = when (paso) {
        PASO_PREDIO -> when {
            tipoPredio.isNullOrBlank() -> "Seleccione el estado del predio (H / D / C)"
            usoPredio.isNullOrBlank() -> "Seleccione el uso del predio"
            habitantes.toIntOrNull()?.let { it < 0 } != false -> "Digite el número de habitantes"
            else -> null
        }
        PASO_VISITA -> when {
            lecturaVisita.toLongOrNull()?.let { it < 0 } != false -> "Digite la lectura al momento de la visita"
            fechaVisita.isNullOrBlank() -> "Indique la fecha y hora de la visita"
            medidorMovimiento.isNullOrBlank() -> "Indique si el medidor está con o sin movimiento"
            else -> null
        }
        PASO_CAUSAS -> when {
            causas.isEmpty() -> "Marque al menos una causa del consumo"
            ListasRespaldo.CAUSA_OTRO in causas && causaOtroCual.isBlank() -> "Escriba cuál es la otra causa"
            else -> null
        }
        PASO_CIERRE -> when {
            firmaFuncionarioPath == null -> "Falta la firma del funcionario. Regístrela en Menú > Firma"
            else -> null
        }
        else -> null
    }

    /** Lectura de visita menor que la anterior: solo advertencia (no bloquea). */
    fun advertenciaLectura(): String? {
        val visita = lecturaVisita.toLongOrNull() ?: return null
        val anterior = orden.value?.lecturaAnterior?.trim()?.toLongOrNull() ?: return null
        return if (visita < anterior) "La lectura ($visita) es menor que la anterior ($anterior)" else null
    }

    /** Primer paso incompleto (para saltar a él al finalizar). */
    fun primerPasoInvalido(): Pair<Int, String>? {
        for (p in listOf(PASO_PREDIO, PASO_VISITA, PASO_CAUSAS, PASO_CIERRE)) {
            validarPaso(p)?.let { return p to it }
        }
        if (firmaClienteGuardada?.let { File(it).exists() } != true) return PASO_CIERRE to "Falta la firma del usuario"
        return null
    }

    // =====================================================================
    // Persistencia
    // =====================================================================

    private fun aplicarFormulario(base: RevisionEntity) = base.copy(
        tipoPredio = tipoPredio,
        usoPredio = usoPredio,
        numPersonas = habitantes.toIntOrNull(),
        tiempoHabitacion = tiempoHabitacion.trim().ifBlank { null },
        lecturaActual = lecturaVisita.trim().ifBlank { null },
        fechaVisita = fechaVisita,
        medidorMovimiento = medidorMovimiento,
        causasConsumo = CausasJson.toJson(causas),
        causaOtroCual = causaOtroCual.trim().takeIf { ListasRespaldo.CAUSA_OTRO in causas && it.isNotBlank() },
        estadoAcometida = estadoAcometida,
        estadoSellos = estadoSellos,
        nombreAtiende = nombreAtiende.trim().ifBlank { null },
        numFamilias = numFamilias.toIntOrNull(),
        gpsLatitudPredio = gpsLatitud,
        gpsLongitudPredio = gpsLongitud,
        generalidades = observacion.trim().ifBlank { null },
        tipoDocumento = tipoDocumento.ifBlank { null },
        documento = documento.trim().ifBlank { null },
        rutaFotos = _fotos.value.orEmpty().joinToString(",").ifBlank { null },
        firmaCliente = firmaClienteGuardada ?: base.firmaCliente
    )

    /** Borrador: solo para órdenes pendientes. No cambia el estado de la orden. */
    fun guardarBorrador() {
        val id = idOrden.value ?: return
        if (_cargado.value != true || soloLectura || _estado.value is Estado.Guardando) return
        val censoActual = _censo.value.orEmpty()
        val form = this::aplicarFormulario
        AppScope.launch {
            val actual = dao.getById(id) ?: return@launch
            if (actual.estadoOrden != EstadoOrden.PENDIENTE) return@launch
            dao.update(form(actual))
            reemplazarCenso(id, censoActual)
        }
    }

    private suspend fun reemplazarCenso(id: Int, items: List<HidraulicoEntity>) {
        hidraulicoDao.deleteByRevision(id)
        if (items.isNotEmpty()) hidraulicoDao.insertAll(items.map { it.copy(id = 0, idRevision = id) })
    }

    /**
     * Cierra la revisión: guarda firmas, marca EJECUTADO, genera el acta PDF (mismo diseño
     * que la impresa) y la deja en cola de envío. No espera la red: el envío lo hace el
     * worker en segundo plano y la pantalla se actualiza sola al quedar sincronizada.
     * La firma del cliente ya está en disco (FirmaClienteActivity).
     */
    fun finalizar() {
        val id = idOrden.value ?: return
        if (_estado.value is Estado.Guardando) return
        _estado.value = Estado.Guardando
        val censoActual = _censo.value.orEmpty()
        val app = getApplication<Application>()

        viewModelScope.launch {
            try {
                val guardada = withContext(Dispatchers.IO) {
                    val dirFirmas = File(app.filesDir, "firmas").apply { mkdirs() }

                    // 1. Firma del cliente (capturada a pantalla completa, ya en disco)
                    val firmaClientePath = firmaClienteGuardada?.takeIf { File(it).exists() }
                        ?: error("Falta la firma del usuario")

                    // 2. Copia de la firma del funcionario (la del menú) para esta revisión
                    val firmaFuncPath = firmaFuncionarioPath?.let { origen ->
                        val f = File(dirFirmas, "firma_funcionario_$id.png")
                        File(origen).copyTo(f, overwrite = true)
                        f.absolutePath
                    }

                    val base = dao.getById(id) ?: error("La orden ya no existe")
                    val ejecutada = aplicarFormulario(base).copy(
                        estadoOrden = EstadoOrden.EJECUTADO,
                        firmaCliente = firmaClientePath,
                        firmaFuncionario = firmaFuncPath,
                        funcionarioNombre = base.funcionarioNombre ?: session.userName,
                        fechaVisita = fechaVisita ?: fechaFmt.format(Date()),
                        fechaCierre = fechaFmt.format(Date()),
                        sincronizado = false
                    )
                    dao.update(ejecutada)
                    reemplazarCenso(id, censoActual)

                    // 3. Acta PDF (mismo diseño que la impresa)
                    val conActa = try {
                        val pdf = ActaFormatoC.generarPdf(app, ActaFormatoC.construirDatos(app, ejecutada))
                        ejecutada.copy(actaPdfPath = pdf.absolutePath).also { dao.update(it) }
                    } catch (_: Exception) { ejecutada } // el uploader la regenera si falta

                    // 4. Cola de envío (idempotente por id_orden)
                    val existente = syncQueueDao.buscarPorRegistro(TipoSync.REVISION_V2, id)
                    if (existente == null) {
                        syncQueueDao.insert(SyncQueueEntity(
                            tipo = TipoSync.REVISION_V2, registroId = id,
                            estado = EstadoSync.PENDIENTE, fechaCreacion = fechaFmt.format(Date())
                        ))
                    } else {
                        syncQueueDao.reencolar(existente.id)
                    }
                    conActa
                }
                firmaClienteGuardada = guardada.firmaCliente
                soloLectura = true
                SyncWorker.ejecutarSincronizacionInmediata(app)
                _estado.value = Estado.Guardado(guardada)
            } catch (e: Exception) {
                _estado.value = Estado.Error("No se pudo guardar: ${e.localizedMessage}")
            }
        }
    }

    /** Datos del acta para imprimir (lee lo último guardado en BD). */
    suspend fun datosActa(): ActaFormatoC.Datos? = withContext(Dispatchers.IO) {
        val id = idOrden.value ?: return@withContext null
        dao.getById(id)?.let { ActaFormatoC.construirDatos(getApplication(), it) }
    }

    companion object {
        const val PASO_PREDIO = 0
        const val PASO_VISITA = 1
        const val PASO_CAUSAS = 2
        const val PASO_ADICIONAL = 3
        const val PASO_CIERRE = 4
        const val TOTAL_PASOS = 5
    }
}
