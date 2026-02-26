{{-- resources/views/revisiones/criticas.blade.php --}}
{{-- Vista del SUPERVISOR para marcar lecturas criticas y generar ordenes de revision --}}
{{-- Usa AJAX: adicionarcritica / eliminarcritica (Coordenada = 'generar') --}}
@extends('layouts.app')

@section('title', 'Lecturas con Critica')

@section('content')

<style>
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.modern-card .card-header h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
.stat-card { background: white; border-radius: 16px; padding: 20px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.06); transition: all 0.3s ease; }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(0,0,0,0.1); }
.stat-card .stat-numero { font-size: 2rem; font-weight: 700; margin: 0; }
.stat-card .stat-label { color: #718096; font-size: 0.85rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.filtros-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); margin-bottom: 20px; }
.filtros-container .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 10px 14px; font-size: 0.9rem; transition: all 0.3s ease; }
.filtros-container .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
.acciones-bar { background: white; border-radius: 16px; padding: 18px 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); margin-bottom: 20px; }
.btn-marcar { background: linear-gradient(135deg, #f6d365 0%, #fda085 100%); color: white; border: none; border-radius: 12px; padding: 10px 20px; font-weight: 600; font-size: 0.9rem; transition: all 0.3s ease; }
.btn-marcar:hover { color: white; transform: translateY(-2px); box-shadow: 0 4px 15px rgba(253,160,133,0.4); }
.btn-desmarcar { background: #e2e8f0; color: #4a5568; border: none; border-radius: 12px; padding: 10px 20px; font-weight: 600; font-size: 0.9rem; transition: all 0.3s ease; }
.btn-desmarcar:hover { background: #cbd5e0; color: #2d3748; transform: translateY(-2px); }
.btn-generar { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; border: none; border-radius: 12px; padding: 10px 20px; font-weight: 600; font-size: 0.9rem; transition: all 0.3s ease; }
.btn-generar:hover { color: white; transform: translateY(-2px); box-shadow: 0 4px 15px rgba(235,51,73,0.4); }
.table-modern-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.08); overflow-x: auto; }
#tblCriticas { font-size: 0.82rem; border-radius: 12px; overflow: hidden; }
#tblCriticas thead th { background: linear-gradient(135deg, #3d57ceff 0%, #776a84ff 100%); color: white; font-weight: 600; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.5px; padding: 14px 8px; border: none; white-space: nowrap; text-align: center; }
#tblCriticas tbody td { padding: 10px 8px; vertical-align: middle; border-bottom: 1px solid #f0f0f0; text-align: center; font-size: 0.8rem; }
#tblCriticas tbody tr { background: white; transition: all 0.2s ease; }
#tblCriticas tbody tr:hover { background: linear-gradient(90deg, #f8f9ff 0%, #fff 100%); transform: scale(1.003); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1); }
.fila-marcada { background: linear-gradient(90deg, #fffbeb 0%, #fff 100%) !important; border-left: 4px solid #f6d365; }
.fila-negativa { background: linear-gradient(90deg, #fff5f5 0%, #fff 100%) !important; border-left: 4px solid #f5576c; }
.badge-estado { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge-marcada { background: linear-gradient(135deg, #f6d365 0%, #fda085 100%); color: white; }
.badge-sin-marcar { background: #e2e8f0; color: #718096; }
.badge-critica { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.consumo-alto { color: #f39c12; font-weight: 700; }
.consumo-negativo { color: #f5576c; font-weight: 700; }
.table-modern-container::-webkit-scrollbar { height: 10px; }
.table-modern-container::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 10px; }
.table-modern-container::-webkit-scrollbar-thumb { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 10px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>

<div class="container-fluid">

    {{-- ALERTAS --}}
    @if(session('success'))
        <div class="alert alert-success alert-dismissible" style="border-radius:12px; border:none; box-shadow:0 4px 15px rgba(17,153,142,0.2);">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <i class="fa fa-check-circle"></i> {{ session('success') }}
        </div>
    @endif
    @if(session('error'))
        <div class="alert alert-danger alert-dismissible" style="border-radius:12px; border:none; box-shadow:0 4px 15px rgba(245,87,108,0.2);">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <i class="fa fa-exclamation-circle"></i> {{ session('error') }}
        </div>
    @endif

    {{-- CARD PRINCIPAL --}}
    <div class="modern-card">
        <div class="card-header">
            <h3><i class="fa fa-exclamation-triangle"></i> Lecturas con Critica (Estado = 4)</h3>
            <a href="{{ route('revisiones.index') }}" class="btn" style="background:rgba(255,255,255,0.2); color:white; border:2px solid rgba(255,255,255,0.4); border-radius:10px; padding:8px 18px; font-weight:600;">
                <i class="fa fa-list"></i> Ver Ordenes de Revision
            </a>
        </div>
        <div class="card-body">

            {{-- CONTADORES --}}
            <div class="row" style="margin-bottom:20px;">
                <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                    <div class="stat-card">
                        <h4 class="stat-numero" style="color:#eb3349;">{{ $totalCriticas }}</h4>
                        <span class="stat-label">Total Criticas</span>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                    <div class="stat-card">
                        <h4 class="stat-numero" style="color:#f6d365;" id="contMarcadas">{{ $totalMarcadas }}</h4>
                        <span class="stat-label">Marcadas para Revision</span>
                    </div>
                </div>
            </div>

            {{-- FILTROS --}}
            <div class="filtros-container">
                <form method="GET" action="{{ route('revisiones.criticas') }}">
                    <div class="row">
                        <div class="col-md-2 col-sm-4" style="margin-bottom:10px;">
                            <select name="critica" class="form-control">
                                <option value="">-- Tipo critica --</option>
                                @foreach($tiposCritica as $tipo)
                                    <option value="{{ $tipo }}" {{ request('critica') == $tipo ? 'selected' : '' }}>{{ $tipo }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-2 col-sm-4" style="margin-bottom:10px;">
                            <select name="marcadas" class="form-control">
                                <option value="">-- Todas --</option>
                                <option value="si" {{ request('marcadas') == 'si' ? 'selected' : '' }}>Marcadas</option>
                                <option value="no" {{ request('marcadas') == 'no' ? 'selected' : '' }}>Sin marcar</option>
                            </select>
                        </div>
                        <div class="col-md-1 col-sm-4" style="margin-bottom:10px;">
                            <input type="text" name="ciclo" class="form-control" placeholder="Ciclo" value="{{ request('ciclo') }}">
                        </div>
                        <div class="col-md-1 col-sm-4" style="margin-bottom:10px;">
                            <input type="text" name="ruta" class="form-control" placeholder="Ruta" value="{{ request('ruta') }}">
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar suscriptor, nombre..." value="{{ request('buscar') }}">
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <button type="submit" class="btn" style="background:linear-gradient(135deg,#667eea,#764ba2); color:white; border:none; border-radius:10px; padding:10px 18px; font-weight:600; margin-right:5px;">
                                <i class="fa fa-search"></i> Filtrar
                            </button>
                            <a href="{{ route('revisiones.criticas') }}" class="btn" style="background:#e2e8f0; color:#4a5568; border:none; border-radius:10px; padding:10px 14px; font-weight:600;">
                                <i class="fa fa-refresh"></i>
                            </a>
                        </div>
                    </div>
                </form>
            </div>

            {{-- BARRA DE ACCIONES --}}
            <div class="acciones-bar">
                <div class="row">
                    <div class="col-md-3 col-sm-6" style="margin-bottom:8px;">
                        <button type="button" class="btn-marcar btn-block" onclick="marcarSeleccionados()">
                            <i class="fa fa-check-circle"></i> Marcar para Revision (<span id="contSel">0</span>)
                        </button>
                    </div>
                    <div class="col-md-3 col-sm-6" style="margin-bottom:8px;">
                        <button type="button" class="btn-desmarcar btn-block" onclick="desmarcarSeleccionados()">
                            <i class="fa fa-times-circle"></i> Quitar marca
                        </button>
                    </div>
                    <div class="col-md-3 col-sm-6" style="margin-bottom:8px;">
                        <select id="selectRevisor" class="form-control" style="border-radius:12px; border:2px solid #e2e8f0;">
                            <option value="">-- Asignar revisor --</option>
                            @foreach($revisores as $id => $nombre)
                                <option value="{{ $id }}">{{ $nombre }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-3 col-sm-6" style="margin-bottom:8px;">
                        <button type="button" class="btn-generar btn-block" onclick="generarOrdenes()">
                            <i class="fa fa-gavel"></i> Generar Ordenes
                        </button>
                    </div>
                </div>
            </div>

            {{-- TABLA DE LECTURAS CRITICAS --}}
            <div class="table-modern-container">
                <table id="tblCriticas" class="table table-hover" style="width:100%;">
                    <thead>
                        <tr>
                            <th style="width:30px;">
                                <input type="checkbox" id="checkAll" onclick="toggleAll(this)">
                            </th>
                            <th>Estado</th>
                            <th>Suscriptor</th>
                            <th>Nombre</th>
                            <th>Direccion</th>
                            <th>Medidor</th>
                            <th>Ruta</th>
                            <th>L. Ant.</th>
                            <th>L. Act.</th>
                            <th>Consumo</th>
                            <th>Prom.</th>
                            <th>Critica</th>
                        </tr>
                    </thead>
                    <tbody>
                        @forelse($criticas as $lectura)
                            @php
                                $consumo = intval($lectura->Cons_Act);
                                $marcada = ($lectura->Coordenada === 'generar');
                                $rowClass = '';
                                if ($marcada) { $rowClass = 'fila-marcada'; }
                                elseif ($consumo < 0) { $rowClass = 'fila-negativa'; }
                            @endphp
                            <tr id="fila-{{ $lectura->id }}" class="{{ $rowClass }}">
                                <td>
                                    <input type="checkbox" class="check-lectura" value="{{ $lectura->id }}" onchange="actualizarContador()">
                                </td>
                                <td>
                                    @if($marcada)
                                        <span class="badge-estado badge-marcada">MARCADA</span>
                                    @else
                                        <span class="badge-estado badge-sin-marcar">--</span>
                                    @endif
                                </td>
                                <td><strong style="color:#2d3748;">{{ $lectura->Suscriptor }}</strong></td>
                                <td style="text-align:left;">{{ $lectura->Nombre }} {{ $lectura->Apell }}</td>
                                <td style="text-align:left;">{{ $lectura->Direccion }}</td>
                                <td>{{ $lectura->Ref_Medidor }}</td>
                                <td>{{ $lectura->Ruta }}</td>
                                <td>{{ $lectura->LA }}</td>
                                <td><strong>{{ $lectura->Lect_Actual }}</strong></td>
                                <td>
                                    <strong class="{{ $consumo < 0 ? 'consumo-negativo' : ($consumo > intval($lectura->Promedio) * 2 ? 'consumo-alto' : '') }}">
                                        {{ $consumo }}
                                    </strong>
                                </td>
                                <td>{{ $lectura->Promedio }}</td>
                                <td><span class="badge-estado badge-critica">{{ $lectura->Critica }}</span></td>
                            </tr>
                        @empty
                            <tr>
                                <td colspan="12" style="padding:40px; color:#a0aec0;">
                                    <i class="fa fa-inbox" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                                    No hay lecturas con critica (Estado = 4).
                                </td>
                            </tr>
                        @endforelse
                    </tbody>
                </table>
            </div>

            <div class="text-center" style="margin-top:20px;">
                {!! $criticas->appends(request()->query())->render() !!}
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
                for (var i = 0; i < ids.length; i++) {
                    var fila = document.getElementById('fila-' + ids[i]);
                    if (fila) {
                        fila.className = 'fila-marcada';
                        var badges = fila.querySelectorAll('.badge-estado');
                        if (badges.length > 0) {
                            badges[0].className = 'badge-estado badge-marcada';
                            badges[0].textContent = 'MARCADA';
                        }
                    }
                }
                var cont = document.getElementById('contMarcadas');
                cont.textContent = parseInt(cont.textContent) + ids.length;
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
                        fila.className = '';
                        var badges = fila.querySelectorAll('.badge-estado');
                        if (badges.length > 0) {
                            badges[0].className = 'badge-estado badge-sin-marcar';
                            badges[0].textContent = '--';
                        }
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
