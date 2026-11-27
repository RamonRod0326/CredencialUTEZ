package mx.edu.utez.credencialnfc.data.remote

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mx.edu.utez.credencialnfc.data.local.GestorFotos
import mx.edu.utez.credencialnfc.data.local.SesionDataStore
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.model.NuevoEstudiante
import mx.edu.utez.credencialnfc.domain.model.RegistroAcceso
import mx.edu.utez.credencialnfc.domain.model.ResultadoAcceso
import mx.edu.utez.credencialnfc.domain.repository.AccesoRepository
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EstudianteRepositoryRemoto(
    private val cliente: ClienteApi,
    private val sesion: SesionDataStore,
    private val fotos: GestorFotos,
) : EstudianteRepository {

    private val actual = MutableStateFlow<Estudiante?>(null)

    override fun observarEstudiante(id: Long): Flow<Estudiante?> = flow {
        if (actual.value?.id != id) actual.value = null
        try {
            actual.value = cliente.llamar { perfil() }.aDominio()
        } catch (e: SesionExpirada) {
            sesion.cerrarSesion()
            emit(null)
            return@flow
        }
        emitAll(actual.filterNotNull())
    }

    override suspend fun verificarDisponibilidad(matricula: String, correo: String): Result<Unit> = runCatching {
        cliente.llamar { verificar(VerificarDto(matricula.trim(), correo.trim())) }
    }

    override suspend fun registrar(nuevo: NuevoEstudiante): Result<Estudiante> = runCatching {
        val respuesta = cliente.llamar {
            registrar(
                RegistroDto(
                    nombreCompleto = nuevo.nombreCompleto,
                    matricula = nuevo.matricula,
                    correo = nuevo.correo,
                    password = nuevo.password,
                    carrera = nuevo.carrera,
                    gustos = nuevo.gustos,
                    intereses = nuevo.intereses,
                    descripcion = nuevo.descripcion,
                ),
            )
        }
        sesion.guardarToken(respuesta.token)
        var estudiante = respuesta.estudiante.aDominio()
        nuevo.fotoUri?.let { estudiante = subirFoto(it) }
        actual.value = estudiante
        estudiante
    }

    override suspend fun iniciarSesion(correoOMatricula: String, password: String): Result<Estudiante> = runCatching {
        val respuesta = cliente.llamar { iniciarSesion(LoginDto(correoOMatricula.trim(), password)) }
        sesion.guardarToken(respuesta.token)
        respuesta.estudiante.aDominio().also { actual.value = it }
    }

    override suspend fun actualizarPerfil(estudiante: Estudiante): Result<Unit> = runCatching {
        val anterior = actual.value
        var nuevo = cliente.llamar {
            actualizarPerfil(
                PerfilDto(
                    nombreCompleto = estudiante.nombreCompleto,
                    carrera = estudiante.carrera,
                    gustos = estudiante.gustos,
                    intereses = estudiante.intereses,
                    descripcion = estudiante.descripcion,
                ),
            )
        }.aDominio()
        val foto = estudiante.fotoUri
        if (foto != null && foto != anterior?.fotoUri && Uri.parse(foto).scheme == "file") {
            nuevo = subirFoto(foto)
        } else if (foto == null && nuevo.fotoUri != null) {
            nuevo = cliente.llamar { eliminarFoto() }.aDominio()
        }
        actual.value = nuevo
    }

    private suspend fun subirFoto(uriLocal: String): Estudiante {
        val archivo = File(Uri.parse(uriLocal).path ?: error("Foto no válida"))
        val parte = MultipartBody.Part.createFormData(
            "foto",
            archivo.name,
            archivo.asRequestBody("image/jpeg".toMediaType()),
        )
        val estudiante = cliente.llamar { subirFoto(parte) }.aDominio()
        fotos.eliminar(uriLocal)
        return estudiante
    }

    private fun EstudianteDto.aDominio() = Estudiante(
        id = id,
        nombreCompleto = nombreCompleto,
        matricula = matricula,
        correo = correo,
        carrera = carrera,
        fotoUri = cliente.urlCompleta(fotoUrl),
        gustos = gustos.orEmpty(),
        intereses = intereses.orEmpty(),
        descripcion = descripcion.orEmpty(),
        fechaRegistro = fechaRegistro,
    )
}

class AccesoRepositoryRemoto(private val cliente: ClienteApi) : AccesoRepository {

    private val lista = MutableStateFlow<List<RegistroAcceso>?>(null)

    override fun observarHistorial(estudianteId: Long): Flow<List<RegistroAcceso>> = flow {
        lista.value = cliente.llamar { accesos() }.map { it.aDominio(estudianteId) }
        emitAll(lista.filterNotNull())
    }

    override suspend fun registrar(registro: RegistroAcceso): Long {
        val creado = cliente.llamar {
            registrarAcceso(
                NuevoAccesoDto(
                    fechaHora = registro.fechaHora,
                    latitud = registro.latitud,
                    longitud = registro.longitud,
                    precisionMetros = registro.precisionMetros,
                    lectorId = registro.lectorId,
                    lectorUbicacion = registro.lectorUbicacion,
                    resultado = registro.resultado.name,
                ),
            )
        }.aDominio(registro.estudianteId)
        lista.value = listOf(creado) + lista.value.orEmpty()
        return creado.id
    }

    private fun AccesoDto.aDominio(estudianteId: Long) = RegistroAcceso(
        id = id,
        estudianteId = estudianteId,
        fechaHora = fechaHora,
        latitud = latitud,
        longitud = longitud,
        precisionMetros = precisionMetros,
        lectorId = lectorId,
        lectorUbicacion = lectorUbicacion,
        resultado = runCatching { ResultadoAcceso.valueOf(resultado) }.getOrDefault(ResultadoAcceso.DENEGADO),
    )
}

class SesionConApi(
    private val local: SesionDataStore,
    private val cliente: ClienteApi,
) : SesionRepository {
    override val estudianteIdActual: Flow<Long?> = local.estudianteIdActual.map { it }

    override suspend fun guardarSesion(estudianteId: Long) = local.guardarSesion(estudianteId)

    override suspend fun cerrarSesion() {
        runCatching { cliente.llamar { cerrarSesion() } }
        local.cerrarSesion()
    }
}
