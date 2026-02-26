package com.systemapp.daily.data.api

import com.systemapp.daily.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // 🔓 Cliente SIN autenticación (para login y endpoints públicos)
    private fun createRetrofitNoAuth(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 🔐 Cliente CON autenticación (para endpoints protegidos)
    private fun createRetrofitWithAuth(token: String): Retrofit {
        val authInterceptor = AuthInterceptor(token)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ Instancia pública SIN token (para login)
    val apiServiceNoAuth: ApiService by lazy {
        createRetrofitNoAuth().create(ApiService::class.java)
    }

    // ✅ Método para obtener instancia CON token (para endpoints protegidos)
    fun getApiService(token: String): ApiService {
        return createRetrofitWithAuth(token).create(ApiService::class.java)
    }

    object RetrofitClient {
        init {
            // ✅ Log para debug: verifica que la URL está bien definida
            android.util.Log.d("RetrofitClient", "API_BASE_URL: ${BuildConfig.API_BASE_URL}")
        }
        // ...
    }
}