{{-- resources/views/macromedidores/show.blade.php --}}
@extends('layouts.app')

@section('title', 'Macromedidor: ' . $macro->codigo_macro)

@section('content')

<style>
.modern-card { border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border: none; overflow: hidden; margin-bottom: 25px; background: white; animation: fadeIn 0.5s ease-out; }
.modern-card .card-header { background: linear-gradient(135deg, #2e50e4ff 0%, #2b0c49ff 100%); border: none; padding: 24px; display: flex; justify-content: space-between; align-items: center; }
.modern-card .card-header h3 { color: white; font-weight: 700; font-size: 1.4rem; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.modern-card .card-body { padding: 30px; background: #fafbfc; }
.badge-estado { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge-pendiente { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
.badge-ejecutado { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; }
.badge-sync-si { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
.badge-sync-no { background: #e2e8f0; color: #718096; }
.info-table { width: 100%; border-collapse: separate; border-spacing: 0; }
.info-table tr { transition: background 0.2s ease; }
.info-table tr:hover { background: #f0f4ff; }
.info-table th { width: 40%; padding: 12px 16px; color: #4a5568; font-weight: 600; font-size: 0.9rem; border-bottom: 1px solid #edf2f7; text-transform: uppercase; letter-spacing: 0.3px; font-size: 0.8rem; }
.info-table td { padding: 12px 16px; color: #2d3748; font-size: 0.95rem; border-bottom: 1px solid #edf2f7; }
.valor-lectura { font-size: 1.6rem; font-weight: 700; color: #11998e; }
.foto-container { border-radius: 16px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); transition: all 0.3s ease; }
.foto-container:hover { transform: translateY(-5px); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }
.foto-container img { width: 100%; height: 200px; object-fit: cover; display: block; }
.btn-accion { border-radius: 12px; padding: 10px 24px; font-weight: 600; font-size: 0.9rem; border: none; transition: all 0.3s ease; display: inline-flex; align-items: center; gap: 8px; }
.btn-accion:hover { transform: translateY(-2px); }
.btn-volver { background: #e2e8f0; color: #4a5568; }
.btn-volver:hover { background: #cbd5e0; color: #2d3748; }
.btn-editar { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
.btn-editar:hover { color: white; box-shadow: 0 4px 15px rgba(245,87,108,0.4); }
.btn-resetear { background: linear-gradient(135deg, #f6d365 0%, #fda085 100%); color: white; }
.btn-resetear:hover { color: white; box-shadow: 0 4px 15px rgba(253,160,133,0.4); }
.btn-eliminar { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); color: white; }
.btn-eliminar:hover { color: white; box-shadow: 0 4px 15px rgba(235,51,73,0.4); }
.btn-mapa { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; border-radius: 10px; padding: 6px 14px; font-size: 0.8rem; font-weight: 600; border: none; transition: all 0.3s ease; }
.btn-mapa:hover { color: white; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(79,172,254,0.4); }
.consumo-negativo { color: #f5576c; font-weight: 700; }
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

    {{-- CARD DATOS BASICOS --}}
    <div class="modern-card">
        <div class="card-header">
            <h3><i class="fa fa-tachometer"></i> Macromedidor: {{ $macro->codigo_macro }}</h3>
            @if($macro->estado == 'PENDIENTE')
                <span class="badge-estado badge-pendiente">Pendiente</span>
            @else
                <span class="badge-estado badge-ejecutado">Ejecutado</span>
            @endif
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <table class="info-table">
                        <tr>
                            <th><i class="fa fa-hashtag" style="color:#667eea;"></i> ID Orden</th>
                            <td>{{ $macro->id }}</td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-barcode" style="color:#667eea;"></i> Codigo</th>
                            <td><strong style="color:#2e50e4;">{{ $macro->codigo_macro }}</strong></td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-map-marker" style="color:#667eea;"></i> Ubicacion</th>
                            <td>{{ $macro->ubicacion ?: 'No especificada' }}</td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-dashboard" style="color:#667eea;"></i> Lectura Anterior</th>
                            <td><strong>{{ $macro->lectura_anterior ?: 0 }}</strong></td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-user" style="color:#667eea;"></i> Usuario Asignado</th>
                            <td>{{ $macro->usuario ? $macro->usuario->nombre : 'Sin asignar' }}</td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-calendar" style="color:#667eea;"></i> Creado</th>
                            <td>{{ $macro->created_at }}</td>
                        </tr>
                    </table>
                </div>

                <div class="col-md-6">
                    <table class="info-table">
                        <tr>
                            <th><i class="fa fa-tachometer" style="color:#11998e;"></i> Lectura Actual</th>
                            <td>
                                @if($macro->lectura_actual)
                                    <span class="valor-lectura">{{ $macro->lectura_actual }}</span>
                                @else
                                    <span style="color:#a0aec0;">Sin lectura</span>
                                @endif
                            </td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-line-chart" style="color:#11998e;"></i> Consumo</th>
                            <td>
                                @if($macro->lectura_actual && $macro->lectura_anterior)
                                    @php $consumo = intval($macro->lectura_actual) - intval($macro->lectura_anterior); @endphp
                                    <strong class="{{ $consumo < 0 ? 'consumo-negativo' : '' }}" style="font-size:1.1rem;">{{ $consumo }}</strong>
                                    @if($consumo < 0)
                                        <span class="badge-estado" style="background:linear-gradient(135deg,#eb3349,#f45c43); color:white; font-size:0.65rem; margin-left:5px;">Negativo</span>
                                    @endif
                                @else
                                    -
                                @endif
                            </td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-comment" style="color:#11998e;"></i> Observacion</th>
                            <td>{{ $macro->observacion ?: '-' }}</td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-clock-o" style="color:#11998e;"></i> Fecha Lectura</th>
                            <td>{{ $macro->fecha_lectura ?: '-' }}</td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-map-pin" style="color:#11998e;"></i> GPS</th>
                            <td>
                                @if($macro->gps_latitud_lectura && $macro->gps_longitud_lectura)
                                    {{ $macro->gps_latitud_lectura }}, {{ $macro->gps_longitud_lectura }}
                                    <a href="https://www.google.com/maps?q={{ $macro->gps_latitud_lectura }},{{ $macro->gps_longitud_lectura }}" target="_blank" class="btn-mapa" style="margin-left:8px;">
                                        <i class="fa fa-external-link"></i> Ver mapa
                                    </a>
                                @else
                                    <span style="color:#a0aec0;">Sin GPS</span>
                                @endif
                            </td>
                        </tr>
                        <tr>
                            <th><i class="fa fa-cloud-upload" style="color:#11998e;"></i> Sincronizado</th>
                            <td>
                                @if($macro->sincronizado)
                                    <span class="badge-estado badge-sync-si">Si</span>
                                @else
                                    <span class="badge-estado badge-sync-no">No</span>
                                @endif
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>
    </div>

    {{-- CARD FOTOS --}}
    @if($macro->fotos->count() > 0)
    <div class="modern-card">
        <div class="card-header">
            <h3><i class="fa fa-camera"></i> Fotos ({{ $macro->fotos->count() }})</h3>
        </div>
        <div class="card-body">
            <div class="row">
                @foreach($macro->fotos as $foto)
                    <div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom:20px;">
                        <a href="{{ asset($foto->ruta_foto) }}" target="_blank">
                            <div class="foto-container">
                                <img src="{{ asset($foto->ruta_foto) }}" alt="Foto macromedidor">
                            </div>
                        </a>
                    </div>
                @endforeach
            </div>
        </div>
    </div>
    @endif

    {{-- ACCIONES --}}
    <div style="margin-bottom:30px; animation: fadeIn 0.6s ease-out;">
        <a href="{{ route('macromedidores.index') }}" class="btn-accion btn-volver">
            <i class="fa fa-arrow-left"></i> Volver al listado
        </a>

        @if($macro->estado == 'PENDIENTE')
            <a href="{{ route('macromedidores.edit', $macro->id) }}" class="btn-accion btn-editar">
                <i class="fa fa-pencil"></i> Editar
            </a>
            <form action="{{ route('macromedidores.destroy', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Eliminar este macromedidor?')">
                {{ csrf_field() }}
                {{ method_field('DELETE') }}
                <button type="submit" class="btn-accion btn-eliminar">
                    <i class="fa fa-trash"></i> Eliminar
                </button>
            </form>
        @endif

        @if($macro->estado == 'EJECUTADO')
            <form action="{{ route('macromedidores.resetear', $macro->id) }}" method="POST" style="display:inline;" onsubmit="return confirm('Resetear esta orden a PENDIENTE? Se eliminaran las fotos y datos de lectura.')">
                {{ csrf_field() }}
                <button type="submit" class="btn-accion btn-resetear">
                    <i class="fa fa-undo"></i> Resetear a Pendiente
                </button>
            </form>
        @endif
    </div>
</div>
@endsection
