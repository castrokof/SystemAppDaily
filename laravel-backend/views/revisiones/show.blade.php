{{-- resources/views/revisiones/show.blade.php --}}
@extends('layouts.app')

@section('title', 'Revision: ' . $revision->codigo_predio)

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-10 col-md-offset-1">

            @if(session('success'))
                <div class="alert alert-success alert-dismissible">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    {{ session('success') }}
                </div>
            @endif

            {{-- PANEL: DATOS DE LA LECTURA ORIGINAL --}}
            <div class="panel panel-danger">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        <i class="fa fa-exclamation-triangle"></i> Lectura Original (Critica)
                    </h3>
                </div>
                <div class="panel-body">
                    <div class="row">
                        <div class="col-md-6">
                            <table class="table table-condensed">
                                <tr><th style="width:40%;">Predio / Suscriptor:</th><td><strong>{{ $revision->codigo_predio }}</strong></td></tr>
                                <tr><th>Nombre:</th><td>{{ $revision->nombre_suscriptor ?: '-' }}</td></tr>
                                <tr><th>Direccion:</th><td>{{ $revision->direccion ?: '-' }}</td></tr>
                                <tr><th>Telefono:</th><td>{{ $revision->telefono ?: '-' }}</td></tr>
                                <tr><th>Ref. Medidor:</th><td>{{ $revision->ref_medidor ?: '-' }}</td></tr>
                            </table>
                        </div>
                        <div class="col-md-6">
                            <table class="table table-condensed">
                                <tr><th style="width:40%;">Lectura Anterior:</th><td>{{ $revision->lectura_anterior ?: '-' }}</td></tr>
                                <tr><th>Lectura Actual:</th><td><strong>{{ $revision->lectura_actual ?: '-' }}</strong></td></tr>
                                <tr>
                                    <th>Consumo:</th>
                                    <td>
                                        @if($revision->consumo_actual !== null)
                                            <strong class="{{ $revision->consumo_actual < 0 ? 'text-danger' : '' }}">{{ $revision->consumo_actual }}</strong>
                                        @else
                                            -
                                        @endif
                                    </td>
                                </tr>
                                <tr><th>Promedio:</th><td>{{ $revision->promedio ?: '-' }}</td></tr>
                                <tr><th>Critica:</th><td><span class="label label-danger">{{ $revision->critica_original ?: '-' }}</span></td></tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            {{-- PANEL: ORDEN DE REVISION --}}
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-clipboard"></i> Orden de Revision #{{ $revision->id }}
                    </h3>
                    <div class="pull-right">
                        @if($revision->estado_orden == 'PENDIENTE')
                            <span class="label label-warning" style="font-size:14px;">PENDIENTE</span>
                        @else
                            <span class="label label-success" style="font-size:14px;">EJECUTADO</span>
                        @endif
                    </div>
                </div>

                <div class="panel-body">
                    @if($revision->estado_orden == 'PENDIENTE')
                        <div class="alert alert-info">
                            <i class="fa fa-info-circle"></i> Esta orden aun no ha sido ejecutada por el revisor en campo.
                        </div>

                        {{-- Reasignar --}}
                        <form method="POST" action="{{ route('revisiones.reasignar', $revision->id) }}" class="form-inline" style="margin-bottom:20px;">
                            {{ csrf_field() }}
                            <div class="form-group">
                                <label>Revisor asignado:</label>
                                <strong style="margin-left:5px;">{{ $revision->usuario ? $revision->usuario->nombre : 'Sin asignar' }}</strong>
                            </div>
                        </form>
                    @else
                        {{-- DATOS LLENADOS POR EL REVISOR EN CAMPO --}}
                        <div class="row">
                            {{-- PASO 1: Registro --}}
                            <div class="col-md-6">
                                <h4><span class="label label-primary">Paso 1</span> Registro</h4>
                                <table class="table table-condensed">
                                    <tr><th style="width:40%;">Nombre quien atiende:</th><td>{{ $revision->nombre_atiende ?: '-' }}</td></tr>
                                    <tr><th>Tipo Documento:</th><td>{{ $revision->tipo_documento ?: '-' }}</td></tr>
                                    <tr><th>Documento:</th><td>{{ $revision->documento ?: '-' }}</td></tr>
                                    <tr><th>Motivo Revision:</th><td><span class="label label-info">{{ $revision->motivo_revision ? str_replace('_', ' ', $revision->motivo_revision) : '-' }}</span></td></tr>
                                    @if($revision->motivo_detalle)
                                        <tr><th>Detalle Motivo:</th><td>{{ $revision->motivo_detalle }}</td></tr>
                                    @endif
                                </table>
                            </div>

                            {{-- PASO 2: Predio --}}
                            <div class="col-md-6">
                                <h4><span class="label label-primary">Paso 2</span> Predio</h4>
                                <table class="table table-condensed">
                                    <tr><th style="width:40%;">Estado Acometida:</th><td>{{ $revision->estado_acometida ?: '-' }}</td></tr>
                                    <tr><th>Estado Sellos:</th><td>{{ $revision->estado_sellos ?: '-' }}</td></tr>
                                    <tr><th>Generalidades:</th><td>{{ $revision->generalidades ?: '-' }}</td></tr>
                                    <tr>
                                        <th>GPS:</th>
                                        <td>
                                            @if($revision->gps_latitud_predio && $revision->gps_longitud_predio)
                                                {{ $revision->gps_latitud_predio }}, {{ $revision->gps_longitud_predio }}
                                                <a href="https://www.google.com/maps?q={{ $revision->gps_latitud_predio }},{{ $revision->gps_longitud_predio }}" target="_blank" class="btn btn-xs btn-default">
                                                    <i class="fa fa-map-marker"></i> Ver mapa
                                                </a>
                                            @else
                                                <span class="text-muted">Sin GPS</span>
                                            @endif
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        <div class="row">
                            {{-- PASO 3: Familia --}}
                            <div class="col-md-6">
                                <h4><span class="label label-primary">Paso 3</span> Familia</h4>
                                <table class="table table-condensed">
                                    <tr><th style="width:40%;">Num. Familias:</th><td>{{ $revision->num_familias ?: '-' }}</td></tr>
                                    <tr><th>Num. Personas:</th><td>{{ $revision->num_personas ?: '-' }}</td></tr>
                                </table>
                            </div>

                            {{-- PASO 4: Censo Hidraulico --}}
                            <div class="col-md-6">
                                <h4><span class="label label-primary">Paso 4</span> Censo Hidraulico</h4>
                                @if($revision->censoHidraulico->count() > 0)
                                    <table class="table table-condensed table-bordered">
                                        <thead>
                                            <tr>
                                                <th>Tipo Punto</th>
                                                <th>Cantidad</th>
                                                <th>Estado</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            @foreach($revision->censoHidraulico as $punto)
                                                <tr>
                                                    <td>{{ $punto->tipo_punto }}</td>
                                                    <td>{{ $punto->cantidad }}</td>
                                                    <td>
                                                        @if($punto->estado == 'BUENO')
                                                            <span class="label label-success">{{ $punto->estado }}</span>
                                                        @else
                                                            <span class="label label-danger">{{ $punto->estado }}</span>
                                                        @endif
                                                    </td>
                                                </tr>
                                            @endforeach
                                        </tbody>
                                    </table>
                                @else
                                    <p class="text-muted">Sin datos de censo hidraulico.</p>
                                @endif
                            </div>
                        </div>

                        <hr>

                        {{-- PASO 5: Fotos + Firma --}}
                        <h4><span class="label label-primary">Paso 5</span> Evidencia</h4>
                        <div class="row">
                            {{-- Fotos --}}
                            <div class="col-md-8">
                                <h5>Fotos ({{ $revision->fotos->count() }})</h5>
                                @if($revision->fotos->count() > 0)
                                    <div class="row">
                                        @foreach($revision->fotos as $foto)
                                            <div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom:15px;">
                                                <a href="{{ asset($foto->ruta_foto) }}" target="_blank">
                                                    <img src="{{ asset($foto->ruta_foto) }}"
                                                         class="img-responsive img-thumbnail"
                                                         style="max-height:180px; width:100%; object-fit:cover;">
                                                </a>
                                            </div>
                                        @endforeach
                                    </div>
                                @else
                                    <p class="text-muted">Sin fotos.</p>
                                @endif
                            </div>

                            {{-- Firma --}}
                            <div class="col-md-4">
                                <h5>Firma del Cliente</h5>
                                @if($revision->firma_cliente)
                                    <div style="border:1px solid #ddd; padding:10px; background:#fff;">
                                        <img src="{{ asset($revision->firma_cliente) }}"
                                             class="img-responsive"
                                             style="max-height:150px;">
                                    </div>
                                @else
                                    <p class="text-muted">Sin firma.</p>
                                @endif
                            </div>
                        </div>

                        <hr>
                        <p>
                            <strong>Fecha de cierre:</strong> {{ $revision->fecha_cierre ?: '-' }} |
                            <strong>Sincronizado:</strong>
                            @if($revision->sincronizado)
                                <span class="label label-success">Si</span>
                            @else
                                <span class="label label-default">No</span>
                            @endif
                        </p>
                    @endif
                </div>
            </div>

            {{-- ACCIONES --}}
            <div class="panel panel-default">
                <div class="panel-body">
                    <a href="{{ route('revisiones.index') }}" class="btn btn-default">
                        <i class="fa fa-arrow-left"></i> Volver al listado
                    </a>
                    @if($revision->estado_orden == 'PENDIENTE')
                        <form action="{{ route('revisiones.destroy', $revision->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar esta orden de revision?')">
                            {{ csrf_field() }}
                            {{ method_field('DELETE') }}
                            <button type="submit" class="btn btn-danger">
                                <i class="fa fa-trash"></i> Eliminar
                            </button>
                        </form>
                    @endif
                </div>
            </div>

        </div>
    </div>
</div>
@endsection
