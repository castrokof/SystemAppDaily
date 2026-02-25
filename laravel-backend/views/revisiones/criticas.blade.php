{{-- resources/views/revisiones/criticas.blade.php --}}
{{-- Vista del SUPERVISOR para seleccionar lecturas con critica y generar ordenes de revision --}}
@extends('layouts.app')

@section('title', 'Lecturas con Critica')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <div class="panel panel-danger">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-exclamation-triangle"></i> Lecturas con Critica - Seleccionar para Revision
                    </h3>
                    <a href="{{ route('revisiones.index') }}" class="btn btn-default btn-sm pull-right">
                        <i class="fa fa-list"></i> Ver Revisiones
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

                    {{-- FILTROS --}}
                    <form method="GET" action="{{ route('revisiones.criticas') }}" class="form-inline" style="margin-bottom:15px;">
                        <div class="form-group" style="margin-right:10px;">
                            <select name="critica" class="form-control">
                                <option value="">-- Tipo de critica --</option>
                                @foreach($tiposCritica as $tipo)
                                    <option value="{{ $tipo }}" {{ request('critica') == $tipo ? 'selected' : '' }}>{{ $tipo }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <input type="text" name="ciclo" class="form-control" placeholder="Ciclo" value="{{ request('ciclo') }}" style="width:80px;">
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <input type="text" name="ruta" class="form-control" placeholder="Ruta" value="{{ request('ruta') }}" style="width:80px;">
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar suscriptor, nombre..." value="{{ request('buscar') }}">
                        </div>
                        <button type="submit" class="btn btn-default"><i class="fa fa-search"></i> Filtrar</button>
                        <a href="{{ route('revisiones.criticas') }}" class="btn btn-default"><i class="fa fa-refresh"></i></a>
                    </form>

                    {{-- FORMULARIO DE GENERACION --}}
                    <form method="POST" action="{{ route('revisiones.generar') }}" id="formGenerar">
                        {{ csrf_field() }}

                        {{-- Barra de acciones --}}
                        <div class="well well-sm">
                            <div class="row">
                                <div class="col-md-4">
                                    <label>Asignar al revisor:</label>
                                    <select name="usuario_id" class="form-control" required>
                                        <option value="">-- Seleccionar revisor --</option>
                                        @foreach($revisores as $id => $nombre)
                                            <option value="{{ $id }}">{{ $nombre }}</option>
                                        @endforeach
                                    </select>
                                </div>
                                <div class="col-md-4" style="padding-top:25px;">
                                    <button type="submit" class="btn btn-danger" onclick="return confirmarGenerar()">
                                        <i class="fa fa-gavel"></i> Generar Ordenes de Revision (<span id="contadorSeleccionados">0</span>)
                                    </button>
                                </div>
                                <div class="col-md-4" style="padding-top:25px;">
                                    <button type="button" class="btn btn-default" onclick="seleccionarTodos()">
                                        <i class="fa fa-check-square-o"></i> Seleccionar Todos
                                    </button>
                                    <button type="button" class="btn btn-default" onclick="deseleccionarTodos()">
                                        <i class="fa fa-square-o"></i> Ninguno
                                    </button>
                                </div>
                            </div>
                        </div>

                        {{-- TABLA DE LECTURAS CRITICAS --}}
                        <div class="table-responsive">
                            <table class="table table-striped table-bordered table-hover table-condensed">
                                <thead>
                                    <tr>
                                        <th style="width:30px;">
                                            <input type="checkbox" id="checkAll" onclick="seleccionarTodos()">
                                        </th>
                                        <th>Suscriptor</th>
                                        <th>Nombre</th>
                                        <th>Direccion</th>
                                        <th>Medidor</th>
                                        <th>Ruta</th>
                                        <th>Lect. Ant.</th>
                                        <th>Lect. Act.</th>
                                        <th>Consumo</th>
                                        <th>Promedio</th>
                                        <th>Critica</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    @forelse($criticas as $lectura)
                                        <tr class="{{ intval($lectura->Cons_Act) < 0 ? 'danger' : '' }}">
                                            <td>
                                                <input type="checkbox"
                                                       name="lecturas[]"
                                                       value="{{ $lectura->id }}"
                                                       class="check-lectura"
                                                       onchange="actualizarContador()">
                                            </td>
                                            <td><strong>{{ $lectura->Suscriptor }}</strong></td>
                                            <td>{{ $lectura->Nombre }} {{ $lectura->Apell }}</td>
                                            <td>{{ $lectura->Direccion }}</td>
                                            <td>{{ $lectura->Ref_Medidor }}</td>
                                            <td>{{ $lectura->Ruta }}</td>
                                            <td>{{ $lectura->LA }}</td>
                                            <td><strong>{{ $lectura->Lect_Actual }}</strong></td>
                                            <td>
                                                @php $consumo = intval($lectura->Cons_Act); @endphp
                                                <strong class="{{ $consumo < 0 ? 'text-danger' : ($consumo > intval($lectura->Promedio) * 2 ? 'text-warning' : '') }}">
                                                    {{ $consumo }}
                                                </strong>
                                            </td>
                                            <td>{{ $lectura->Promedio }}</td>
                                            <td>
                                                <span class="label label-danger">{{ $lectura->Critica }}</span>
                                            </td>
                                        </tr>
                                    @empty
                                        <tr>
                                            <td colspan="11" class="text-center">No hay lecturas con critica pendientes de revision.</td>
                                        </tr>
                                    @endforelse
                                </tbody>
                            </table>
                        </div>
                    </form>

                    {{-- PAGINACION --}}
                    <div class="text-center">
                        {!! $criticas->appends(request()->query())->render() !!}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
function seleccionarTodos() {
    var checks = document.querySelectorAll('.check-lectura');
    for (var i = 0; i < checks.length; i++) { checks[i].checked = true; }
    actualizarContador();
}
function deseleccionarTodos() {
    var checks = document.querySelectorAll('.check-lectura');
    for (var i = 0; i < checks.length; i++) { checks[i].checked = false; }
    actualizarContador();
}
function actualizarContador() {
    var seleccionados = document.querySelectorAll('.check-lectura:checked').length;
    document.getElementById('contadorSeleccionados').textContent = seleccionados;
}
function confirmarGenerar() {
    var seleccionados = document.querySelectorAll('.check-lectura:checked').length;
    if (seleccionados === 0) {
        alert('Debe seleccionar al menos una lectura.');
        return false;
    }
    var revisor = document.querySelector('select[name="usuario_id"]').value;
    if (!revisor) {
        alert('Debe seleccionar un revisor.');
        return false;
    }
    return confirm('Generar ' + seleccionados + ' ordenes de revision?');
}
</script>
@endsection
