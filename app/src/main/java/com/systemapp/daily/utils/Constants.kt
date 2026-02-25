package com.systemapp.daily.utils

object Constants {
    const val MAX_LECTURAS_POR_DIA = 2
    const val MIN_FOTOS_POR_LECTURA = 2
    const val MAX_FOTOS_POR_LECTURA = 5
    const val GPS_PRECISION_MINIMA = 30f

    // SharedPreferences
    const val PREF_NAME = "system_app_daily_prefs"
    const val PREF_API_TOKEN = "api_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_USUARIO = "user_usuario"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_USER_EMPRESA = "user_empresa"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_FIRMA_PATH = "firma_path"
    const val PREF_PRINTER_NAME = "printer_name"
    const val PREF_PRINTER_ADDRESS = "printer_address"
    const val PREF_LAST_SYNC = "last_sync_date"

    // Intent extras
    const val EXTRA_MACRO_ID = "extra_macro_id"
    const val EXTRA_MACRO_NOMBRE = "extra_macro_nombre"
    const val EXTRA_MACRO_CODIGO = "extra_macro_codigo"
    const val EXTRA_PHOTO_PATH = "extra_photo_path"
    const val EXTRA_ORDEN_ID = "extra_orden_id"

    // Request codes
    const val REQUEST_CAMERA = 1001
    const val REQUEST_CAMERA_PERMISSION = 1002
}
