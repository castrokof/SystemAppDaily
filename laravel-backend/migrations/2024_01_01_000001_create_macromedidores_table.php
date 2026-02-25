<?php

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Database\Migrations\Migration;

class CreateMacromedidoresTable extends Migration
{
    public function up()
    {
        // ========================================
        // TABLA: macromedidores (ordenes de macro)
        // ========================================
        Schema::create('macromedidores', function (Blueprint $table) {
            $table->increments('id');
            $table->string('codigo_macro')->unique();
            $table->string('ubicacion')->nullable();
            $table->integer('lectura_anterior')->nullable();
            $table->string('estado')->default('PENDIENTE'); // PENDIENTE | EJECUTADO
            $table->string('lectura_actual')->nullable();
            $table->text('observacion')->nullable();
            $table->double('gps_latitud_lectura', 10, 7)->nullable();
            $table->double('gps_longitud_lectura', 10, 7)->nullable();
            $table->timestamp('fecha_lectura')->nullable();
            $table->boolean('sincronizado')->default(false);

            // Usuario asignado
            $table->integer('usuario_id')->unsigned()->nullable();
            $table->foreign('usuario_id')->references('id')->on('users')->onDelete('set null');

            $table->timestamps();
        });

        // ========================================
        // TABLA: macro_fotos (fotos de lecturas)
        // ========================================
        Schema::create('macro_fotos', function (Blueprint $table) {
            $table->increments('id');
            $table->integer('macromedidor_id')->unsigned();
            $table->foreign('macromedidor_id')->references('id')->on('macromedidores')->onDelete('cascade');
            $table->string('ruta_foto');
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('macro_fotos');
        Schema::dropIfExists('macromedidores');
    }
}
