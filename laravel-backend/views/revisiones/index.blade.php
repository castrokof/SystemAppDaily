{{-- resources/views/revisiones/index.blade.php --}}
@extends('layouts.app')

@section('title', 'Ordenes de Revision')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-clipboard"></i> Ordenes de Revision
                    </h3>
                    <a href="{{ route('revisiones.criticas') }}" class="btn btn-danger btn-sm pull-right">
                        <i class="fa fa-exclamation-triangle"></i> Ver Criticas / Generar Nuevas
                    </a>
                </div>

                <div class="panel-body">
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
                    <form method="GET" action="{{ route('revisiones.index') }}" class="form-inline" style="margin-bottom:15px;">
                        <div class="form-group" style="margin-right:10px;">
                            <select name="estado_orden" class="form-control">
                                <option value="">-- Todos los estados --</option>
                                <option value="PENDIENTE" {{ request('estado_orden') == 'PENDIENTE' ? 'selected' : '' }}>Pendiente</option>
                                <option value="EJECUTADO" {{ request('estado_orden') == 'EJECUTADO' ? 'selected' : '' }}>Ejecutado</option>
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <select name="usuario_id" class="form-control">
                                <option value="">-- Todos los revisores --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ request('usuario_id') == $id ? 'selected' : '' }}>{{ $nombre }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <select name="motivo_revision" class="form-control">
                                <option value="">-- Todos los motivos --</option>
                                <option value="DESVIACION_BAJA" {{ request('motivo_revision') == 'DESVIACION_BAJA' ? 'selected' : '' }}>Desviacion Baja</option>
                                <option value="DESVIACION_ALTA" {{ request('motivo_revision') == 'DESVIACION_ALTA' ? 'selected' : '' }}>Desviacion Alta</option>
                                <option value="OTRO" {{ request('motivo_revision') == 'OTRO' ? 'selected' : '' }}>Otro</option>
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar predio, nombre..." value="{{ request('buscar') }}">
                        </div>
                        <button type="submit" class="btn btn-default"><i class="fa fa-search"></i> Filtrar</button>
                        <a href="{{ route('revisiones.index') }}" class="btn btn-default"><i class="fa fa-refresh"></i></a>
                    </form>

                    {{-- TABLA --}}
                    <div class="table-responsive">
                        <table class="table table-striped table-bordered table-hover">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Predio</th>
                                    <th>Suscriptor</th>
                                    <th>Direccion</th>
                                    <th>Critica</th>
                                    <th>Motivo</th>
                                    <th>Estado</th>
                                    <th>Revisor</th>
                                    <th>Fecha Cierre</th>
                                    <th>Fotos</th>
                                    <th>Sync</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                @forelse($revisiones as $rev)
                                    <tr>
                                        <td>{{ $rev->id }}</td>
                                        <td><strong>{{ $rev->codigo_predio }}</strong></td>
                                        <td>{{ $rev->nombre_suscriptor ?: '-' }}</td>
                                        <td>{{ $rev->direccion ?: '-' }}</td>
                                        <td>
                                            @if($rev->critica_original)
                                                <span class="label label-danger">{{ $rev->critica_original }}</span>
                                            @else
                                                -
                                            @endif
                                        </td>
                                        <td>
                                            @if($rev->motivo_revision)
                                                <span class="label label-info">{{ str_replace('_', ' ', $rev->motivo_revision) }}</span>
                                            @else
                                                -
                                            @endif
                                        </td>
                                        <td>
                                            @if($rev->estado_orden == 'PENDIENTE')
                                                <span class="label label-warning">PENDIENTE</span>
                                            @else
                                                <span class="label label-success">EJECUTADO</span>
                                            @endif
                                        </td>
                                        <td>{{ $rev->usuario ? $rev->usuario->nombre : '-' }}</td>
                                        <td>{{ $rev->fecha_cierre ?: '-' }}</td>
                                        <td>{{ $rev->fotos->count() }}</td>
                                        <td>
                                            @if($rev->sincronizado)
                                                <span class="label label-success">Si</span>
                                            @else
                                                <span class="label label-default">No</span>
                                            @endif
                                        </td>
                                        <td>
                                            <a href="{{ route('revisiones.show', $rev->id) }}" class="btn btn-info btn-xs" title="Ver detalle">
                                                <i class="fa fa-eye"></i>
                                            </a>
                                            @if($rev->estado_orden == 'PENDIENTE')
                                                <form action="{{ route('revisiones.destroy', $rev->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar esta orden?')">
                                                    {{ csrf_field() }}
                                                    {{ method_field('DELETE') }}
                                                    <button type="submit" class="btn btn-danger btn-xs" title="Eliminar">
                                                        <i class="fa fa-trash"></i>
                                                    </button>
                                                </form>
                                            @endif
                                        </td>
                                    </tr>
                                @empty
                                    <tr>
                                        <td colspan="12" class="text-center">No hay ordenes de revision.</td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    <div class="text-center">
                        {!! $revisiones->appends(request()->query())->render() !!}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
