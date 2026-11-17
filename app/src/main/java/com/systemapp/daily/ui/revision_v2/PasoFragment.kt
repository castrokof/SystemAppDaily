package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.systemapp.daily.R
import com.systemapp.daily.data.model.Opcion
import com.systemapp.daily.utils.habilitarTodo

/**
 * Base de los pasos del wizard: espera a que el formulario y las listas estén cargados
 * y entonces llama [onListo] una sola vez por vista. En modo consulta deshabilita [contenido].
 */
abstract class PasoFragment : Fragment() {

    protected val viewModel: RevisionWizardViewModel by activityViewModels()
    private var poblado = false

    /** Vista que se deshabilita en modo solo consulta. */
    protected abstract val contenido: View?

    protected abstract fun onListo(listas: RevisionWizardViewModel.Listas)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        poblado = false
        val intentar = {
            val listas = viewModel.listas.value
            if (!poblado && viewModel.cargado.value == true && listas != null) {
                poblado = true
                onListo(listas)
                aplicarSoloLectura()
            }
        }
        viewModel.cargado.observe(viewLifecycleOwner) { intentar() }
        viewModel.listas.observe(viewLifecycleOwner) { intentar() }
        viewModel.estado.observe(viewLifecycleOwner) { if (poblado) aplicarSoloLectura() }
    }

    protected open fun aplicarSoloLectura() {
        contenido?.habilitarTodo(!viewModel.soloLectura)
    }

    /** Chips de selección única. [alCambiar] recibe el código o null si se desmarca. */
    protected fun llenarChips(
        grupo: ChipGroup,
        opciones: List<Opcion>,
        seleccionado: String?,
        etiqueta: (Opcion) -> String = { "${it.codigo} · ${it.descripcion}" },
        alCambiar: (String?) -> Unit
    ) {
        grupo.removeAllViews()
        grupo.isSingleSelection = true
        val inflater = LayoutInflater.from(grupo.context)
        opciones.forEach { op ->
            val chip = inflater.inflate(R.layout.item_chip_opcion, grupo, false) as Chip
            chip.id = View.generateViewId()
            chip.text = etiqueta(op)
            chip.tag = op.codigo
            chip.isChecked = op.codigo == seleccionado
            grupo.addView(chip)
        }
        grupo.setOnCheckedStateChangeListener { g, ids ->
            alCambiar(ids.firstOrNull()?.let { g.findViewById<Chip>(it)?.tag as? String })
        }
    }

    /** Lista desplegable (Material exposed dropdown) por código. */
    protected fun llenarLista(
        campo: AutoCompleteTextView,
        opciones: List<Opcion>,
        seleccionado: String?,
        alCambiar: (String?) -> Unit
    ) {
        campo.setAdapter(ArrayAdapter(campo.context, android.R.layout.simple_list_item_1, opciones.map { it.descripcion }))
        val actual = opciones.firstOrNull { it.codigo == seleccionado || it.descripcion.equals(seleccionado, true) }
        campo.setText(actual?.descripcion ?: seleccionado.orEmpty(), false)
        campo.setOnItemClickListener { _, _, pos, _ -> alCambiar(opciones.getOrNull(pos)?.codigo) }
    }
}
