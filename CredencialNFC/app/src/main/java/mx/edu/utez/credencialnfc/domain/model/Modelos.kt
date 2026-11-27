package mx.edu.utez.credencialnfc.domain.model

data class Estudiante(
    val id: Long,
    val nombreCompleto: String,
    val matricula: String,
    val correo: String,
    val carrera: String,
    val fotoUri: String?,
    val gustos: List<String>,
    val intereses: List<String>,
    val descripcion: String,
    val fechaRegistro: Long,
)

data class NuevoEstudiante(
    val nombreCompleto: String,
    val matricula: String,
    val correo: String,
    val password: String,
    val carrera: String,
    val fotoUri: String?,
    val gustos: List<String>,
    val intereses: List<String>,
    val descripcion: String,
)

enum class ResultadoAcceso { PERMITIDO, DENEGADO }

data class RegistroAcceso(
    val id: Long = 0,
    val estudianteId: Long,
    val fechaHora: Long,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Float?,
    val lectorId: String?,
    val lectorUbicacion: String?,
    val resultado: ResultadoAcceso,
)

class ErrorDeDominio(mensaje: String) : Exception(mensaje)
