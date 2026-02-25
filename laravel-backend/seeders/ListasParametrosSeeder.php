<?php

use Illuminate\Database\Seeder;

/**
 * Seeder para listas_parametros.
 *
 * Ejecutar: php artisan db:seed --class=ListasParametrosSeeder
 *
 * Estas listas se descargan a la app Android via GET /api/listasParametros
 * y se usan en los dropdowns del wizard de revisiones.
 */
class ListasParametrosSeeder extends Seeder
{
    public function run()
    {
        $listas = [

            // ========================================
            // MOTIVOS DE REVISION
            // ========================================
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'DESVIACION_BAJA',  'descripcion' => 'Desviacion baja de consumo'],
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'DESVIACION_ALTA',  'descripcion' => 'Desviacion alta de consumo'],
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'FRAUDE',           'descripcion' => 'Posible fraude'],
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'MEDIDOR_DANADO',   'descripcion' => 'Medidor danado'],
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'SIN_LECTURA',      'descripcion' => 'Sin lectura / Impedido'],
            ['tipo_lista' => 'MOTIVOS', 'codigo' => 'OTRO',             'descripcion' => 'Otro motivo'],

            // ========================================
            // ESTADO DE ACOMETIDA
            // ========================================
            ['tipo_lista' => 'ACOMETIDA', 'codigo' => 'BUENA',          'descripcion' => 'Buena - Sin novedad'],
            ['tipo_lista' => 'ACOMETIDA', 'codigo' => 'REGULAR',        'descripcion' => 'Regular - Necesita mantenimiento'],
            ['tipo_lista' => 'ACOMETIDA', 'codigo' => 'MALA',           'descripcion' => 'Mala - Requiere cambio'],
            ['tipo_lista' => 'ACOMETIDA', 'codigo' => 'DIRECTA',        'descripcion' => 'Conexion directa (sin medidor)'],
            ['tipo_lista' => 'ACOMETIDA', 'codigo' => 'NO_VISIBLE',     'descripcion' => 'No visible / Enterrada'],

            // ========================================
            // ESTADO DE SELLOS
            // ========================================
            ['tipo_lista' => 'SELLOS', 'codigo' => 'INTACTOS',          'descripcion' => 'Intactos'],
            ['tipo_lista' => 'SELLOS', 'codigo' => 'ROTOS',             'descripcion' => 'Rotos'],
            ['tipo_lista' => 'SELLOS', 'codigo' => 'SIN_SELLOS',        'descripcion' => 'Sin sellos'],
            ['tipo_lista' => 'SELLOS', 'codigo' => 'MANIPULADOS',       'descripcion' => 'Manipulados'],

            // ========================================
            // TIPO DE DOCUMENTO
            // ========================================
            ['tipo_lista' => 'TIPO_DOCUMENTO', 'codigo' => 'CC',        'descripcion' => 'Cedula de Ciudadania'],
            ['tipo_lista' => 'TIPO_DOCUMENTO', 'codigo' => 'CE',        'descripcion' => 'Cedula de Extranjeria'],
            ['tipo_lista' => 'TIPO_DOCUMENTO', 'codigo' => 'TI',        'descripcion' => 'Tarjeta de Identidad'],
            ['tipo_lista' => 'TIPO_DOCUMENTO', 'codigo' => 'NIT',       'descripcion' => 'NIT'],
            ['tipo_lista' => 'TIPO_DOCUMENTO', 'codigo' => 'PP',        'descripcion' => 'Pasaporte'],

            // ========================================
            // TIPO DE PUNTO HIDRAULICO (censo)
            // ========================================
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'GRIFO',         'descripcion' => 'Grifo / Llave'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'SANITARIO',     'descripcion' => 'Sanitario'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'DUCHA',         'descripcion' => 'Ducha'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'LAVAMANOS',     'descripcion' => 'Lavamanos'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'LAVAPLATOS',    'descripcion' => 'Lavaplatos'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'LAVADERO',      'descripcion' => 'Lavadero'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'TANQUE',        'descripcion' => 'Tanque'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'CALENTADOR',    'descripcion' => 'Calentador'],
            ['tipo_lista' => 'TIPO_PUNTO', 'codigo' => 'OTRO',          'descripcion' => 'Otro punto'],

            // ========================================
            // SURTE (de donde surte el agua)
            // ========================================
            ['tipo_lista' => 'SURTE', 'codigo' => 'ACUEDUCTO',          'descripcion' => 'Acueducto municipal'],
            ['tipo_lista' => 'SURTE', 'codigo' => 'POZO',               'descripcion' => 'Pozo'],
            ['tipo_lista' => 'SURTE', 'codigo' => 'TANQUE_ELEVADO',     'descripcion' => 'Tanque elevado'],
            ['tipo_lista' => 'SURTE', 'codigo' => 'PILA_PUBLICA',       'descripcion' => 'Pila publica'],
            ['tipo_lista' => 'SURTE', 'codigo' => 'OTRO',               'descripcion' => 'Otro'],
        ];

        foreach ($listas as $item) {
            \App\ListaParametro::firstOrCreate(
                ['tipo_lista' => $item['tipo_lista'], 'codigo' => $item['codigo']],
                ['descripcion' => $item['descripcion'], 'activo' => true]
            );
        }
    }
}
