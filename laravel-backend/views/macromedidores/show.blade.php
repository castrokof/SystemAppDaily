{{-- resources/views/macromedidores/show.blade.php --}}
@extends('layouts.app')

@section('title', 'Macromedidor: ' . $macro->codigo_macro)

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-10 col-md-offset-1">

            {{-- ALERTAS --}}
            @if(session('success'))
                <div class="alert alert-success alert-dismissible">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    {{ session('success') }}
                </div>
            @endif

            {{-- PANEL DATOS BASICOS --}}
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-tachometer"></i> Macromedidor: <strong>{{ $macro->codigo_macro }}</strong>
                    </h3>
                    <div class="pull-right">
                        @if($macro->estado == 'PENDIENTE')
                            <span class="label label-warning" style="font-size:14px;">PENDIENTE</span>
                        @else
                            <span class="label label-success" style="font-size:14px;">EJECUTADO</span>
                        @endif
                    </div>
                </div>

                <div class="panel-body">
                    <div class="row">
                        <div class="col-md-6">
                            <table class="table table-condensed">
                                <tr>
                                    <th style="width:40%;">ID Orden:</th>
                                    <td>{{ $macro->id }}</td>
                                </tr>
                                <tr>
                                    <th>Codigo:</th>
                                    <td><strong>{{ $macro->codigo_macro }}</strong></td>
                                </tr>
                                <tr>
                                    <th>Ubicacion:</th>
                                    <td>{{ $macro->ubicacion ?: 'No especificada' }}</td>
                                </tr>
                                <tr>
                                    <th>Lectura Anterior:</th>
                                    <td><strong>{{ $macro->lectura_anterior ?: 0 }}</strong></td>
                                </tr>
                                <tr>
                                    <th>Usuario Asignado:</th>
                                    <td>{{ $macro->usuario ? $macro->usuario->nombre : 'Sin asignar' }}</td>
                                </tr>
                                <tr>
                                    <th>Creado:</th>
                                    <td>{{ $macro->created_at }}</td>
                                </tr>
                            </table>
                        </div>

                        <div class="col-md-6">
                            <table class="table table-condensed">
                                <tr>
                                    <th style="width:40%;">Lectura Actual:</th>
                                    <td>
                                        @if($macro->lectura_actual)
                                            <strong style="font-size:18px; color:#2ecc71;">{{ $macro->lectura_actual }}</strong>
                                        @else
                                            <span class="text-muted">Sin lectura</span>
                                        @endif
                                    </td>
                                </tr>
                                <tr>
                                    <th>Consumo:</th>
                                    <td>
                                        @if($macro->lectura_actual && $macro->lectura_anterior)
                                            @php $consumo = intval($macro->lectura_actual) - intval($macro->lectura_anterior); @endphp
                                            <strong>{{ $consumo }}</strong>
                                            @if($consumo < 0)
                                                <span class="label label-danger">Negativo</span>
                                            @endif
                                        @else
                                            -
                                        @endif
                                    </td>
                                </tr>
                                <tr>
                                    <th>Observacion:</th>
                                    <td>{{ $macro->observacion ?: '-' }}</td>
                                </tr>
                                <tr>
                                    <th>Fecha Lectura:</th>
                                    <td>{{ $macro->fecha_lectura ?: '-' }}</td>
                                </tr>
                                <tr>
                                    <th>GPS:</th>
                                    <td>
                                        @if($macro->gps_latitud_lectura && $macro->gps_longitud_lectura)
                                            {{ $macro->gps_latitud_lectura }}, {{ $macro->gps_longitud_lectura }}
                                            <a href="https://www.google.com/maps?q={{ $macro->gps_latitud_lectura }},{{ $macro->gps_longitud_lectura }}" target="_blank" class="btn btn-xs btn-default">
                                                <i class="fa fa-map-marker"></i> Ver mapa
                                            </a>
                                        @else
                                            <span class="text-muted">Sin GPS</span>
                                        @endif
                                    </td>
                                </tr>
                                <tr>
                                    <th>Sincronizado:</th>
                                    <td>
                                        @if($macro->sincronizado)
                                            <span class="label label-success">Si</span>
                                        @else
                                            <span class="label label-default">No</span>
                                        @endif
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            {{-- PANEL FOTOS --}}
            @if($macro->fotos->count() > 0)
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        <i class="fa fa-camera"></i> Fotos ({{ $macro->fotos->count() }})
                    </h3>
                </div>
                <div class="panel-body">
                    <div class="row">
                        @foreach($macro->fotos as $foto)
                            <div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom:15px;">
                                <a href="{{ asset($foto->ruta_foto) }}" target="_blank">
                                    <img src="{{ asset($foto->ruta_foto) }}"
                                         class="img-responsive img-thumbnail"
                                         style="max-height:200px; width:100%; object-fit:cover;">
                                </a>
                            </div>
                        @endforeach
                    </div>
                </div>
            </div>
            @endif

            {{-- ACCIONES --}}
            <div class="panel panel-default">
                <div class="panel-body">
                    <a href="{{ route('macromedidores.index') }}" class="btn btn-default">
                        <i class="fa fa-arrow-left"></i> Volver al listado
                    </a>
                    <a href="{{ route('macromedidores.edit', $macro->id) }}" class="btn btn-warning">
                        <i class="fa fa-pencil"></i> Editar
                    </a>

                    @if($macro->estado == 'EJECUTADO')
                        <form action="{{ route('macromedidores.resetear', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Resetear esta orden a PENDIENTE? Se eliminaran las fotos y datos de lectura.')">
                            {{ csrf_field() }}
                            <button type="submit" class="btn btn-danger">
                                <i class="fa fa-undo"></i> Resetear a Pendiente
                            </button>
                        </form>
                    @endif

                    @if($macro->estado == 'PENDIENTE')
                        <form action="{{ route('macromedidores.destroy', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar este macromedidor?')">
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
