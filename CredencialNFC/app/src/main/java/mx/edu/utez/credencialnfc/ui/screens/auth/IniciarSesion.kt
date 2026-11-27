package mx.edu.utez.credencialnfc.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
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
import mx.edu.utez.credencialnfc.domain.repository.EstudianteRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.ui.components.BotonPrincipal
import mx.edu.utez.credencialnfc.ui.components.CampoPassword
import mx.edu.utez.credencialnfc.ui.components.CampoTexto
import mx.edu.utez.credencialnfc.ui.components.EmblemaUtez
import mx.edu.utez.credencialnfc.ui.components.MensajeError
import mx.edu.utez.credencialnfc.ui.theme.AzulUtez
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtez

data class EstadoInicioSesion(
    val usuario: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val completado: Boolean = false,
)

class IniciarSesionViewModel(
    private val estudiantes: EstudianteRepository,
    private val sesion: SesionRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoInicioSesion())
    val estado: StateFlow<EstadoInicioSesion> = _estado.asStateFlow()

    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, error = null) }
    fun cambiarPassword(valor: String) = _estado.update { it.copy(password = valor, error = null) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return
        if (actual.usuario.isBlank() || actual.password.isEmpty()) {
            _estado.update { it.copy(error = "Escribe tu correo o matrícula y tu contraseña.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            estudiantes.iniciarSesion(actual.usuario, actual.password)
                .onSuccess { estudiante ->
                    sesion.guardarSesion(estudiante.id)
                    _estado.update { it.copy(cargando = false, completado = true) }
                }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}

@Composable
fun PantallaIniciarSesion(
    viewModel: IniciarSesionViewModel,
    regresar: () -> Unit,
    irARegistro: () -> Unit,
    alIniciarSesion: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.completado) { if (estado.completado) alIniciarSesion() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to VerdeUtez, 0.45f to AzulUtez, 0.45f to MaterialTheme.colorScheme.background)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            IconButton(onClick = regresar, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmblemaUtez(tamano = 84.dp)
                Spacer(Modifier.height(16.dp))
                Text("¡Hola de nuevo!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    "Inicia sesión para usar tu credencial",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(28.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CampoTexto(
                            valor = estado.usuario,
                            onCambio = viewModel::cambiarUsuario,
                            etiqueta = "Correo o matrícula",
                            tipoTeclado = KeyboardType.Email,
                        )
                        CampoPassword(
                            valor = estado.password,
                            onCambio = viewModel::cambiarPassword,
                            etiqueta = "Contraseña",
                            accionIme = ImeAction.Done,
                            alConfirmar = viewModel::iniciarSesion,
                        )
                        estado.error?.let { MensajeError(it) }
                        Spacer(Modifier.height(6.dp))
                        if (estado.cargando) {
                            Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp))
                            }
                        } else {
                            BotonPrincipal("Iniciar sesión", viewModel::iniciarSesion)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = irARegistro) { Text("Crear cuenta") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
