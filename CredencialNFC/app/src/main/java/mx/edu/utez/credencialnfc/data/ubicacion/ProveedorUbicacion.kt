package mx.edu.utez.credencialnfc.data.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class ProveedorUbicacion(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun ubicacionActivada(): Boolean = manager?.isLocationEnabled == true

    @SuppressLint("MissingPermission")
    suspend fun obtener(tiempoMaximoMs: Long = 8_000): Location? {
        val lm = manager ?: return null
        if (!tienePermiso() || !lm.isLocationEnabled) return null
        val proveedor = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            ?: return null

        val actual = withTimeoutOrNull(tiempoMaximoMs) {
            suspendCancellableCoroutine { cont ->
                val cancelar = CancellationSignal()
                cont.invokeOnCancellation { cancelar.cancel() }
                runCatching {
                    lm.getCurrentLocation(proveedor, cancelar, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
        return actual ?: runCatching { lm.getLastKnownLocation(proveedor) }.getOrNull()
    }
}
