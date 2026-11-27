package mx.edu.utez.credencialnfc.ui.screens.perfil

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.credencialnfc.data.local.GestorFotos
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.domain.util.Campo
import mx.edu.utez.credencialnfc.domain.util.Catalogos
import mx.edu.utez.credencialnfc.domain.util.ValidadorRegistro
import mx.edu.utez.credencialnfc.ui.components.BotonPrincipal
import mx.edu.utez.credencialnfc.ui.components.CampoTexto
import mx.edu.utez.credencialnfc.ui.components.MensajeError
import mx.edu.utez.credencialnfc.ui.components.SelectorCarrera
import mx.edu.utez.credencialnfc.ui.components.SelectorEtiquetas
import mx.edu.utez.credencialnfc.ui.components.SelectorFoto

data class EstadoEdicion(
    val original: Estudiante? = null,
    val nombre: String = "",
    val carrera: String = "",
    val fotoUri: String? = null,
    val procesandoFoto: Boolean = false,
    val gustos: List<String> = emptyList(),
    val intereses: List<String> = emptyList(),
    val descripcion: String = "",
    val errores: Map<Campo, String> = emptyMap(),
    val guardando: Boolean = false,
    val mensajeError: String? = null,
    val guardado: Boolean = false,
)

class EditarPerfilViewModel(
    private val estudiantes: EstudianteRepository,
    sesion: SesionRepository,
    private val fotos: GestorFotos,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoEdicion())
    val estado: StateFlow<EstadoEdicion> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val id = sesion.estudianteIdActual.first() ?: return@launch
            val e = estudiantes.observarEstudiante(id).first() ?: return@launch
            _estado.value = EstadoEdicion(
                original = e,
                nombre = e.nombreCompleto,
                carrera = e.carrera,
                fotoUri = e.fotoUri,
                gustos = e.gustos,
                intereses = e.intereses,
                descripcion = e.descripcion,
            )
        }
    }

    fun editar(campo: Campo, cambio: (EstadoEdicion) -> EstadoEdicion) =
        _estado.update { cambio(it).copy(errores = it.errores - campo, mensajeError = null) }

    fun uriParaCamara(): Uri = fotos.crearUriParaCamara()

    fun seleccionarFoto(uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(procesandoFoto = true, mensajeError = null) }
            runCatching { fotos.guardarFoto(uri) }
                .onSuccess { nueva ->
                    val actual = _estado.value
                    if (actual.fotoUri != actual.original?.fotoUri) fotos.eliminar(actual.fotoUri)
                    _estado.update { it.copy(fotoUri = nueva, procesandoFoto = false, errores = it.errores - Campo.FOTO) }
                }
                .onFailure { _estado.update { it.copy(procesandoFoto = false, mensajeError = "No se pudo cargar la foto. Intenta con otra.") } }
        }
    }

    fun guardar() {
        val e = _estado.value
        val original = e.original ?: return
        if (e.guardando || e.procesandoFoto) return
        val errores = buildMap {
            ValidadorRegistro.nombre(e.nombre)?.let { put(Campo.NOMBRE, it) }
            ValidadorRegistro.carrera(e.carrera)?.let { put(Campo.CARRERA, it) }
            ValidadorRegistro.gustos(e.gustos)?.let { put(Campo.GUSTOS, it) }
            ValidadorRegistro.intereses(e.intereses)?.let { put(Campo.INTERESES, it) }
            ValidadorRegistro.descripcion(e.descripcion)?.let { put(Campo.DESCRIPCION, it) }
        }
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores, mensajeError = "Revisa los campos marcados.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            estudiantes.actualizarPerfil(
                original.copy(
                    nombreCompleto = e.nombre,
                    carrera = e.carrera,
                    fotoUri = e.fotoUri,
                    gustos = e.gustos,
                    intereses = e.intereses,
                    descripcion = e.descripcion,
                ),
            ).onSuccess {
                if (e.fotoUri != original.fotoUri) fotos.eliminar(original.fotoUri)
                _estado.update { it.copy(guardando = false, guardado = true) }
            }.onFailure { err ->
                _estado.update { it.copy(guardando = false, mensajeError = err.message) }
            }
        }
    }

    override fun onCleared() {
        val e = _estado.value
        if (!e.guardado && e.fotoUri != e.original?.fotoUri) fotos.eliminar(e.fotoUri)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarPerfil(viewModel: EditarPerfilViewModel, regresar: () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardado) { if (estado.guardado) regresar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
                navigationIcon = { IconButton(onClick = regresar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") } },
            )
        },
    ) { relleno ->
        val original = estado.original
        if (original == null) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectorFoto(
                fotoUri = estado.fotoUri,
                nombre = estado.nombre,
                procesando = estado.procesandoFoto,
                error = estado.errores[Campo.FOTO],
                crearUriCamara = viewModel::uriParaCamara,
                onFotoElegida = viewModel::seleccionarFoto,
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                valor = estado.nombre,
                onCambio = { v -> viewModel.editar(Campo.NOMBRE) { it.copy(nombre = v) } },
                etiqueta = "Nombre completo",
                error = estado.errores[Campo.NOMBRE],
                mayusculas = KeyboardCapitalization.Words,
            )
            CampoTexto(
                valor = original.matricula,
                onCambio = {},
                etiqueta = "Matrícula",
                habilitado = false,
            )
            CampoTexto(
                valor = original.correo,
                onCambio = {},
                etiqueta = "Correo electrónico",
                habilitado = false,
                ayuda = "La matrícula y el correo no se pueden cambiar.",
            )
            SelectorCarrera(
                carrera = estado.carrera,
                opciones = Catalogos.carreras,
                onElegir = { v -> viewModel.editar(Campo.CARRERA) { it.copy(carrera = v) } },
                error = estado.errores[Campo.CARRERA],
            )
            Spacer(Modifier.height(8.dp))
            SelectorEtiquetas(
                titulo = "Gustos",
                sugerencias = Catalogos.sugerenciasGustos,
                seleccionadas = estado.gustos,
                onCambio = { v -> viewModel.editar(Campo.GUSTOS) { it.copy(gustos = v) } },
                error = estado.errores[Campo.GUSTOS],
                maximo = ValidadorRegistro.MAX_ETIQUETAS,
            )
            Spacer(Modifier.height(8.dp))
            SelectorEtiquetas(
                titulo = "Intereses",
                sugerencias = Catalogos.sugerenciasIntereses,
                seleccionadas = estado.intereses,
                onCambio = { v -> viewModel.editar(Campo.INTERESES) { it.copy(intereses = v) } },
                error = estado.errores[Campo.INTERESES],
                maximo = ValidadorRegistro.MAX_ETIQUETAS,
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                valor = estado.descripcion,
                onCambio = { v -> viewModel.editar(Campo.DESCRIPCION) { it.copy(descripcion = v) } },
                etiqueta = "Descripción personal",
                error = estado.errores[Campo.DESCRIPCION],
                ayuda = "${estado.descripcion.length}/${ValidadorRegistro.MAX_DESCRIPCION}",
                mayusculas = KeyboardCapitalization.Sentences,
                unaLinea = false,
                lineasMinimas = 4,
                accionIme = ImeAction.Default,
            )
            estado.mensajeError?.let { MensajeError(it) }
            Spacer(Modifier.height(8.dp))
            if (estado.guardando) {
                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            } else {
                BotonPrincipal("Guardar cambios", viewModel::guardar, habilitado = !estado.procesandoFoto)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
