{{-- resources/views/revisiones/index.blade.php --}}
@extends('layouts.app')

@section('title', 'Ordenes de Revision')

@section('content')

<style>
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.modern-card .card-header h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
.filtros-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); margin-bottom: 20px; }
.filtros-container .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 10px 14px; font-size: 0.9rem; transition: all 0.3s ease; }
.filtros-container .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
.table-modern-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.08); overflow-x: auto; }
#tblRevisiones { font-size: 0.85rem; border-radius: 12px; overflow: hidden; }
#tblRevisiones thead th { background: linear-gradient(135deg, #3d57ceff 0%, #776a84ff 100%); color: white; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.5px; padding: 14px 10px; border: none; white-space: nowrap; text-align: center; }
#tblRevisiones tbody td { padding: 12px 10px; vertical-align: middle; border-bottom: 1px solid #f0f0f0; text-align: center; font-size: 0.82rem; }
#tblRevisiones tbody tr { background: white; transition: all 0.2s ease; }
#tblRevisiones tbody tr:hover { background: linear-gradient(90deg, #f8f9ff 0%, #fff 100%); transform: scale(1.005); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1); }
.badge-estado { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge-pendiente { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
.badge-ejecutado { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-sync-si { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
.badge-sync-no { background: #e2e8f0; color: #718096; }
.badge-critica { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.badge-motivo { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
.btn-sm { border-radius: 8px; font-weight: 600; padding: 6px 12px; font-size: 0.8rem; }
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
            <h3><i class="fa fa-clipboard"></i> Ordenes de Revision</h3>
            <a href="{{ route('revisiones.criticas') }}" class="btn" style="background:rgba(255,255,255,0.2); color:white; border:2px solid rgba(255,255,255,0.4); border-radius:10px; padding:8px 18px; font-weight:600;">
                <i class="fa fa-exclamation-triangle"></i> Ver Criticas / Generar Nuevas
            </a>
        </div>
        <div class="card-body">

            {{-- FILTROS --}}
            <div class="filtros-container">
                <form method="GET" action="{{ route('revisiones.index') }}">
                    <div class="row">
                        <div class="col-md-2 col-sm-6" style="margin-bottom:10px;">
                            <select name="estado_orden" class="form-control">
                                <option value="">-- Todos los estados --</option>
                                <option value="PENDIENTE" {{ request('estado_orden') == 'PENDIENTE' ? 'selected' : '' }}>Pendiente</option>
                                <option value="EJECUTADO" {{ request('estado_orden') == 'EJECUTADO' ? 'selected' : '' }}>Ejecutado</option>
                            </select>
                        </div>
                        <div class="col-md-2 col-sm-6" style="margin-bottom:10px;">
                            <select name="usuario_id" class="form-control">
                                <option value="">-- Todos los revisores --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ request('usuario_id') == $id ? 'selected' : '' }}>{{ $nombre }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-2 col-sm-6" style="margin-bottom:10px;">
                            <select name="motivo_revision" class="form-control">
                                <option value="">-- Todos los motivos --</option>
                                <option value="DESVIACION_BAJA" {{ request('motivo_revision') == 'DESVIACION_BAJA' ? 'selected' : '' }}>Desviacion Baja</option>
                                <option value="DESVIACION_ALTA" {{ request('motivo_revision') == 'DESVIACION_ALTA' ? 'selected' : '' }}>Desviacion Alta</option>
                                <option value="OTRO" {{ request('motivo_revision') == 'OTRO' ? 'selected' : '' }}>Otro</option>
                            </select>
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar predio, nombre, direccion..." value="{{ request('buscar') }}">
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <button type="submit" class="btn btn-sm" style="background:linear-gradient(135deg,#667eea,#764ba2); color:white; border:none; border-radius:10px; padding:10px 18px; font-weight:600; margin-right:5px;">
                                <i class="fa fa-search"></i> Filtrar
                            </button>
                            <a href="{{ route('revisiones.index') }}" class="btn btn-sm" style="background:#e2e8f0; color:#4a5568; border:none; border-radius:10px; padding:10px 14px; font-weight:600;">
                                <i class="fa fa-refresh"></i>
                            </a>
                        </div>
                    </div>
                </form>
            </div>

            {{-- TABLA --}}
            <div class="table-modern-container">
                <table id="tblRevisiones" class="table table-hover" style="width:100%;">
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
                                <td><strong style="color:#2d3748;">{{ $rev->codigo_predio }}</strong></td>
                                <td style="text-align:left;">{{ $rev->nombre_suscriptor ?: '-' }}</td>
                                <td style="text-align:left;">{{ $rev->direccion ?: '-' }}</td>
                                <td>
                                    @if($rev->critica_original)
                                        <span class="badge-estado badge-critica">{{ $rev->critica_original }}</span>
                                    @else
                                        -
                                    @endif
                                </td>
                                <td>
                                    @if($rev->motivo_revision)
                                        <span class="badge-estado badge-motivo">{{ str_replace('_', ' ', $rev->motivo_revision) }}</span>
                                    @else
                                        -
                                    @endif
                                </td>
                                <td>
                                    @if($rev->estado_orden == 'PENDIENTE')
                                        <span class="badge-estado badge-pendiente">Pendiente</span>
                                    @else
                                        <span class="badge-estado badge-ejecutado">Ejecutado</span>
                                    @endif
                                </td>
                                <td>{{ $rev->usuario ? $rev->usuario->nombre : '-' }}</td>
                                <td>{{ $rev->fecha_cierre ?: '-' }}</td>
                                <td>{{ $rev->fotos->count() }}</td>
                                <td>
                                    @if($rev->sincronizado)
                                        <span class="badge-estado badge-sync-si">Si</span>
                                    @else
                                        <span class="badge-estado badge-sync-no">No</span>
                                    @endif
                                </td>
                                <td style="white-space:nowrap;">
                                    <a href="{{ route('revisiones.show', $rev->id) }}" class="btn btn-sm" style="background:linear-gradient(135deg,#4facfe,#00f2fe); color:white; border:none;" title="Ver detalle">
                                        <i class="fa fa-eye"></i>
                                    </a>
                                    @if($rev->estado_orden == 'PENDIENTE')
                                        <form action="{{ route('revisiones.destroy', $rev->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar esta orden?')">
                                            {{ csrf_field() }}
                                            {{ method_field('DELETE') }}
                                            <button type="submit" class="btn btn-sm" style="background:linear-gradient(135deg,#eb3349,#f45c43); color:white; border:none;" title="Eliminar">
                                                <i class="fa fa-trash"></i>
                                            </button>
                                        </form>
                                    @endif
                                </td>
                            </tr>
                        @empty
                            <tr>
                                <td colspan="12" style="padding:40px; color:#a0aec0;">
                                    <i class="fa fa-inbox" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                                    No hay ordenes de revision.
                                </td>
                            </tr>
                        @endforelse
                    </tbody>
                </table>
            </div>

            <div class="text-center" style="margin-top:20px;">
                {!! $revisiones->appends(request()->query())->render() !!}
            </div>
        </div>
    </div>
</div>
@endsection
