package mx.edu.utez.credencialnfc.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mx.edu.utez.credencialnfc.BuildConfig
import mx.edu.utez.credencialnfc.data.local.GestorFotos
import mx.edu.utez.credencialnfc.data.local.SesionDataStore
import mx.edu.utez.credencialnfc.data.nfc.GestorCredencial
import mx.edu.utez.credencialnfc.data.remote.AccesoRepositoryRemoto
import mx.edu.utez.credencialnfc.data.remote.ClienteApi
import mx.edu.utez.credencialnfc.data.remote.EstudianteRepositoryRemoto
import mx.edu.utez.credencialnfc.data.remote.SesionConApi
import mx.edu.utez.credencialnfc.data.ubicacion.ProveedorUbicacion
import mx.edu.utez.credencialnfc.domain.repository.AccesoRepository
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository

class ContenedorApp(context: Context) {
    val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sesionLocal by lazy { SesionDataStore(context) }
    private val clienteApi by lazy { ClienteApi(BuildConfig.API_URL, sesionLocal) }

    val gestorFotos: GestorFotos by lazy { GestorFotos(context) }
    val proveedorUbicacion: ProveedorUbicacion by lazy { ProveedorUbicacion(context) }

    val sesionRepository: SesionRepository by lazy { SesionConApi(sesionLocal, clienteApi) }
    val estudianteRepository: EstudianteRepository by lazy {
        EstudianteRepositoryRemoto(clienteApi, sesionLocal, gestorFotos)
    }
    val accesoRepository: AccesoRepository by lazy { AccesoRepositoryRemoto(clienteApi) }

    val gestorCredencial: GestorCredencial by lazy {
        GestorCredencial(alcanceApp, accesoRepository, proveedorUbicacion)
    }
}
