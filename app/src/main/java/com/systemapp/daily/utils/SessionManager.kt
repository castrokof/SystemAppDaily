package com.systemapp.daily.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Maneja la sesión del usuario (login/logout, token, datos de usuario).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(Constants.PREF_TOKEN, null)
        set(value) = prefs.edit().putString(Constants.PREF_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(Constants.PREF_USER_ID, -1)
        set(value) = prefs.edit().putInt(Constants.PREF_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(Constants.PREF_USER_NAME, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(Constants.PREF_USER_EMAIL, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_EMAIL, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_IS_LOGGED_IN, value).apply()

    fun saveLoginData(token: String, userId: Int, userName: String, email: String?) {
        this.token = token
        this.userId = userId
        this.userName = userName
        this.userEmail = email
        this.isLoggedIn = true
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
