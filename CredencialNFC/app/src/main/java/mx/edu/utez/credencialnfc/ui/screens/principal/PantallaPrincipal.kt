package mx.edu.utez.credencialnfc.ui.screens.principal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utez.credencialnfc.data.local.GestorFotos
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.ui.components.BotonPrincipal
import mx.edu.utez.credencialnfc.ui.components.EnConstruccion
import mx.edu.utez.credencialnfc.ui.screens.credencial.CredencialViewModel
import mx.edu.utez.credencialnfc.ui.screens.credencial.PestanaCredencial
import mx.edu.utez.credencialnfc.ui.screens.historial.HistorialViewModel
import mx.edu.utez.credencialnfc.ui.screens.historial.PestanaHistorial
import mx.edu.utez.credencialnfc.ui.viewModelConContenedor

enum class Seccion(val titulo: String, val iconoActivo: ImageVector, val iconoInactivo: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    CREDENCIAL("Credencial", Icons.Filled.Badge, Icons.Outlined.Badge),
    HISTORIAL("Historial", Icons.Filled.History, Icons.Outlined.History),
    PERFIL("Perfil", Icons.Filled.Person, Icons.Outlined.Person),
}

sealed interface EstadoPrincipal {
    data object Cargando : EstadoPrincipal
    data object SinSesion : EstadoPrincipal
    data class Listo(val estudiante: Estudiante) : EstadoPrincipal
    data class Error(val mensaje: String) : EstadoPrincipal
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrincipalViewModel(
    private val sesion: SesionRepository,
    private val estudiantes: EstudianteRepository,
    private val fotos: GestorFotos,
) : ViewModel() {
    private val intento = MutableStateFlow(0)

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    val estado: StateFlow<EstadoPrincipal> = combine(sesion.estudianteIdActual, intento) { id, _ -> id }
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(EstadoPrincipal.SinSesion)
            } else {
                estudiantes.observarEstudiante(id)
                    .map<Estudiante?, EstadoPrincipal> { e ->
                        if (e == null) EstadoPrincipal.SinSesion else EstadoPrincipal.Listo(e)
                    }
                    .onStart { emit(EstadoPrincipal.Cargando) }
                    .catch { emit(EstadoPrincipal.Error(it.message ?: "No se pudo cargar tu perfil.")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPrincipal.Cargando)

    fun reintentar() {
        intento.value++
    }

    fun descartarAviso() {
        _aviso.value = null
    }

    fun cerrarSesion() {
        viewModelScope.launch { sesion.cerrarSesion() }
    }

    fun eliminarFoto(estudiante: Estudiante) {
        val anterior = estudiante.fotoUri ?: return
        viewModelScope.launch {
            estudiantes.actualizarPerfil(estudiante.copy(fotoUri = null))
                .onSuccess { fotos.eliminar(anterior) }
                .onFailure { _aviso.value = it.message }
        }
    }

    fun eliminarDescripcion(estudiante: Estudiante) {
        viewModelScope.launch {
            estudiantes.actualizarPerfil(estudiante.copy(descripcion = ""))
                .onFailure { _aviso.value = it.message }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    viewModel: PrincipalViewModel,
    alCerrarSesion: () -> Unit,
    irAEditarPerfil: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    var seccion by rememberSaveable { mutableStateOf(Seccion.INICIO) }
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(estado) { if (estado is EstadoPrincipal.SinSesion) alCerrarSesion() }
    LaunchedEffect(aviso) {
        aviso?.let {
            avisos.showSnackbar(it)
            viewModel.descartarAviso()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = { Text(if (seccion == Seccion.INICIO) "Credencial UTEZ" else seccion.titulo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Seccion.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == seccion,
                        onClick = { seccion = item },
                        icon = { Icon(if (item == seccion) item.iconoActivo else item.iconoInactivo, null) },
                        label = { Text(item.titulo) },
                    )
                }
            }
        },
    ) { relleno ->
        val mod = Modifier.padding(relleno)
        val actual = estado
        if (actual is EstadoPrincipal.Error) {
            EnConstruccion(
                icono = Icons.Outlined.CloudOff,
                titulo = "Sin conexión",
                descripcion = actual.mensaje,
                modifier = mod,
                accion = { BotonPrincipal("Reintentar", viewModel::reintentar) },
            )
            return@Scaffold
        }
        if (actual !is EstadoPrincipal.Listo) {
            Box(mod.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        when (seccion) {
            Seccion.INICIO -> PestanaInicio(
                estudiante = actual.estudiante,
                irA = { seccion = it },
                modifier = mod,
            )
            Seccion.CREDENCIAL -> PestanaCredencial(
                estudiante = actual.estudiante,
                viewModel = viewModelConContenedor { CredencialViewModel(it.gestorCredencial, it.proveedorUbicacion) },
                modifier = mod,
            )
            Seccion.HISTORIAL -> PestanaHistorial(
                viewModel = viewModelConContenedor { HistorialViewModel(it.sesionRepository, it.accesoRepository) },
                modifier = mod,
            )
            Seccion.PERFIL -> PestanaPerfil(
                estudiante = actual.estudiante,
                editar = irAEditarPerfil,
                cerrarSesion = viewModel::cerrarSesion,
                eliminarFoto = { viewModel.eliminarFoto(actual.estudiante) },
                eliminarDescripcion = { viewModel.eliminarDescripcion(actual.estudiante) },
                modifier = mod,
            )
        }
    }
}
