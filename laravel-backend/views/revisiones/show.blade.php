{{-- resources/views/revisiones/show.blade.php --}}
@extends('layouts.app')

@section('title', 'Revision: ' . $revision->codigo_predio)

@section('content')

<style>
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.modern-card .card-header-danger .card-header, .card-header-danger { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%) !important; }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
.modern-card h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.badge-estado { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge-pendiente { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
.badge-ejecutado { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-sync-si { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
.badge-sync-no { background: #e2e8f0; color: #718096; }
.badge-critica { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.badge-motivo { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
.badge-paso { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 6px 16px; border-radius: 12px; font-size: 0.85rem; font-weight: 700; margin-right: 10px; }
.info-table { width: 100%; border-collapse: separate; border-spacing: 0; }
.info-table tr { transition: background 0.2s ease; }
.info-table tr:hover { background: #f0f4ff; }
.info-table th { width: 40%; padding: 12px 16px; color: #4a5568; font-weight: 600; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.3px; border-bottom: 1px solid #edf2f7; }
.info-table td { padding: 12px 16px; color: #2d3748; font-size: 0.95rem; border-bottom: 1px solid #edf2f7; }
.consumo-negativo { color: #f5576c; font-weight: 700; }
.paso-titulo { font-size: 1.1rem; font-weight: 700; color: #2d3748; margin-bottom: 15px; display: flex; align-items: center; }
.censo-table { width: 100%; border-collapse: separate; border-spacing: 0; border-radius: 12px; overflow: hidden; }
.censo-table thead th { background: linear-gradient(135deg, #3d57ceff 0%, #776a84ff 100%); color: white; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; padding: 12px; text-align: center; border: none; }
.censo-table tbody td { padding: 10px 12px; text-align: center; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.censo-table tbody tr:hover { background: #f0f4ff; }
.badge-bueno { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-malo { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.foto-container { border-radius: 16px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); transition: all 0.3s ease; }
.foto-container:hover { transform: translateY(-5px); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }
.foto-container img { width: 100%; height: 180px; object-fit: cover; display: block; }
.firma-container { border-radius: 16px; border: 2px solid #e2e8f0; padding: 15px; background: white; text-align: center; }
.firma-container img { max-height: 150px; }
.btn-accion { border-radius: 12px; padding: 10px 24px; font-weight: 600; font-size: 0.9rem; border: none; transition: all 0.3s ease; display: inline-flex; align-items: center; gap: 8px; }
.btn-accion:hover { transform: translateY(-2px); }
.btn-volver { background: #e2e8f0; color: #4a5568; }
.btn-volver:hover { background: #cbd5e0; color: #2d3748; }
.btn-eliminar { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.btn-eliminar:hover { color: white; box-shadow: 0 4px 15px rgba(235,51,73,0.4); }
.btn-mapa { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; border-radius: 10px; padding: 6px 14px; font-size: 0.8rem; font-weight: 600; border: none; transition: all 0.3s ease; }
.btn-mapa:hover { color: white; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(79,172,254,0.4); }
.alert-info-modern { background: linear-gradient(135deg, #e0e7ff 0%, #f0f4ff 100%); border: 2px solid #c7d2fe; border-radius: 12px; padding: 15px 20px; color: #4338ca; font-weight: 500; }
.reasignar-form .form-control { border-radius: 12px; border: 2px solid #e2e8f0; padding: 10px 14px; transition: all 0.3s ease; }
.reasignar-form .form-control:focus { border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); outline: none; }
.separator { height: 2px; background: linear-gradient(90deg, transparent 0%, #e2e8f0 50%, transparent 100%); margin: 25px 0; }
.info-cierre { background: white; border-radius: 12px; padding: 15px 20px; display: flex; gap: 30px; align-items: center; flex-wrap: wrap; }
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

    {{-- CARD: LECTURA ORIGINAL (CRITICA) --}}
    <div class="modern-card">
        <div class="card-header" style="background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);">
            <h3><i class="fa fa-exclamation-triangle"></i> Lectura Original (Critica)</h3>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <table class="info-table">
                        <tr><th><i class="fa fa-id-card" style="color:#eb3349;"></i> Predio / Suscriptor</th><td><strong style="color:#2e50e4;">{{ $revision->codigo_predio }}</strong></td></tr>
                        <tr><th><i class="fa fa-user" style="color:#eb3349;"></i> Nombre</th><td>{{ $revision->nombre_suscriptor ?: '-' }}</td></tr>
                        <tr><th><i class="fa fa-map-marker" style="color:#eb3349;"></i> Direccion</th><td>{{ $revision->direccion ?: '-' }}</td></tr>
                        <tr><th><i class="fa fa-phone" style="color:#eb3349;"></i> Telefono</th><td>{{ $revision->telefono ?: '-' }}</td></tr>
                        <tr><th><i class="fa fa-barcode" style="color:#eb3349;"></i> Ref. Medidor</th><td>{{ $revision->ref_medidor ?: '-' }}</td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="info-table">
                        <tr><th><i class="fa fa-arrow-left" style="color:#eb3349;"></i> Lectura Anterior</th><td>{{ $revision->lectura_anterior ?: '-' }}</td></tr>
                        <tr><th><i class="fa fa-arrow-right" style="color:#eb3349;"></i> Lectura Actual</th><td><strong style="font-size:1.1rem;">{{ $revision->lectura_actual ?: '-' }}</strong></td></tr>
                        <tr>
                            <th><i class="fa fa-line-chart" style="color:#eb3349;"></i> Consumo</th>
                            <td>
                                @if($revision->consumo_actual !== null)
                                    <strong class="{{ $revision->consumo_actual < 0 ? 'consumo-negativo' : '' }}" style="font-size:1.1rem;">{{ $revision->consumo_actual }}</strong>
                                @else
                                    -
                                @endif
                            </td>
                        </tr>
                        <tr><th><i class="fa fa-bar-chart" style="color:#eb3349;"></i> Promedio</th><td>{{ $revision->promedio ?: '-' }}</td></tr>
                        <tr><th><i class="fa fa-warning" style="color:#eb3349;"></i> Critica</th><td><span class="badge-estado badge-critica">{{ $revision->critica_original ?: '-' }}</span></td></tr>
                    </table>
                </div>
            </div>
        </div>
    </div>

    {{-- CARD: ORDEN DE REVISION --}}
    <div class="modern-card">
        <div class="card-header">
            <h3><i class="fa fa-clipboard"></i> Orden de Revision #{{ $revision->id }}</h3>
            @if($revision->estado_orden == 'PENDIENTE')
                <span class="badge-estado badge-pendiente">Pendiente</span>
            @else
                <span class="badge-estado badge-ejecutado">Ejecutado</span>
            @endif
        </div>
        <div class="card-body">
            @if($revision->estado_orden == 'PENDIENTE')
                <div class="alert-info-modern">
                    <i class="fa fa-info-circle"></i> Esta orden aun no ha sido ejecutada por el revisor en campo.
                </div>

                <div style="margin-top:20px;">
                    <strong style="color:#4a5568;">Revisor asignado:</strong>
                    <span style="font-size:1.1rem; font-weight:600; color:#2e50e4; margin-left:8px;">{{ $revision->usuario ? $revision->usuario->nombre : 'Sin asignar' }}</span>
                </div>

            @else
                {{-- DATOS LLENADOS POR EL REVISOR EN CAMPO --}}
                <div class="row">
                    {{-- PASO 1: Registro --}}
                    <div class="col-md-6">
                        <div class="paso-titulo">
                            <span class="badge-paso">Paso 1</span> Registro
                        </div>
                        <table class="info-table">
                            <tr><th>Nombre quien atiende</th><td>{{ $revision->nombre_atiende ?: '-' }}</td></tr>
                            <tr><th>Tipo Documento</th><td>{{ $revision->tipo_documento ?: '-' }}</td></tr>
                            <tr><th>Documento</th><td>{{ $revision->documento ?: '-' }}</td></tr>
                            <tr><th>Motivo Revision</th><td><span class="badge-estado badge-motivo">{{ $revision->motivo_revision ? str_replace('_', ' ', $revision->motivo_revision) : '-' }}</span></td></tr>
                            @if($revision->motivo_detalle)
                                <tr><th>Detalle Motivo</th><td>{{ $revision->motivo_detalle }}</td></tr>
                            @endif
                        </table>
                    </div>

                    {{-- PASO 2: Predio --}}
                    <div class="col-md-6">
                        <div class="paso-titulo">
                            <span class="badge-paso">Paso 2</span> Predio
                        </div>
                        <table class="info-table">
                            <tr><th>Estado Acometida</th><td>{{ $revision->estado_acometida ?: '-' }}</td></tr>
                            <tr><th>Estado Sellos</th><td>{{ $revision->estado_sellos ?: '-' }}</td></tr>
                            <tr><th>Generalidades</th><td>{{ $revision->generalidades ?: '-' }}</td></tr>
                            <tr>
                                <th>GPS</th>
                                <td>
                                    @if($revision->gps_latitud_predio && $revision->gps_longitud_predio)
                                        {{ $revision->gps_latitud_predio }}, {{ $revision->gps_longitud_predio }}
                                        <a href="https://www.google.com/maps?q={{ $revision->gps_latitud_predio }},{{ $revision->gps_longitud_predio }}" target="_blank" class="btn-mapa" style="margin-left:8px;">
                                            <i class="fa fa-external-link"></i> Ver mapa
                                        </a>
                                    @else
                                        <span style="color:#a0aec0;">Sin GPS</span>
                                    @endif
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>

                <div class="separator"></div>

                <div class="row">
                    {{-- PASO 3: Familia --}}
                    <div class="col-md-6">
                        <div class="paso-titulo">
                            <span class="badge-paso">Paso 3</span> Familia
                        </div>
                        <table class="info-table">
                            <tr><th>Num. Familias</th><td>{{ $revision->num_familias ?: '-' }}</td></tr>
                            <tr><th>Num. Personas</th><td>{{ $revision->num_personas ?: '-' }}</td></tr>
                        </table>
                    </div>

                    {{-- PASO 4: Censo Hidraulico --}}
                    <div class="col-md-6">
                        <div class="paso-titulo">
                            <span class="badge-paso">Paso 4</span> Censo Hidraulico
                        </div>
                        @if($revision->censoHidraulico->count() > 0)
                            <table class="censo-table">
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
                                            <td><strong>{{ $punto->cantidad }}</strong></td>
                                            <td>
                                                @if($punto->estado == 'BUENO')
                                                    <span class="badge-estado badge-bueno">{{ $punto->estado }}</span>
                                                @else
                                                    <span class="badge-estado badge-malo">{{ $punto->estado }}</span>
                                                @endif
                                            </td>
                                        </tr>
                                    @endforeach
                                </tbody>
                            </table>
                        @else
                            <p style="color:#a0aec0; padding:15px 0;">Sin datos de censo hidraulico.</p>
                        @endif
                    </div>
                </div>

                <div class="separator"></div>

                {{-- PASO 5: Evidencia --}}
                <div class="paso-titulo">
                    <span class="badge-paso">Paso 5</span> Evidencia
                </div>
                <div class="row">
                    {{-- Fotos --}}
                    <div class="col-md-8">
                        <h5 style="color:#4a5568; font-weight:600; margin-bottom:15px;"><i class="fa fa-camera" style="color:#667eea;"></i> Fotos ({{ $revision->fotos->count() }})</h5>
                        @if($revision->fotos->count() > 0)
                            <div class="row">
                                @foreach($revision->fotos as $foto)
                                    <div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom:15px;">
                                        <a href="{{ asset($foto->ruta_foto) }}" target="_blank">
                                            <div class="foto-container">
                                                <img src="{{ asset($foto->ruta_foto) }}" alt="Foto revision">
                                            </div>
                                        </a>
                                    </div>
                                @endforeach
                            </div>
                        @else
                            <p style="color:#a0aec0;">Sin fotos.</p>
                        @endif
                    </div>

                    {{-- Firma --}}
                    <div class="col-md-4">
                        <h5 style="color:#4a5568; font-weight:600; margin-bottom:15px;"><i class="fa fa-pencil-square-o" style="color:#667eea;"></i> Firma del Cliente</h5>
                        @if($revision->firma_cliente)
                            <div class="firma-container">
                                <img src="{{ asset($revision->firma_cliente) }}" class="img-responsive" style="max-height:150px;">
                            </div>
                        @else
                            <p style="color:#a0aec0;">Sin firma.</p>
                        @endif
                    </div>
                </div>

                <div class="separator"></div>

                <div class="info-cierre">
                    <div>
                        <strong style="color:#4a5568;"><i class="fa fa-calendar-check-o" style="color:#667eea;"></i> Fecha de cierre:</strong>
                        <span style="margin-left:5px;">{{ $revision->fecha_cierre ?: '-' }}</span>
                    </div>
                    <div>
                        <strong style="color:#4a5568;"><i class="fa fa-cloud-upload" style="color:#667eea;"></i> Sincronizado:</strong>
                        @if($revision->sincronizado)
                            <span class="badge-estado badge-sync-si" style="margin-left:5px;">Si</span>
                        @else
                            <span class="badge-estado badge-sync-no" style="margin-left:5px;">No</span>
                        @endif
                    </div>
                </div>
            @endif
        </div>
    </div>

    {{-- ACCIONES --}}
    <div style="margin-bottom:30px; animation: fadeIn 0.6s ease-out;">
        <a href="{{ route('revisiones.index') }}" class="btn-accion btn-volver">
            <i class="fa fa-arrow-left"></i> Volver al listado
        </a>
        @if($revision->estado_orden == 'PENDIENTE')
            <form action="{{ route('revisiones.destroy', $revision->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar esta orden de revision?')">
                {{ csrf_field() }}
                {{ method_field('DELETE') }}
                <button type="submit" class="btn-accion btn-eliminar">
                    <i class="fa fa-trash"></i> Eliminar
                </button>
            </form>
        @endif
    </div>
</div>
@endsection
