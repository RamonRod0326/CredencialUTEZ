package mx.edu.utez.credencialnfc.data.nfc

import mx.edu.utez.credencialnfc.BuildConfig
import mx.edu.utez.credencialnfc.domain.nfc.ProtocoloCredencial

object ConfigCredencial {
    const val DURACION_ACTIVACION_MS = 60_000L

    // se lee de local.properties (credencial.clave); tiene que ser la misma que el secrets.h del lector
    fun claveLector(): ByteArray = ProtocoloCredencial.hexABytes(BuildConfig.CLAVE_LECTOR)

    fun claveConfigurada(): Boolean = BuildConfig.CLAVE_LECTOR.any { it != '0' }
}
