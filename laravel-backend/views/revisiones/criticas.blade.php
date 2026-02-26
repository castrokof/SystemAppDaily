{{-- resources/views/revisiones/criticas.blade.php --}}
{{-- Vista del SUPERVISOR para marcar lecturas criticas y generar ordenes de revision --}}
{{-- Usa AJAX: adicionarcritica / eliminarcritica (Coordenada = 'generar') --}}
@extends('layouts.app')

@section('title', 'Lecturas con Critica')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <div class="panel panel-danger">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-exclamation-triangle"></i> Lecturas con Critica (Estado = 4)
                    </h3>
                    <a href="{{ route('revisiones.index') }}" class="btn btn-default btn-sm pull-right">
                        <i class="fa fa-list"></i> Ver Ordenes de Revision
                    </a>
                </div>

                <div class="panel-body">
                    {{-- ALERTAS --}}
                    @if(session('success'))
                        <div class="alert alert-success alert-dismissible">
                            <button type="button" class="close" data-dismiss="alert">&times;</button>
                            {{ session('success') }}
                        </div>
                    @endif
                    @if(session('error'))
                        <div class="alert alert-danger alert-dismissible">
                            <button type="button" class="close" data-dismiss="alert">&times;</button>
                            {{ session('error') }}
                        </div>
                    @endif

                    {{-- CONTADORES --}}
                    <div class="row" style="margin-bottom:15px;">
                        <div class="col-md-3 col-sm-6">
                            <div class="panel panel-default" style="margin-bottom:10px;">
                                <div class="panel-body text-center" style="padding:10px;">
                                    <h4 style="margin:0; color:#e74c3c;">{{ $totalCriticas }}</h4>
                                    <small style="color:#777;">Total Criticas</small>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3 col-sm-6">
                            <div class="panel panel-default" style="margin-bottom:10px;">
                                <div class="panel-body text-center" style="padding:10px;">
                                    <h4 style="margin:0; color:#f39c12;" id="contMarcadas">{{ $totalMarcadas }}</h4>
                                    <small style="color:#777;">Marcadas para Revision</small>
                                </div>
                            </div>
                        </div>
                    </div>

                    {{-- FILTROS --}}
                    <form method="GET" action="{{ route('revisiones.criticas') }}" class="form-inline" style="margin-bottom:15px;">
                        <div class="form-group" style="margin-right:8px; margin-bottom:5px;">
                            <select name="critica" class="form-control input-sm">
                                <option value="">-- Tipo critica --</option>
                                @foreach($tiposCritica as $tipo)
                                    <option value="{{ $tipo }}" {{ request('critica') == $tipo ? 'selected' : '' }}>{{ $tipo }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:8px; margin-bottom:5px;">
                            <select name="marcadas" class="form-control input-sm">
                                <option value="">-- Todas --</option>
                                <option value="si" {{ request('marcadas') == 'si' ? 'selected' : '' }}>Marcadas</option>
                                <option value="no" {{ request('marcadas') == 'no' ? 'selected' : '' }}>Sin marcar</option>
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:8px; margin-bottom:5px;">
                            <input type="text" name="ciclo" class="form-control input-sm" placeholder="Ciclo" value="{{ request('ciclo') }}" style="width:70px;">
                        </div>
                        <div class="form-group" style="margin-right:8px; margin-bottom:5px;">
                            <input type="text" name="ruta" class="form-control input-sm" placeholder="Ruta" value="{{ request('ruta') }}" style="width:70px;">
                        </div>
                        <div class="form-group" style="margin-right:8px; margin-bottom:5px;">
                            <input type="text" name="buscar" class="form-control input-sm" placeholder="Buscar suscriptor, nombre..." value="{{ request('buscar') }}" style="width:200px;">
                        </div>
                        <button type="submit" class="btn btn-default btn-sm" style="margin-bottom:5px;"><i class="fa fa-search"></i> Filtrar</button>
                        <a href="{{ route('revisiones.criticas') }}" class="btn btn-default btn-sm" style="margin-bottom:5px;"><i class="fa fa-refresh"></i></a>
                    </form>

                    {{-- BARRA DE ACCIONES --}}
                    <div class="panel panel-default" style="margin-bottom:15px;">
                        <div class="panel-body" style="padding:12px;">
                            <div class="row">
                                <div class="col-md-3">
                                    <button type="button" class="btn btn-warning btn-sm btn-block" onclick="marcarSeleccionados()">
                                        <i class="fa fa-check-circle"></i> Marcar para Revision (<span id="contSel">0</span>)
                                    </button>
                                </div>
                                <div class="col-md-3">
                                    <button type="button" class="btn btn-default btn-sm btn-block" onclick="desmarcarSeleccionados()">
                                        <i class="fa fa-times-circle"></i> Quitar marca
                                    </button>
                                </div>
                                <div class="col-md-3">
                                    <select id="selectRevisor" class="form-control input-sm">
                                        <option value="">-- Asignar revisor --</option>
                                        @foreach($revisores as $id => $nombre)
                                            <option value="{{ $id }}">{{ $nombre }}</option>
                                        @endforeach
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <button type="button" class="btn btn-danger btn-sm btn-block" onclick="generarOrdenes()">
                                        <i class="fa fa-gavel"></i> Generar Ordenes
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {{-- TABLA DE LECTURAS CRITICAS --}}
                    <div class="table-responsive">
                        <table class="table table-bordered table-hover table-condensed" style="font-size:12px;">
                            <thead style="background-color:#f5f5f5;">
                                <tr>
                                    <th style="width:30px; text-align:center;">
                                        <input type="checkbox" id="checkAll" onclick="toggleAll(this)">
                                    </th>
                                    <th style="width:50px; text-align:center;">Estado</th>
                                    <th>Suscriptor</th>
                                    <th>Nombre</th>
                                    <th>Direccion</th>
                                    <th>Medidor</th>
                                    <th>Ruta</th>
                                    <th style="text-align:right;">L. Ant.</th>
                                    <th style="text-align:right;">L. Act.</th>
                                    <th style="text-align:right;">Consumo</th>
                                    <th style="text-align:right;">Prom.</th>
                                    <th>Critica</th>
                                </tr>
                            </thead>
                            <tbody>
                                @forelse($criticas as $lectura)
                                    @php
                                        $consumo = intval($lectura->Cons_Act);
                                        $marcada = ($lectura->Coordenada === 'generar');
                                        $rowStyle = '';
                                        if ($marcada) {
                                            $rowStyle = 'background-color:#fff3cd;'; // amarillo suave
                                        } elseif ($consumo < 0) {
                                            $rowStyle = 'background-color:#f8d7da;'; // rojo suave
                                        }
                                    @endphp
                                    <tr id="fila-{{ $lectura->id }}" style="{{ $rowStyle }}">
                                        <td style="text-align:center;">
                                            <input type="checkbox"
                                                   class="check-lectura"
                                                   value="{{ $lectura->id }}"
                                                   onchange="actualizarContador()">
                                        </td>
                                        <td style="text-align:center;">
                                            @if($marcada)
                                                <span class="label label-warning" style="font-size:10px;">MARCADA</span>
                                            @else
                                                <span class="label label-default" style="font-size:10px;">--</span>
                                            @endif
                                        </td>
                                        <td><strong>{{ $lectura->Suscriptor }}</strong></td>
                                        <td>{{ $lectura->Nombre }} {{ $lectura->Apell }}</td>
                                        <td>{{ $lectura->Direccion }}</td>
                                        <td>{{ $lectura->Ref_Medidor }}</td>
                                        <td>{{ $lectura->Ruta }}</td>
                                        <td style="text-align:right;">{{ $lectura->LA }}</td>
                                        <td style="text-align:right;"><strong>{{ $lectura->Lect_Actual }}</strong></td>
                                        <td style="text-align:right;">
                                            <strong style="color:{{ $consumo < 0 ? '#e74c3c' : ($consumo > intval($lectura->Promedio) * 2 ? '#f39c12' : '#333') }};">
                                                {{ $consumo }}
                                            </strong>
                                        </td>
                                        <td style="text-align:right;">{{ $lectura->Promedio }}</td>
                                        <td>
                                            <span class="label label-danger" style="font-size:10px;">{{ $lectura->Critica }}</span>
                                        </td>
                                    </tr>
                                @empty
                                    <tr>
                                        <td colspan="12" class="text-center" style="padding:20px;">
                                            No hay lecturas con critica (Estado = 4).
                                        </td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    {{-- PAGINACION --}}
                    <div class="text-center">
                        {!! $criticas->appends(request()->query())->render() !!}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

{{-- Formulario oculto para generar ordenes --}}
<form id="formGenerar" method="POST" action="{{ route('revisiones.generar') }}" style="display:none;">
    {{ csrf_field() }}
    <input type="hidden" name="usuario_id" id="hiddenUsuarioId">
</form>

<script>
// ============================
// SELECCION
// ============================
function toggleAll(el) {
    var checks = document.querySelectorAll('.check-lectura');
    for (var i = 0; i < checks.length; i++) { checks[i].checked = el.checked; }
    actualizarContador();
}

function actualizarContador() {
    var sel = document.querySelectorAll('.check-lectura:checked').length;
    document.getElementById('contSel').textContent = sel;
}

function getSeleccionados() {
    var checks = document.querySelectorAll('.check-lectura:checked');
    var ids = [];
    for (var i = 0; i < checks.length; i++) { ids.push(parseInt(checks[i].value)); }
    return ids;
}

// ============================
// MARCAR PARA REVISION (AJAX)
// Coordenada = 'generar'
// ============================
function marcarSeleccionados() {
    var ids = getSeleccionados();
    if (ids.length === 0) { alert('Seleccione al menos una lectura.'); return; }

    $.ajax({
        url: '{{ route("revisiones.adicionar-critica") }}',
        type: 'POST',
        data: {
            _token: '{{ csrf_token() }}',
            id: ids
        },
        success: function(resp) {
            if (resp.mensaje === 'ok') {
                // Actualizar filas visualmente
                for (var i = 0; i < ids.length; i++) {
                    var fila = document.getElementById('fila-' + ids[i]);
                    if (fila) {
                        fila.style.backgroundColor = '#fff3cd';
                        fila.querySelectorAll('.label')[0].className = 'label label-warning';
                        fila.querySelectorAll('.label')[0].style.fontSize = '10px';
                        fila.querySelectorAll('.label')[0].textContent = 'MARCADA';
                    }
                }
                // Actualizar contador
                var cont = document.getElementById('contMarcadas');
                cont.textContent = parseInt(cont.textContent) + ids.length;
                // Deseleccionar
                var checks = document.querySelectorAll('.check-lectura:checked');
                for (var j = 0; j < checks.length; j++) { checks[j].checked = false; }
                actualizarContador();
            }
        },
        error: function() { alert('Error al marcar las lecturas.'); }
    });
}

// ============================
// QUITAR MARCA (AJAX)
// Coordenada = NULL
// ============================
function desmarcarSeleccionados() {
    var ids = getSeleccionados();
    if (ids.length === 0) { alert('Seleccione al menos una lectura.'); return; }

    $.ajax({
        url: '{{ route("revisiones.eliminar-critica") }}',
        type: 'POST',
        data: {
            _token: '{{ csrf_token() }}',
            id: ids
        },
        success: function(resp) {
            if (resp.mensaje === 'ok') {
                for (var i = 0; i < ids.length; i++) {
                    var fila = document.getElementById('fila-' + ids[i]);
                    if (fila) {
                        fila.style.backgroundColor = '';
                        fila.querySelectorAll('.label')[0].className = 'label label-default';
                        fila.querySelectorAll('.label')[0].style.fontSize = '10px';
                        fila.querySelectorAll('.label')[0].textContent = '--';
                    }
                }
                var cont = document.getElementById('contMarcadas');
                cont.textContent = Math.max(0, parseInt(cont.textContent) - ids.length);
                var checks = document.querySelectorAll('.check-lectura:checked');
                for (var j = 0; j < checks.length; j++) { checks[j].checked = false; }
                actualizarContador();
            }
        },
        error: function() { alert('Error al desmarcar las lecturas.'); }
    });
}

// ============================
// GENERAR ORDENES DE REVISION
// Solo las marcadas (Coordenada='generar')
// ============================
function generarOrdenes() {
    var revisor = document.getElementById('selectRevisor').value;
    if (!revisor) {
        alert('Debe seleccionar un revisor antes de generar ordenes.');
        return;
    }
    var marcadas = parseInt(document.getElementById('contMarcadas').textContent);
    if (marcadas === 0) {
        alert('No hay lecturas marcadas para revision. Primero marque las lecturas.');
        return;
    }
    if (!confirm('Generar ordenes de revision para las ' + marcadas + ' lecturas marcadas?')) {
        return;
    }
    document.getElementById('hiddenUsuarioId').value = revisor;
    document.getElementById('formGenerar').submit();
}
</script>
@endsection
