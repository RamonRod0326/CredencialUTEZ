package mx.edu.utez.credencialnfc.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.model.NuevoEstudiante
import mx.edu.utez.credencialnfc.domain.model.RegistroAcceso

interface EstudianteRepository {
    fun observarEstudiante(id: Long): Flow<Estudiante?>
    suspend fun verificarDisponibilidad(matricula: String, correo: String): Result<Unit>
    suspend fun registrar(nuevo: NuevoEstudiante): Result<Estudiante>
    suspend fun iniciarSesion(correoOMatricula: String, password: String): Result<Estudiante>
    suspend fun actualizarPerfil(estudiante: Estudiante): Result<Unit>
}

interface AccesoRepository {
    fun observarHistorial(estudianteId: Long): Flow<List<RegistroAcceso>>
    suspend fun registrar(registro: RegistroAcceso): Long
}

interface SesionRepository {
    val estudianteIdActual: Flow<Long?>
    suspend fun guardarSesion(estudianteId: Long)
    suspend fun cerrarSesion()
}
