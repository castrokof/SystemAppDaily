{{-- resources/views/macromedidores/edit.blade.php --}}
@extends('layouts.app')

@section('title', 'Editar Macromedidor: ' . $macro->codigo_macro)

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-8 col-md-offset-2">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        <i class="fa fa-pencil"></i> Editar Macromedidor: {{ $macro->codigo_macro }}
                    </h3>
                </div>

                <div class="panel-body">
                    @if($errors->any())
                        <div class="alert alert-danger">
                            <ul style="margin-bottom:0;">
                                @foreach($errors->all() as $error)
                                    <li>{{ $error }}</li>
                                @endforeach
                            </ul>
                        </div>
                    @endif

                    <form method="POST" action="{{ route('macromedidores.update', $macro->id) }}">
                        {{ csrf_field() }}
                        {{ method_field('PUT') }}

                        {{-- CODIGO MACRO --}}
                        <div class="form-group {{ $errors->has('codigo_macro') ? 'has-error' : '' }}">
                            <label for="codigo_macro">Codigo del Macromedidor <span class="text-danger">*</span></label>
                            <input type="text"
                                   class="form-control"
                                   id="codigo_macro"
                                   name="codigo_macro"
                                   value="{{ old('codigo_macro', $macro->codigo_macro) }}"
                                   required
                                   style="text-transform: uppercase;">
                        </div>

                        {{-- UBICACION --}}
                        <div class="form-group">
                            <label for="ubicacion">Ubicacion</label>
                            <textarea class="form-control"
                                      id="ubicacion"
                                      name="ubicacion"
                                      rows="2">{{ old('ubicacion', $macro->ubicacion) }}</textarea>
                        </div>

                        {{-- LECTURA ANTERIOR --}}
                        <div class="form-group">
                            <label for="lectura_anterior">Lectura Anterior</label>
                            <input type="number"
                                   class="form-control"
                                   id="lectura_anterior"
                                   name="lectura_anterior"
                                   value="{{ old('lectura_anterior', $macro->lectura_anterior) }}"
                                   min="0">
                        </div>

                        {{-- USUARIO ASIGNADO --}}
                        <div class="form-group {{ $errors->has('usuario_id') ? 'has-error' : '' }}">
                            <label for="usuario_id">Usuario Asignado <span class="text-danger">*</span></label>
                            <select class="form-control" id="usuario_id" name="usuario_id" required>
                                <option value="">-- Seleccionar usuario --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ old('usuario_id', $macro->usuario_id) == $id ? 'selected' : '' }}>
                                        {{ $nombre }}
                                    </option>
                                @endforeach
                            </select>
                        </div>

                        {{-- INFO DE LECTURA (solo lectura, no editable) --}}
                        @if($macro->estado == 'EJECUTADO')
                            <div class="well">
                                <h4>Datos de Lectura (enviados desde la app)</h4>
                                <p><strong>Lectura Actual:</strong> {{ $macro->lectura_actual }}</p>
                                <p><strong>Observacion:</strong> {{ $macro->observacion ?: '-' }}</p>
                                <p><strong>Fecha:</strong> {{ $macro->fecha_lectura }}</p>
                                <p><strong>GPS:</strong> {{ $macro->gps_latitud_lectura }}, {{ $macro->gps_longitud_lectura }}</p>
                                <p class="text-muted"><em>Estos datos solo se modifican desde la app movil.</em></p>
                            </div>
                        @endif

                        <hr>

                        <button type="submit" class="btn btn-primary">
                            <i class="fa fa-save"></i> Actualizar
                        </button>
                        <a href="{{ route('macromedidores.show', $macro->id) }}" class="btn btn-default">
                            <i class="fa fa-arrow-left"></i> Cancelar
                        </a>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
