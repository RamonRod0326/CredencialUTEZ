package mx.edu.utez.credencialnfc.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository

private val Context.almacenSesion by preferencesDataStore(name = "sesion")

class SesionDataStore(private val context: Context) : SesionRepository {
    private val claveEstudiante = longPreferencesKey("estudiante_id")
    private val claveToken = stringPreferencesKey("token")

    override val estudianteIdActual: Flow<Long?> =
        context.almacenSesion.data.map { it[claveEstudiante] }

    override suspend fun guardarSesion(estudianteId: Long) {
        context.almacenSesion.edit { it[claveEstudiante] = estudianteId }
    }

    override suspend fun cerrarSesion() {
        context.almacenSesion.edit {
            it.remove(claveEstudiante)
            it.remove(claveToken)
        }
    }

    suspend fun guardarToken(token: String) {
        context.almacenSesion.edit { it[claveToken] = token }
    }

    suspend fun token(): String? = context.almacenSesion.data.first()[claveToken]
}
