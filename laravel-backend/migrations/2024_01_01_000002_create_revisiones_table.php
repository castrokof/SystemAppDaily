<?php

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Database\Migrations\Migration;

class CreateRevisionesTable extends Migration
{
    public function up()
    {
        // ========================================
        // TABLA: ordenes_revision
        // Vinculada a ordenescu (lecturas con critica)
        // ========================================
        Schema::create('ordenes_revision', function (Blueprint $table) {
            $table->increments('id');

            // FK a la lectura original (ordenescu)
            $table->integer('lectura_id')->unsigned()->nullable();
            $table->foreign('lectura_id')->references('id')->on('ordenescu')->onDelete('set null');

            // Datos copiados de la lectura para referencia
            $table->string('codigo_predio');          // Suscriptor de ordenescu
            $table->string('nombre_suscriptor')->nullable();  // Nombre + Apell
            $table->string('direccion')->nullable();          // Direccion
            $table->string('telefono')->nullable();           // Telefono
            $table->string('ref_medidor')->nullable();        // Ref_Medidor
            $table->string('critica_original')->nullable();   // Critica de la lectura
            $table->integer('lectura_actual')->nullable();    // Lect_Actual
            $table->integer('lectura_anterior')->nullable();  // LA
            $table->integer('consumo_actual')->nullable();    // Cons_Act
            $table->integer('promedio')->nullable();          // Promedio

            // Estado de la orden
            $table->string('estado_orden')->default('PENDIENTE'); // PENDIENTE | EJECUTADO

            // Datos llenados por el revisor en campo (wizard app)
            $table->string('estado_acometida')->nullable();
            $table->string('estado_sellos')->nullable();
            $table->string('nombre_atiende')->nullable();
            $table->string('tipo_documento')->nullable();
            $table->string('documento')->nullable();
            $table->integer('num_familias')->nullable();
            $table->integer('num_personas')->nullable();
            $table->string('motivo_revision')->nullable();   // DESVIACION_BAJA | DESVIACION_ALTA | OTRO
            $table->text('motivo_detalle')->nullable();
            $table->text('generalidades')->nullable();

            // Firma del cliente
            $table->string('firma_cliente')->nullable();

            // GPS del predio
            $table->double('gps_latitud_predio', 10, 7)->nullable();
            $table->double('gps_longitud_predio', 10, 7)->nullable();

            // Fechas
            $table->timestamp('fecha_cierre')->nullable();

            // Sincronizacion
            $table->boolean('sincronizado')->default(false);

            // Usuario asignado (revisor)
            $table->integer('usuario_id')->unsigned()->nullable();
            $table->foreign('usuario_id')->references('id')->on('users')->onDelete('set null');

            $table->timestamps();
        });

        // ========================================
        // TABLA: censo_hidraulico
        // Puntos hidraulicos encontrados en la revision
        // ========================================
        Schema::create('censo_hidraulico', function (Blueprint $table) {
            $table->increments('id');
            $table->integer('revision_id')->unsigned();
            $table->foreign('revision_id')->references('id')->on('ordenes_revision')->onDelete('cascade');
            $table->string('tipo_punto');  // GRIFO, SANITARIO, DUCHA, etc.
            $table->integer('cantidad');
            $table->string('estado');      // BUENO | MALO
            $table->timestamps();
        });

        // ========================================
        // TABLA: revision_fotos
        // Fotos tomadas durante la revision
        // ========================================
        Schema::create('revision_fotos', function (Blueprint $table) {
            $table->increments('id');
            $table->integer('revision_id')->unsigned();
            $table->foreign('revision_id')->references('id')->on('ordenes_revision')->onDelete('cascade');
            $table->string('ruta_foto');
            $table->timestamps();
        });

        // ========================================
        // TABLA: listas_parametros
        // Listas desplegables para la app movil
        // ========================================
        Schema::create('listas_parametros', function (Blueprint $table) {
            $table->increments('id');
            $table->string('tipo_lista');    // MOTIVOS, ACOMETIDA, SELLOS, TIPO_DOCUMENTO, TIPO_PUNTO, SURTE
            $table->string('codigo');
            $table->string('descripcion');
            $table->boolean('activo')->default(true);
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('revision_fotos');
        Schema::dropIfExists('censo_hidraulico');
        Schema::dropIfExists('ordenes_revision');
        Schema::dropIfExists('listas_parametros');
    }
}
