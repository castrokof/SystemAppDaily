{{-- resources/views/macromedidores/index.blade.php --}}
@extends('layouts.app')

@section('title', 'Macromedidores')

@section('content')

<style>
/* Modal moderno */
.modal-modern .modal-content { border-radius: 20px; border: none; box-shadow: 0 20px 60px rgba(0,0,0,0.3); overflow: hidden; }
.modal-modern .modal-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px 30px; }
.modal-modern .modal-header .modal-title { color: white; font-weight: 700; font-size: 1.4rem; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modal-modern .modal-header .close { color: white; opacity: 0.8; text-shadow: none; font-size: 1.8rem; font-weight: 300; transition: all 0.3s ease; }
.modal-modern .modal-header .close:hover { opacity: 1; transform: rotate(90deg); }
.modal-modern .modal-body { padding: 35px 30px; background: #fafbfc; }
.modal-modern .modal-footer { padding: 20px 30px; border-top: 2px solid #e2e8f0; background: white; }
.modal-modern .form-group { margin-bottom: 24px; }
.modal-modern .form-group label { font-weight: 600; color: #4a5568; font-size: 0.9rem; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; gap: 8px; }
.modal-modern .form-group label i { color: #667eea; font-size: 1.1rem; }
.modal-modern .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 14px 18px; font-size: 1rem; transition: all 0.3s ease; background: white; }
.modal-modern .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
.modal-modern .btn-modal-cancel { border-radius: 12px; padding: 12px 30px; font-size: 0.95rem; font-weight: 600; border: 2px solid #e2e8f0; background: white; color: #718096; transition: all 0.3s ease; }
.modal-modern .btn-modal-cancel:hover { background: #f7fafc; border-color: #cbd5e0; color: #4a5568; transform: translateY(-2px); }
.modal-modern .btn-modal-save { border-radius: 12px; padding: 12px 35px; font-size: 0.95rem; font-weight: 700; border: none; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4); text-transform: uppercase; letter-spacing: 0.5px; }
.modal-modern .btn-modal-save:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5); }
.modal-modern .modal-dialog { max-width: 600px; }
.modal.fade .modal-dialog { transform: scale(0.8); opacity: 0; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.modal.in .modal-dialog { transform: scale(1); opacity: 1; }
.form-divider { height: 2px; background: linear-gradient(90deg, transparent 0%, #e2e8f0 50%, transparent 100%); margin: 25px 0; }
label.requerido::after { content: " *"; color: #f5576c; font-weight: 700; }
/* Card moderno */
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; }
.modern-card .card-header h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
/* Tabla moderna */
.table-modern-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.08); overflow-x: auto; }
#tblMacros { font-size: 0.85rem; border-radius: 12px; overflow: hidden; }
#tblMacros thead th { background: linear-gradient(135deg, #3d57ceff 0%, #776a84ff 100%); color: white; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.5px; padding: 14px 10px; border: none; white-space: nowrap; text-align: center; }
#tblMacros tbody td { padding: 12px 10px; vertical-align: middle; border-bottom: 1px solid #f0f0f0; text-align: center; font-size: 0.82rem; }
#tblMacros tbody tr { background: white; transition: all 0.2s ease; }
#tblMacros tbody tr:hover { background: linear-gradient(90deg, #f8f9ff 0%, #fff 100%); transform: scale(1.005); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1); }
/* Badges */
.badge-estado { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge-pendiente { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
.badge-ejecutado { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-sync-si { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
.badge-sync-no { background: #e2e8f0; color: #718096; }
/* Filtros */
.filtros-container { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); margin-bottom: 20px; }
.filtros-container .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 10px 14px; font-size: 0.9rem; transition: all 0.3s ease; }
.filtros-container .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
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
            <h3><i class="fa fa-tachometer"></i> Macromedidores</h3>
            <button class="btn btn-sm" style="background:rgba(255,255,255,0.2); color:white; border:2px solid rgba(255,255,255,0.4); border-radius:10px; padding:8px 18px; font-weight:600;" onclick="$('#modalCrear').modal('show')">
                <i class="fa fa-plus"></i> Crear Macromedidor
            </button>
        </div>
        <div class="card-body">

            {{-- FILTROS --}}
            <div class="filtros-container">
                <form method="GET" action="{{ route('macromedidores.index') }}">
                    <div class="row">
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <select name="estado" class="form-control">
                                <option value="">-- Todos los estados --</option>
                                <option value="PENDIENTE" {{ request('estado') == 'PENDIENTE' ? 'selected' : '' }}>Pendiente</option>
                                <option value="EJECUTADO" {{ request('estado') == 'EJECUTADO' ? 'selected' : '' }}>Ejecutado</option>
                            </select>
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <select name="usuario_id" class="form-control">
                                <option value="">-- Todos los usuarios --</option>
                                @foreach($usuarios as $id => $nombre)
                                    <option value="{{ $id }}" {{ request('usuario_id') == $id ? 'selected' : '' }}>{{ $nombre }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <input type="text" name="buscar" class="form-control" placeholder="Buscar codigo o ubicacion..." value="{{ request('buscar') }}">
                        </div>
                        <div class="col-md-3 col-sm-6" style="margin-bottom:10px;">
                            <button type="submit" class="btn btn-sm" style="background:linear-gradient(135deg,#667eea,#764ba2); color:white; border:none; border-radius:10px; padding:10px 18px; font-weight:600; margin-right:5px;">
                                <i class="fa fa-search"></i> Filtrar
                            </button>
                            <a href="{{ route('macromedidores.index') }}" class="btn btn-sm" style="background:#e2e8f0; color:#4a5568; border:none; border-radius:10px; padding:10px 14px; font-weight:600;">
                                <i class="fa fa-refresh"></i>
                            </a>
                        </div>
                    </div>
                </form>
            </div>

            {{-- TABLA --}}
            <div class="table-modern-container">
                <table id="tblMacros" class="table table-hover" style="width:100%;">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Codigo</th>
                            <th>Ubicacion</th>
                            <th>L. Anterior</th>
                            <th>Estado</th>
                            <th>L. Actual</th>
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
                                <td><strong style="color:#2d3748;">{{ $macro->codigo_macro }}</strong></td>
                                <td style="text-align:left;">{{ $macro->ubicacion ?: '-' }}</td>
                                <td>{{ $macro->lectura_anterior ?: '0' }}</td>
                                <td>
                                    @if($macro->estado == 'PENDIENTE')
                                        <span class="badge-estado badge-pendiente">Pendiente</span>
                                    @else
                                        <span class="badge-estado badge-ejecutado">Ejecutado</span>
                                    @endif
                                </td>
                                <td><strong>{{ $macro->lectura_actual ?: '-' }}</strong></td>
                                <td>{{ $macro->fecha_lectura ?: '-' }}</td>
                                <td>{{ $macro->fotos->count() }}</td>
                                <td>{{ $macro->usuario ? $macro->usuario->nombre : '-' }}</td>
                                <td>
                                    @if($macro->sincronizado)
                                        <span class="badge-estado badge-sync-si">Si</span>
                                    @else
                                        <span class="badge-estado badge-sync-no">No</span>
                                    @endif
                                </td>
                                <td style="white-space:nowrap;">
                                    <a href="{{ route('macromedidores.show', $macro->id) }}" class="btn btn-sm" style="background:linear-gradient(135deg,#4facfe,#00f2fe); color:white; border:none;" title="Ver">
                                        <i class="fa fa-eye"></i>
                                    </a>
                                    @if($macro->estado == 'PENDIENTE')
                                        <button class="btn btn-sm" style="background:linear-gradient(135deg,#f093fb,#f5576c); color:white; border:none;" title="Editar" onclick="abrirModalEditar({{ $macro->id }}, '{{ $macro->codigo_macro }}', '{{ addslashes($macro->ubicacion) }}', {{ $macro->lectura_anterior ?: 0 }}, {{ $macro->usuario_id ?: 'null' }})">
                                            <i class="fa fa-pencil"></i>
                                        </button>
                                        <form action="{{ route('macromedidores.destroy', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar este macromedidor?')">
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
                                <td colspan="11" style="padding:40px; color:#a0aec0;">
                                    <i class="fa fa-inbox" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                                    No hay macromedidores registrados.
                                </td>
                            </tr>
                        @endforelse
                    </tbody>
                </table>
            </div>

            <div class="text-center" style="margin-top:20px;">
                {!! $macros->appends(request()->query())->render() !!}
            </div>
        </div>
    </div>
</div>

{{-- MODAL CREAR --}}
<div class="modal fade modal-modern" id="modalCrear" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form method="POST" action="{{ route('macromedidores.store') }}">
                {{ csrf_field() }}
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title"><i class="fa fa-plus-circle"></i> Crear Macromedidor</h4>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label class="requerido"><i class="fa fa-barcode"></i> Codigo del Macromedidor</label>
                        <input type="text" class="form-control" name="codigo_macro" placeholder="Ej: MAC-001" required style="text-transform:uppercase;">
                    </div>
                    <div class="form-group">
                        <label><i class="fa fa-map-marker"></i> Ubicacion</label>
                        <textarea class="form-control" name="ubicacion" rows="2" placeholder="Direccion o descripcion"></textarea>
                    </div>
                    <div class="form-divider"></div>
                    <div class="form-group">
                        <label><i class="fa fa-dashboard"></i> Lectura Anterior</label>
                        <input type="number" class="form-control" name="lectura_anterior" value="0" min="0">
                    </div>
                    <div class="form-group">
                        <label class="requerido"><i class="fa fa-user"></i> Usuario Asignado</label>
                        <select class="form-control" name="usuario_id" required>
                            <option value="">-- Seleccionar usuario --</option>
                            @foreach($usuarios as $id => $nombre)
                                <option value="{{ $id }}">{{ $nombre }}</option>
                            @endforeach
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-modal-cancel" data-dismiss="modal"><i class="fa fa-times"></i> Cancelar</button>
                    <button type="submit" class="btn btn-modal-save"><i class="fa fa-save"></i> Guardar</button>
                </div>
            </form>
        </div>
    </div>
</div>

{{-- MODAL EDITAR --}}
<div class="modal fade modal-modern" id="modalEditar" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form method="POST" id="formEditar">
                {{ csrf_field() }}
                {{ method_field('PUT') }}
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title"><i class="fa fa-pencil-square-o"></i> Editar Macromedidor</h4>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label class="requerido"><i class="fa fa-barcode"></i> Codigo del Macromedidor</label>
                        <input type="text" class="form-control" name="codigo_macro" id="editCodigo" required style="text-transform:uppercase;">
                    </div>
                    <div class="form-group">
                        <label><i class="fa fa-map-marker"></i> Ubicacion</label>
                        <textarea class="form-control" name="ubicacion" id="editUbicacion" rows="2"></textarea>
                    </div>
                    <div class="form-divider"></div>
                    <div class="form-group">
                        <label><i class="fa fa-dashboard"></i> Lectura Anterior</label>
                        <input type="number" class="form-control" name="lectura_anterior" id="editLectura" min="0">
                    </div>
                    <div class="form-group">
                        <label class="requerido"><i class="fa fa-user"></i> Usuario Asignado</label>
                        <select class="form-control" name="usuario_id" id="editUsuario" required>
                            <option value="">-- Seleccionar usuario --</option>
                            @foreach($usuarios as $id => $nombre)
                                <option value="{{ $id }}">{{ $nombre }}</option>
                            @endforeach
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-modal-cancel" data-dismiss="modal"><i class="fa fa-times"></i> Cancelar</button>
                    <button type="submit" class="btn btn-modal-save"><i class="fa fa-save"></i> Actualizar</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function abrirModalEditar(id, codigo, ubicacion, lectura, usuarioId) {
    document.getElementById('formEditar').action = '/macromedidores/' + id;
    document.getElementById('editCodigo').value = codigo;
    document.getElementById('editUbicacion').value = ubicacion || '';
    document.getElementById('editLectura').value = lectura;
    if (usuarioId) { document.getElementById('editUsuario').value = usuarioId; }
    $('#modalEditar').modal('show');
}
</script>
@endsection
