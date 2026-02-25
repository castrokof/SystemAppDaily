{{-- resources/views/revisiones/listas.blade.php --}}
@extends('layouts.app')

@section('title', 'Listas de Parametros')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-md-10 col-md-offset-1">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        <i class="fa fa-list-ul"></i> Listas de Parametros
                    </h3>
                </div>

                <div class="panel-body">
                    <p class="text-muted">
                        Estas listas se descargan a la app movil y se usan en los dropdowns del wizard de revisiones.
                        Para agregar o modificar valores, ejecuta el seeder o edita directamente en la base de datos.
                    </p>

                    {{-- FILTRO POR TIPO --}}
                    <form method="GET" action="{{ route('listas.index') }}" class="form-inline" style="margin-bottom:15px;">
                        <div class="form-group" style="margin-right:10px;">
                            <select name="tipo_lista" class="form-control">
                                <option value="">-- Todos los tipos --</option>
                                @foreach($tipos as $tipo)
                                    <option value="{{ $tipo }}" {{ request('tipo_lista') == $tipo ? 'selected' : '' }}>{{ $tipo }}</option>
                                @endforeach
                            </select>
                        </div>
                        <button type="submit" class="btn btn-default"><i class="fa fa-search"></i> Filtrar</button>
                        <a href="{{ route('listas.index') }}" class="btn btn-default"><i class="fa fa-refresh"></i></a>
                    </form>

                    <div class="table-responsive">
                        <table class="table table-striped table-bordered">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Tipo Lista</th>
                                    <th>Codigo</th>
                                    <th>Descripcion</th>
                                    <th>Activo</th>
                                </tr>
                            </thead>
                            <tbody>
                                @forelse($listas as $item)
                                    <tr>
                                        <td>{{ $item->id }}</td>
                                        <td><span class="label label-primary">{{ $item->tipo_lista }}</span></td>
                                        <td><strong>{{ $item->codigo }}</strong></td>
                                        <td>{{ $item->descripcion }}</td>
                                        <td>
                                            @if($item->activo)
                                                <span class="label label-success">Si</span>
                                            @else
                                                <span class="label label-default">No</span>
                                            @endif
                                        </td>
                                    </tr>
                                @empty
                                    <tr>
                                        <td colspan="5" class="text-center">
                                            No hay listas. Ejecuta: <code>php artisan db:seed --class=ListasParametrosSeeder</code>
                                        </td>
                                    </tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>

                    <div class="text-center">
                        {!! $listas->appends(request()->query())->render() !!}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
