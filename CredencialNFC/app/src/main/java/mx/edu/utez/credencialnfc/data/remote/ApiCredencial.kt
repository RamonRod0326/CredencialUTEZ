package mx.edu.utez.credencialnfc.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiCredencial {

    @POST("api/auth/verificar")
    suspend fun verificar(@Body cuerpo: VerificarDto): Response<Unit>

    @POST("api/auth/registro")
    suspend fun registrar(@Body cuerpo: RegistroDto): Response<AuthDto>

    @POST("api/auth/login")
    suspend fun iniciarSesion(@Body cuerpo: LoginDto): Response<AuthDto>

    @POST("api/auth/logout")
    suspend fun cerrarSesion(): Response<Unit>

    @GET("api/estudiantes/yo")
    suspend fun perfil(): Response<EstudianteDto>

    @PUT("api/estudiantes/yo")
    suspend fun actualizarPerfil(@Body cuerpo: PerfilDto): Response<EstudianteDto>

    @Multipart
    @PUT("api/estudiantes/yo/foto")
    suspend fun subirFoto(@Part foto: MultipartBody.Part): Response<EstudianteDto>

    @DELETE("api/estudiantes/yo/foto")
    suspend fun eliminarFoto(): Response<EstudianteDto>

    @GET("api/accesos")
    suspend fun accesos(): Response<List<AccesoDto>>

    @POST("api/accesos")
    suspend fun registrarAcceso(@Body cuerpo: NuevoAccesoDto): Response<AccesoDto>

    @DELETE("api/accesos/{id}")
    suspend fun eliminarAcceso(@Path("id") id: Long): Response<Unit>
}

data class VerificarDto(val matricula: String, val correo: String)

data class LoginDto(val usuario: String, val password: String)

data class RegistroDto(
    val nombreCompleto: String,
    val matricula: String,
    val correo: String,
    val password: String,
    val carrera: String,
    val gustos: List<String>,
    val intereses: List<String>,
    val descripcion: String,
)

data class PerfilDto(
    val nombreCompleto: String,
    val carrera: String,
    val gustos: List<String>,
    val intereses: List<String>,
    val descripcion: String,
)

data class EstudianteDto(
    val id: Long,
    val nombreCompleto: String,
    val matricula: String,
    val correo: String,
    val carrera: String,
    val fotoUrl: String?,
    val gustos: List<String>?,
    val intereses: List<String>?,
    val descripcion: String?,
    val fechaRegistro: Long,
)

data class AuthDto(val token: String, val estudiante: EstudianteDto)

data class NuevoAccesoDto(
    val fechaHora: Long,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Float?,
    val lectorId: String?,
    val lectorUbicacion: String?,
    val resultado: String,
)

data class AccesoDto(
    val id: Long,
    val fechaHora: Long,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Float?,
    val lectorId: String?,
    val lectorUbicacion: String?,
    val resultado: String,
)

data class ErrorDto(val estado: Int?, val mensaje: String?)
