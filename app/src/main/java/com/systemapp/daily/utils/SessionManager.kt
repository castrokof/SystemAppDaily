package com.systemapp.daily.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    var apiToken: String?
        get() = prefs.getString(Constants.PREF_API_TOKEN, null)
        set(value) = prefs.edit().putString(Constants.PREF_API_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(Constants.PREF_USER_ID, -1)
        set(value) = prefs.edit().putInt(Constants.PREF_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(Constants.PREF_USER_NAME, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_NAME, value).apply()

    var userUsuario: String?
        get() = prefs.getString(Constants.PREF_USER_USUARIO, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_USUARIO, value).apply()

    var userEmail: String?
        get() = prefs.getString(Constants.PREF_USER_EMAIL, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_EMAIL, value).apply()

    var userEmpresa: String?
        get() = prefs.getString(Constants.PREF_USER_EMPRESA, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_EMPRESA, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_IS_LOGGED_IN, value).apply()

    var firmaPath: String?
        get() = prefs.getString(Constants.PREF_FIRMA_PATH, null)
        set(value) = prefs.edit().putString(Constants.PREF_FIRMA_PATH, value).apply()

    var printerName: String?
        get() = prefs.getString(Constants.PREF_PRINTER_NAME, null)
        set(value) = prefs.edit().putString(Constants.PREF_PRINTER_NAME, value).apply()

    var printerAddress: String?
        get() = prefs.getString(Constants.PREF_PRINTER_ADDRESS, null)
        set(value) = prefs.edit().putString(Constants.PREF_PRINTER_ADDRESS, value).apply()

    var lastSyncDate: String?
        get() = prefs.getString(Constants.PREF_LAST_SYNC, null)
        set(value) = prefs.edit().putString(Constants.PREF_LAST_SYNC, value).apply()

    fun saveLoginData(
        apiToken: String,
        userId: Int,
        nombre: String,
        usuario: String,
        email: String?,
        empresa: String?
    ) {
        this.apiToken = apiToken
        this.userId = userId
        this.userName = nombre
        this.userUsuario = usuario
        this.userEmail = email
        this.userEmpresa = empresa
        this.isLoggedIn = true
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
