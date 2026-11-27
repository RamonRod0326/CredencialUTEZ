package mx.edu.utez.credencialnfc.data.nfc

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.edu.utez.credencialnfc.data.ubicacion.ProveedorUbicacion
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.model.RegistroAcceso
import mx.edu.utez.credencialnfc.domain.model.ResultadoAcceso
import mx.edu.utez.credencialnfc.domain.nfc.FuenteCredencial
import mx.edu.utez.credencialnfc.domain.repository.AccesoRepository

sealed interface EstadoCredencial {
    data object Inactiva : EstadoCredencial
    data class Activa(val expiraEn: Long) : EstadoCredencial
    data class Leyendo(val expiraEn: Long) : EstadoCredencial
    data class Resultado(val permitido: Boolean, val lector: String) : EstadoCredencial
}

class GestorCredencial(
    private val alcance: CoroutineScope,
    private val accesos: AccesoRepository,
    private val ubicacion: ProveedorUbicacion,
) : FuenteCredencial {

    private data class Activacion(val estudianteId: Long, val matricula: String, val expiraEn: Long)

    private val _estado = MutableStateFlow<EstadoCredencial>(EstadoCredencial.Inactiva)
    val estado: StateFlow<EstadoCredencial> = _estado.asStateFlow()

    private val _errorRegistro = MutableStateFlow<String?>(null)
    val errorRegistro: StateFlow<String?> = _errorRegistro.asStateFlow()

    fun descartarErrorRegistro() {
        _errorRegistro.value = null
    }

    @Volatile private var activacion: Activacion? = null
    @Volatile private var ultimaUbicacion: Location? = null
    private var temporizador: Job? = null
    private var limpiarResultado: Job? = null

    fun activar(estudiante: Estudiante, duracionMs: Long = ConfigCredencial.DURACION_ACTIVACION_MS) {
        val expira = System.currentTimeMillis() + duracionMs
        activacion = Activacion(estudiante.id, estudiante.matricula, expira)
        ultimaUbicacion = null
        limpiarResultado?.cancel()
        _estado.value = EstadoCredencial.Activa(expira)

        temporizador?.cancel()
        temporizador = alcance.launch {
            delay(duracionMs)
            desactivar()
        }
        alcance.launch { ultimaUbicacion = ubicacion.obtener() }
    }

    fun desactivar() {
        activacion = null
        temporizador?.cancel()
        if (_estado.value !is EstadoCredencial.Resultado) _estado.value = EstadoCredencial.Inactiva
    }

    fun descartarResultado() {
        if (_estado.value is EstadoCredencial.Resultado) _estado.value = EstadoCredencial.Inactiva
    }

    override fun matriculaActiva(): String? =
        activacion?.takeIf { it.expiraEn > System.currentTimeMillis() }?.matricula

    override fun alSeleccionar() {
        val a = activacion ?: return
        _estado.value = EstadoCredencial.Leyendo(a.expiraEn)
    }

    override fun alRecibirResultado(permitido: Boolean, lector: String) {
        val a = activacion ?: return
        activacion = null
        temporizador?.cancel()

        val (lectorId, lectorNombre) = lector.split(';', limit = 2).let { partes ->
            val id = partes.getOrNull(0)?.trim().orEmpty().ifEmpty { "?" }
            id to (partes.getOrNull(1)?.trim()?.ifEmpty { null } ?: id)
        }
        _estado.value = EstadoCredencial.Resultado(permitido, lectorNombre)

        val momento = System.currentTimeMillis()
        alcance.launch {
            val lugar = ultimaUbicacion ?: ubicacion.obtener(tiempoMaximoMs = 5_000)
            runCatching {
                accesos.registrar(
                RegistroAcceso(
                    estudianteId = a.estudianteId,
                    fechaHora = momento,
                    latitud = lugar?.latitude,
                    longitud = lugar?.longitude,
                    precisionMetros = lugar?.takeIf { it.hasAccuracy() }?.accuracy,
                    lectorId = lectorId,
                    lectorUbicacion = lectorNombre,
                    resultado = if (permitido) ResultadoAcceso.PERMITIDO else ResultadoAcceso.DENEGADO,
                ),
                )
            }.onFailure { _errorRegistro.value = it.message }
        }
        limpiarResultado = alcance.launch {
            delay(6_000)
            descartarResultado()
        }
    }
}
