{{-- resources/views/macromedidores/create.blade.php --}}
@extends('layouts.app')

@section('title', 'Crear Macromedidor')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-8 col-md-offset-2">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        <i class="fa fa-plus"></i> Crear Macromedidor
                    </h3>
                </div>

                <div class="panel-body">
                    {{-- ERRORES DE VALIDACION --}}
                    @if($errors->any())
                        <div class="alert alert-danger">
                            <ul style="margin-bottom:0;">
                                @foreach($errors->all() as $error)
                                    <li>{{ $error }}</li>
                                @endforeach
                            </ul>
                        </div>
                    @endif

                    <form method="POST" action="{{ route('macromedidores.store') }}">
                        {{ csrf_field() }}

                        {{-- CODIGO MACRO --}}
                        <div class="form-group {{ $errors->has('codigo_macro') ? 'has-error' : '' }}">
                            <label for="codigo_macro">Codigo del Macromedidor <span class="text-danger">*</span></label>
                            <input type="text"
                                   class="form-control"
                                   id="codigo_macro"
                                   name="codigo_macro"
                                   value="{{ old('codigo_macro') }}"
                                   placeholder="Ej: MAC-001"
                                   required
                                   style="text-transform: uppercase;">
                            @if($errors->has('codigo_macro'))
                                <span class="help-block">{{ $errors->first('codigo_macro') }}</span>
                            @endif
                        </div>

                        {{-- UBICACION --}}
                        <div class="form-group {{ $errors->has('ubicacion') ? 'has-error' : '' }}">
                            <label for="ubicacion">Ubicacion</label>
                            <textarea class="form-control"
                                      id="ubicacion"
                                      name="ubicacion"
                                      rows="2"
                                      placeholder="Direccion o descripcion de ubicacion">{{ old('ubicacion') }}</textarea>
                        </div>

                        {{-- LECTURA ANTERIOR --}}
                        <div class="form-group {{ $errors->has('lectura_anterior') ? 'has-error' : '' }}">
                            <label for="lectura_anterior">Lectura Anterior</label>
                            <input type="number"
                                   class="form-control"
                                   id="lectura_anterior"
                                   name="lectura_anterior"
                                   value="{{ old('lectura_anterior', 0) }}"
                                   min="0"
                                   placeholder="0">
                            <span class="help-block">Valor de la ultima lectura registrada del macromedidor.</span>
                        </div>

                        {{-- USUARIO ASIGNADO --}}
                        <div class="form-group {{ $errors->has('usuario_id') ? 'has-error' : '' }}">
                            <label for="usuario_id">Usuario Asignado <span class="text-danger">*</span></label>
                            <select class="form-control" id="usuario_id" name="usuario_id" required>
                                <option value="">-- Seleccionar usuario --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ old('usuario_id') == $id ? 'selected' : '' }}>
                                        {{ $nombre }}
                                    </option>
                                @endforeach
                            </select>
                            @if($errors->has('usuario_id'))
                                <span class="help-block">{{ $errors->first('usuario_id') }}</span>
                            @endif
                        </div>

                        <hr>

                        <div class="form-group">
                            <button type="submit" class="btn btn-primary">
                                <i class="fa fa-save"></i> Guardar Macromedidor
                            </button>
                            <a href="{{ route('macromedidores.index') }}" class="btn btn-default">
                                <i class="fa fa-arrow-left"></i> Cancelar
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
