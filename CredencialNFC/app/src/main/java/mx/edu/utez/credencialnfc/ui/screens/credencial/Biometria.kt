package mx.edu.utez.credencialnfc.ui.screens.credencial

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

object Biometria {
    private const val AUTENTICADORES = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    enum class Disponibilidad { DISPONIBLE, SIN_BLOQUEO, SIN_HARDWARE, NO_DISPONIBLE }

    fun disponibilidad(context: Context): Disponibilidad {
        val manager = context.getSystemService(BiometricManager::class.java) ?: return Disponibilidad.NO_DISPONIBLE
        return when (manager.canAuthenticate(AUTENTICADORES)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Disponibilidad.DISPONIBLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Disponibilidad.SIN_BLOQUEO
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Disponibilidad.SIN_HARDWARE
            else -> Disponibilidad.NO_DISPONIBLE
        }
    }

    fun autenticar(
        context: Context,
        alExito: () -> Unit,
        alError: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt.Builder(context)
            .setTitle("Verifica tu identidad")
            .setSubtitle("Para activar tu credencial UTEZ")
            .setAllowedAuthenticators(AUTENTICADORES)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(
            CancellationSignal(),
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = alExito()

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    val cancelado = codigo == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                        codigo == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    if (!cancelado) alError(mensaje.toString())
                }
            },
        )
    }
}
