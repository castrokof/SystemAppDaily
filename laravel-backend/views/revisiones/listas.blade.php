{{-- resources/views/revisiones/listas.blade.php --}}
@extends('layouts.app')

@section('title', 'Listas de Parametros')

@section('content')

<style>
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; }
.modern-card .card-header h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
.filtros-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); margin-bottom: 20px; }
.filtros-container .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 10px 14px; font-size: 0.9rem; transition: all 0.3s ease; }
.filtros-container .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
.table-modern-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.08); overflow-x: auto; }
#tblListas { font-size: 0.85rem; border-radius: 12px; overflow: hidden; }
#tblListas thead th { background: linear-gradient(135deg, #3d57ceff 0%, #776a84ff 100%); color: white; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.5px; padding: 14px 10px; border: none; white-space: nowrap; text-align: center; }
#tblListas tbody td { padding: 12px 10px; vertical-align: middle; border-bottom: 1px solid #f0f0f0; text-align: center; font-size: 0.9rem; }
#tblListas tbody tr { background: white; transition: all 0.2s ease; }
#tblListas tbody tr:hover { background: linear-gradient(90deg, #f8f9ff 0%, #fff 100%); transform: scale(1.005); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1); }
.badge-tipo { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
.badge-activo-si { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-activo-no { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; background: #e2e8f0; color: #718096; }
.nota-seeder { background: linear-gradient(135deg, #e0e7ff 0%, #f0f4ff 100%); border: 2px solid #c7d2fe; border-radius: 12px; padding: 15px 20px; color: #4338ca; font-weight: 500; margin-bottom: 20px; }
.nota-seeder code { background: rgba(67, 56, 202, 0.1); padding: 3px 8px; border-radius: 6px; font-size: 0.85rem; }
.table-modern-container::-webkit-scrollbar { height: 10px; }
.table-modern-container::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 10px; }
.table-modern-container::-webkit-scrollbar-thumb { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 10px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>

<div class="container-fluid">

    {{-- CARD PRINCIPAL --}}
    <div class="modern-card">
        <div class="card-header">
            <h3><i class="fa fa-list-ul"></i> Listas de Parametros</h3>
        </div>
        <div class="card-body">

            <div class="nota-seeder">
                <i class="fa fa-info-circle"></i>
                Estas listas se descargan a la app movil y se usan en los dropdowns del wizard de revisiones.
                Para agregar o modificar valores, ejecuta: <code>php artisan db:seed --class=ListasParametrosSeeder</code>
            </div>

            {{-- FILTRO POR TIPO --}}
            <div class="filtros-container">
                <form method="GET" action="{{ route('listas.index') }}">
                    <div class="row">
                        <div class="col-md-4 col-sm-6" style="margin-bottom:10px;">
                            <select name="tipo_lista" class="form-control">
                                <option value="">-- Todos los tipos --</option>
                                @foreach($tipos as $tipo)
                                    <option value="{{ $tipo }}" {{ request('tipo_lista') == $tipo ? 'selected' : '' }}>{{ $tipo }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-4 col-sm-6" style="margin-bottom:10px;">
                            <button type="submit" class="btn" style="background:linear-gradient(135deg,#667eea,#764ba2); color:white; border:none; border-radius:10px; padding:10px 18px; font-weight:600; margin-right:5px;">
                                <i class="fa fa-search"></i> Filtrar
                            </button>
                            <a href="{{ route('listas.index') }}" class="btn" style="background:#e2e8f0; color:#4a5568; border:none; border-radius:10px; padding:10px 14px; font-weight:600;">
                                <i class="fa fa-refresh"></i>
                            </a>
                        </div>
                    </div>
                </form>
            </div>

            {{-- TABLA --}}
            <div class="table-modern-container">
                <table id="tblListas" class="table table-hover" style="width:100%;">
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
                                <td><span class="badge-tipo">{{ $item->tipo_lista }}</span></td>
                                <td><strong style="color:#2d3748;">{{ $item->codigo }}</strong></td>
                                <td style="text-align:left;">{{ $item->descripcion }}</td>
                                <td>
                                    @if($item->activo)
                                        <span class="badge-activo-si">Si</span>
                                    @else
                                        <span class="badge-activo-no">No</span>
                                    @endif
                                </td>
                            </tr>
                        @empty
                            <tr>
                                <td colspan="5" style="padding:40px; color:#a0aec0;">
                                    <i class="fa fa-inbox" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                                    No hay listas. Ejecuta: <code>php artisan db:seed --class=ListasParametrosSeeder</code>
                                </td>
                            </tr>
                        @endforelse
                    </tbody>
                </table>
            </div>

            <div class="text-center" style="margin-top:20px;">
                {!! $listas->appends(request()->query())->render() !!}
            </div>
        </div>
    </div>
</div>
@endsection
