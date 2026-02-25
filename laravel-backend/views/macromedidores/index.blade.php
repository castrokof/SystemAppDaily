{{-- resources/views/macromedidores/index.blade.php --}}
{{-- Ajusta @extends segun tu layout principal --}}
@extends('layouts.app')

@section('title', 'Macromedidores')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title" style="display:inline-block;">
                        <i class="fa fa-tachometer"></i> Macromedidores
                    </h3>
                    <a href="{{ route('macromedidores.create') }}" class="btn btn-primary btn-sm pull-right">
                        <i class="fa fa-plus"></i> Crear Macromedidor
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
                    <form method="GET" action="{{ route('macromedidores.index') }}" class="form-inline" style="margin-bottom:15px;">
                        <div class="form-group" style="margin-right:10px;">
                            <select name="estado" class="form-control">
                                <option value="">-- Todos los estados --</option>
                                <option value="PENDIENTE" {{ request('estado') == 'PENDIENTE' ? 'selected' : '' }}>Pendiente</option>
                                <option value="EJECUTADO" {{ request('estado') == 'EJECUTADO' ? 'selected' : '' }}>Ejecutado</option>
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <select name="usuario_id" class="form-control">
                                <option value="">-- Todos los usuarios --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ request('usuario_id') == $id ? 'selected' : '' }}>{{ $nombre }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="form-group" style="margin-right:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar codigo o ubicacion..." value="{{ request('buscar') }}">
                        </div>
                        <button type="submit" class="btn btn-default"><i class="fa fa-search"></i> Filtrar</button>
                        <a href="{{ route('macromedidores.index') }}" class="btn btn-default"><i class="fa fa-refresh"></i></a>
                    </form>

                    {{-- CONTADORES --}}
                    <div class="row" style="margin-bottom:15px;">
                        <div class="col-md-3">
                            <div class="well well-sm text-center">
                                <strong>{{ $macros->total() }}</strong> Total
                            </div>
                        </div>
                    </div>

                    {{-- TABLA --}}
                    <div class="table-responsive">
                        <table class="table table-striped table-bordered table-hover">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Codigo</th>
                                    <th>Ubicacion</th>
                                    <th>Lect. Anterior</th>
                                    <th>Estado</th>
                                    <th>Lect. Actual</th>
                                    <th>Fecha Lectura</th>
                                    <th>Fotos</th>
                                    <th>Usuario</th>
                                    <th>Sync</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                @forelse($macros as $macro)
                                    <tr>
                                        <td>{{ $macro->id }}</td>
                                        <td><strong>{{ $macro->codigo_macro }}</strong></td>
                                        <td>{{ $macro->ubicacion ?: '-' }}</td>
                                        <td>{{ $macro->lectura_anterior ?: '0' }}</td>
                                        <td>
                                            @if($macro->estado == 'PENDIENTE')
                                                <span class="label label-warning">PENDIENTE</span>
                                            @else
                                                <span class="label label-success">EJECUTADO</span>
                                            @endif
                                        </td>
                                        <td>{{ $macro->lectura_actual ?: '-' }}</td>
                                        <td>{{ $macro->fecha_lectura ?: '-' }}</td>
                                        <td>{{ $macro->fotos->count() }}</td>
                                        <td>{{ $macro->usuario ? $macro->usuario->nombre : '-' }}</td>
                                        <td>
                                            @if($macro->sincronizado)
                                                <span class="label label-success">Si</span>
                                            @else
                                                <span class="label label-default">No</span>
                                            @endif
                                        </td>
                                        <td>
                                            <a href="{{ route('macromedidores.show', $macro->id) }}" class="btn btn-info btn-xs" title="Ver">
                                                <i class="fa fa-eye"></i>
                                            </a>
                                            <a href="{{ route('macromedidores.edit', $macro->id) }}" class="btn btn-warning btn-xs" title="Editar">
                                                <i class="fa fa-pencil"></i>
                                            </a>
                                            @if($macro->estado == 'PENDIENTE')
                                                <form action="{{ route('macromedidores.destroy', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar este macromedidor?')">
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
                                        <td colspan="11" class="text-center">No hay macromedidores registrados.</td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    {{-- PAGINACION --}}
                    <div class="text-center">
                        {!! $macros->appends(request()->query())->render() !!}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
