package com.systemapp.daily.utils

object Constants {
    // Límite máximo de lecturas por día por macro (sin autorización)
    const val MAX_LECTURAS_POR_DIA = 2

    // Mínimo de fotos requeridas por lectura
    const val MIN_FOTOS_POR_LECTURA = 2

    // Máximo de fotos permitidas por lectura
    const val MAX_FOTOS_POR_LECTURA = 5

    // Keys para SharedPreferences
    const val PREF_NAME = "system_app_daily_prefs"
    const val PREF_TOKEN = "auth_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_LOGGED_IN = "is_logged_in"

    // Intent extras
    const val EXTRA_MACRO_ID = "extra_macro_id"
    const val EXTRA_MACRO_NOMBRE = "extra_macro_nombre"
    const val EXTRA_MACRO_CODIGO = "extra_macro_codigo"
    const val EXTRA_PHOTO_PATH = "extra_photo_path"

    // Request codes
    const val REQUEST_CAMERA = 1001
    const val REQUEST_CAMERA_PERMISSION = 1002
}
