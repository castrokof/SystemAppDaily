package com.systemapp.daily.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GpsLocationManager private constructor(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val PRECISION_MINIMA_METROS = 30f
        private const val TIMEOUT_MS = 15_000L

        @Volatile
        private var INSTANCE: GpsLocationManager? = null

        fun getInstance(context: Context): GpsLocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GpsLocationManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val isAccurate: Boolean
    )

    sealed class GpsResult {
        data class Success(val location: LocationResult) : GpsResult()
        data class LowAccuracy(val location: LocationResult) : GpsResult()
        data class Error(val message: String) : GpsResult()
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): GpsResult {
        if (!hasPermission()) {
            return GpsResult.Error("Permiso de ubicación no concedido")
        }

        if (!isGpsEnabled()) {
            return GpsResult.Error("GPS desactivado. Active la ubicación del dispositivo")
        }

        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000L
            ).setMaxUpdates(1)
                .setDurationMillis(TIMEOUT_MS)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null && continuation.isActive) {
                        val locResult = LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            isAccurate = location.accuracy <= PRECISION_MINIMA_METROS
                        )
                        if (locResult.isAccurate) {
                            continuation.resume(GpsResult.Success(locResult))
                        } else {
                            continuation.resume(GpsResult.LowAccuracy(locResult))
                        }
                    } else if (continuation.isActive) {
                        continuation.resume(GpsResult.Error("No se pudo obtener ubicación"))
                    }
                }
            }

            fusedClient.requestLocationUpdates(
                locationRequest, callback, Looper.getMainLooper()
            )

            continuation.invokeOnCancellation {
                fusedClient.removeLocationUpdates(callback)
            }

            // Timeout fallback: intentar última ubicación conocida
            fusedClient.lastLocation.addOnSuccessListener { lastLocation ->
                if (lastLocation != null && continuation.isActive) {
                    // Solo usar última conocida si no se ha obtenido una nueva
                    // La callback de arriba tiene prioridad
                }
            }
        }
    }

    @Suppress("MissingPermission")
    suspend fun getLastKnownLocation(): GpsResult {
        if (!hasPermission()) {
            return GpsResult.Error("Permiso de ubicación no concedido")
        }

        return suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && continuation.isActive) {
                        val locResult = LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            isAccurate = location.accuracy <= PRECISION_MINIMA_METROS
                        )
                        if (locResult.isAccurate) {
                            continuation.resume(GpsResult.Success(locResult))
                        } else {
                            continuation.resume(GpsResult.LowAccuracy(locResult))
                        }
                    } else if (continuation.isActive) {
                        continuation.resume(GpsResult.Error("No hay ubicación disponible"))
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(GpsResult.Error("Error GPS: ${e.localizedMessage}"))
                    }
                }
        }
    }
}
