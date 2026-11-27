package mx.edu.utez.credencialnfc.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.credencialnfc.data.local.GestorFotos
import mx.edu.utez.credencialnfc.domain.model.NuevoEstudiante
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.domain.util.Campo
import mx.edu.utez.credencialnfc.domain.util.Catalogos
import mx.edu.utez.credencialnfc.domain.util.ValidadorMatricula
import mx.edu.utez.credencialnfc.domain.util.ValidadorRegistro
import mx.edu.utez.credencialnfc.ui.components.BotonPrincipal
import mx.edu.utez.credencialnfc.ui.components.BotonSecundario
import mx.edu.utez.credencialnfc.ui.components.CampoPassword
import mx.edu.utez.credencialnfc.ui.components.CampoTexto
import mx.edu.utez.credencialnfc.ui.components.MensajeError
import mx.edu.utez.credencialnfc.ui.components.SelectorCarrera
import mx.edu.utez.credencialnfc.ui.components.SelectorEtiquetas
import mx.edu.utez.credencialnfc.ui.components.SelectorFoto

data class EstadoRegistro(
    val paso: Int = 0,
    val nombre: String = "",
    val matricula: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmar: String = "",
    val carrera: String = "",
    val fotoUri: String? = null,
    val procesandoFoto: Boolean = false,
    val gustos: List<String> = emptyList(),
    val intereses: List<String> = emptyList(),
    val descripcion: String = "",
    val errores: Map<Campo, String> = emptyMap(),
    val cargando: Boolean = false,
    val mensajeError: String? = null,
    val completado: Boolean = false,
) {
    companion object {
        const val TOTAL_PASOS = 3
        val TITULOS = listOf("Tu cuenta", "Tu credencial", "Sobre ti")
    }
}

class RegistroViewModel(
    private val estudiantes: EstudianteRepository,
    private val sesion: SesionRepository,
    private val fotos: GestorFotos,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoRegistro())
    val estado: StateFlow<EstadoRegistro> = _estado.asStateFlow()

    fun editar(campo: Campo, cambio: (EstadoRegistro) -> EstadoRegistro) =
        _estado.update { cambio(it).copy(errores = it.errores - campo, mensajeError = null) }

    fun uriParaCamara(): Uri = fotos.crearUriParaCamara()

    fun seleccionarFoto(uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(procesandoFoto = true, mensajeError = null) }
            runCatching { fotos.guardarFoto(uri) }
                .onSuccess { nueva ->
                    fotos.eliminar(_estado.value.fotoUri)
                    _estado.update { it.copy(fotoUri = nueva, procesandoFoto = false, errores = it.errores - Campo.FOTO) }
                }
                .onFailure { _estado.update { it.copy(procesandoFoto = false, mensajeError = "No se pudo cargar la foto. Intenta con otra.") } }
        }
    }

    fun anterior() = _estado.update { it.copy(paso = (it.paso - 1).coerceAtLeast(0), mensajeError = null) }

    fun siguiente() {
        val e = _estado.value
        if (e.cargando || e.procesandoFoto) return
        val errores = validarPaso(e)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        when (e.paso) {
            0 -> viewModelScope.launch {
                _estado.update { it.copy(cargando = true, mensajeError = null) }
                estudiantes.verificarDisponibilidad(e.matricula, e.correo)
                    .onSuccess { _estado.update { it.copy(cargando = false, paso = 1) } }
                    .onFailure { err -> _estado.update { it.copy(cargando = false, mensajeError = err.message) } }
            }
            1 -> _estado.update { it.copy(paso = 2) }
            else -> registrar(e)
        }
    }

    private fun validarPaso(e: EstadoRegistro): Map<Campo, String> = buildMap {
        fun revisar(campo: Campo, error: String?) { if (error != null) put(campo, error) }
        when (e.paso) {
            0 -> {
                revisar(Campo.NOMBRE, ValidadorRegistro.nombre(e.nombre))
                revisar(Campo.MATRICULA, ValidadorRegistro.matricula(e.matricula))
                revisar(Campo.CORREO, ValidadorRegistro.correo(e.correo))
                revisar(Campo.PASSWORD, ValidadorRegistro.password(e.password))
                revisar(Campo.CONFIRMAR, ValidadorRegistro.confirmacion(e.password, e.confirmar))
            }
            1 -> {
                revisar(Campo.FOTO, ValidadorRegistro.foto(e.fotoUri))
                revisar(Campo.CARRERA, ValidadorRegistro.carrera(e.carrera))
            }
            else -> {
                revisar(Campo.GUSTOS, ValidadorRegistro.gustos(e.gustos))
                revisar(Campo.INTERESES, ValidadorRegistro.intereses(e.intereses))
                revisar(Campo.DESCRIPCION, ValidadorRegistro.descripcion(e.descripcion))
            }
        }
    }

    private fun registrar(e: EstadoRegistro) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensajeError = null) }
            estudiantes.registrar(
                NuevoEstudiante(
                    nombreCompleto = e.nombre,
                    matricula = e.matricula,
                    correo = e.correo,
                    password = e.password,
                    carrera = e.carrera,
                    fotoUri = e.fotoUri,
                    gustos = e.gustos,
                    intereses = e.intereses,
                    descripcion = e.descripcion,
                ),
            ).onSuccess { estudiante ->
                sesion.guardarSesion(estudiante.id)
                _estado.update { it.copy(cargando = false, completado = true) }
            }.onFailure { err ->
                _estado.update { it.copy(cargando = false, mensajeError = err.message) }
            }
        }
    }

    override fun onCleared() {
        if (!_estado.value.completado) fotos.eliminar(_estado.value.fotoUri)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    viewModel: RegistroViewModel,
    regresar: () -> Unit,
    alRegistrarse: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.completado) { if (estado.completado) alRegistrarse() }

    val atras = { if (estado.paso > 0) viewModel.anterior() else regresar() }
    BackHandler(enabled = estado.paso > 0) { viewModel.anterior() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = { IconButton(onClick = atras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") } },
            )
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).imePadding()) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                LinearProgressIndicator(
                    progress = { (estado.paso + 1f) / EstadoRegistro.TOTAL_PASOS },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Paso ${estado.paso + 1} de ${EstadoRegistro.TOTAL_PASOS}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(EstadoRegistro.TITULOS[estado.paso], style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when (estado.paso) {
                    0 -> PasoCuenta(estado, viewModel)
                    1 -> PasoCredencial(estado, viewModel)
                    else -> PasoSobreTi(estado, viewModel)
                }
                estado.mensajeError?.let {
                    Spacer(Modifier.height(4.dp))
                    MensajeError(it)
                }
            }
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (estado.paso > 0) {
                        BotonSecundario("Anterior", viewModel::anterior, Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (estado.cargando) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        } else {
                            BotonPrincipal(
                                texto = if (estado.paso == EstadoRegistro.TOTAL_PASOS - 1) "Crear cuenta" else "Siguiente",
                                onClick = viewModel::siguiente,
                                habilitado = !estado.procesandoFoto,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasoCuenta(estado: EstadoRegistro, vm: RegistroViewModel) {
    CampoTexto(
        valor = estado.nombre,
        onCambio = { v -> vm.editar(Campo.NOMBRE) { it.copy(nombre = v) } },
        etiqueta = "Nombre completo",
        error = estado.errores[Campo.NOMBRE],
        mayusculas = KeyboardCapitalization.Words,
    )
    CampoTexto(
        valor = estado.matricula,
        onCambio = { v -> vm.editar(Campo.MATRICULA) { it.copy(matricula = v.uppercase().take(ValidadorMatricula.LONGITUD)) } },
        etiqueta = "Matrícula",
        error = estado.errores[Campo.MATRICULA],
        ayuda = "Ejemplo: 20253DS007",
        mayusculas = KeyboardCapitalization.Characters,
    )
    CampoTexto(
        valor = estado.correo,
        onCambio = { v -> vm.editar(Campo.CORREO) { it.copy(correo = v.trim()) } },
        etiqueta = "Correo electrónico",
        error = estado.errores[Campo.CORREO],
        tipoTeclado = KeyboardType.Email,
    )
    CampoPassword(
        valor = estado.password,
        onCambio = { v -> vm.editar(Campo.PASSWORD) { it.copy(password = v) } },
        etiqueta = "Contraseña",
        error = estado.errores[Campo.PASSWORD],
        ayuda = "Mínimo 8 caracteres, con letras y números",
    )
    CampoPassword(
        valor = estado.confirmar,
        onCambio = { v -> vm.editar(Campo.CONFIRMAR) { it.copy(confirmar = v) } },
        etiqueta = "Confirmar contraseña",
        error = estado.errores[Campo.CONFIRMAR],
        accionIme = ImeAction.Done,
        alConfirmar = vm::siguiente,
    )
}

@Composable
private fun PasoCredencial(estado: EstadoRegistro, vm: RegistroViewModel) {
    Text(
        "Esta foto aparecerá en tu credencial digital. Usa una foto de frente, con buena luz.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    SelectorFoto(
        fotoUri = estado.fotoUri,
        nombre = estado.nombre,
        procesando = estado.procesandoFoto,
        error = estado.errores[Campo.FOTO],
        crearUriCamara = vm::uriParaCamara,
        onFotoElegida = vm::seleccionarFoto,
    )
    Spacer(Modifier.height(16.dp))
    SelectorCarrera(
        carrera = estado.carrera,
        opciones = Catalogos.carreras,
        onElegir = { v -> vm.editar(Campo.CARRERA) { it.copy(carrera = v) } },
        error = estado.errores[Campo.CARRERA],
    )
}

@Composable
private fun PasoSobreTi(estado: EstadoRegistro, vm: RegistroViewModel) {
    SelectorEtiquetas(
        titulo = "Gustos",
        sugerencias = Catalogos.sugerenciasGustos,
        seleccionadas = estado.gustos,
        onCambio = { v -> vm.editar(Campo.GUSTOS) { it.copy(gustos = v) } },
        error = estado.errores[Campo.GUSTOS],
        maximo = ValidadorRegistro.MAX_ETIQUETAS,
    )
    Spacer(Modifier.height(12.dp))
    SelectorEtiquetas(
        titulo = "Intereses",
        sugerencias = Catalogos.sugerenciasIntereses,
        seleccionadas = estado.intereses,
        onCambio = { v -> vm.editar(Campo.INTERESES) { it.copy(intereses = v) } },
        error = estado.errores[Campo.INTERESES],
        maximo = ValidadorRegistro.MAX_ETIQUETAS,
    )
    Spacer(Modifier.height(12.dp))
    CampoTexto(
        valor = estado.descripcion,
        onCambio = { v -> vm.editar(Campo.DESCRIPCION) { it.copy(descripcion = v) } },
        etiqueta = "Descripción personal",
        error = estado.errores[Campo.DESCRIPCION],
        ayuda = "${estado.descripcion.length}/${ValidadorRegistro.MAX_DESCRIPCION} · Opcional",
        mayusculas = KeyboardCapitalization.Sentences,
        unaLinea = false,
        lineasMinimas = 4,
        accionIme = ImeAction.Default,
    )
}
