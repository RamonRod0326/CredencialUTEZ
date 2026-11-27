package mx.edu.utez.credencialnfc.ui.screens.credencial

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import mx.edu.utez.credencialnfc.data.nfc.ConfigCredencial
import mx.edu.utez.credencialnfc.data.nfc.EstadoCredencial
import mx.edu.utez.credencialnfc.data.nfc.GestorCredencial
import mx.edu.utez.credencialnfc.data.ubicacion.ProveedorUbicacion
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.domain.nfc.LectorReferencia
import mx.edu.utez.credencialnfc.domain.nfc.ProcesadorApdu
import mx.edu.utez.credencialnfc.nfc.ServicioCredencialNfc
import mx.edu.utez.credencialnfc.ui.components.BotonSecundario
import mx.edu.utez.credencialnfc.ui.components.MensajeError
import mx.edu.utez.credencialnfc.ui.components.TarjetaCredencial
import mx.edu.utez.credencialnfc.ui.theme.ErrorRojo
import mx.edu.utez.credencialnfc.ui.theme.ExitoVerde
import kotlin.random.Random

class CredencialViewModel(
    private val gestor: GestorCredencial,
    private val ubicacion: ProveedorUbicacion,
) : ViewModel() {
    val estado = gestor.estado
    val errorRegistro = gestor.errorRegistro

    fun descartarErrorRegistro() = gestor.descartarErrorRegistro()

    fun tienePermisoUbicacion() = ubicacion.tienePermiso()
    fun ubicacionActivada() = ubicacion.ubicacionActivada()
    fun activar(estudiante: Estudiante) = gestor.activar(estudiante)
    fun cancelar() = gestor.desactivar()
    fun descartarResultado() = gestor.descartarResultado()

    fun simularLector(estudiante: Estudiante) {
        gestor.activar(estudiante)
        viewModelScope.launch(Dispatchers.Default) {
            delay(1_200)
            val clave = ConfigCredencial.claveLector()
            val telefono = ProcesadorApdu(clave, gestor)
            LectorReferencia(clave, "SIM;Lector simulado") { Random.nextBytes(8) }.leer(telefono::procesar)
        }
    }
}

private enum class EstadoNfc { SIN_NFC, SIN_HCE, APAGADO, LISTO }

private fun revisarNfc(context: Context): EstadoNfc {
    val adaptador = NfcAdapter.getDefaultAdapter(context) ?: return EstadoNfc.SIN_NFC
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return EstadoNfc.SIN_HCE
    return if (adaptador.isEnabled) EstadoNfc.LISTO else EstadoNfc.APAGADO
}

@Composable
fun PestanaCredencial(estudiante: Estudiante, viewModel: CredencialViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val actividad = LocalActivity.current
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val errorRegistro by viewModel.errorRegistro.collectAsStateWithLifecycle()
    val esDepuracion = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    var nfc by remember { mutableStateOf(revisarNfc(context)) }
    var biometria by remember { mutableStateOf(Biometria.disponibilidad(context)) }
    var permisoUbicacion by remember { mutableStateOf(viewModel.tienePermisoUbicacion()) }
    var ubicacionActivada by remember { mutableStateOf(viewModel.ubicacionActivada()) }
    var errorBiometria by remember { mutableStateOf<String?>(null) }

    val pedirPermisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permisoUbicacion = viewModel.tienePermisoUbicacion()
    }

    LifecycleResumeEffect(Unit) {
        nfc = revisarNfc(context)
        biometria = Biometria.disponibilidad(context)
        permisoUbicacion = viewModel.tienePermisoUbicacion()
        ubicacionActivada = viewModel.ubicacionActivada()
        val emulacion = NfcAdapter.getDefaultAdapter(context)
            ?.takeIf { nfc == EstadoNfc.LISTO }
            ?.let { runCatching { CardEmulation.getInstance(it) }.getOrNull() }
        val servicio = ComponentName(context, ServicioCredencialNfc::class.java)
        if (actividad != null) runCatching { emulacion?.setPreferredService(actividad, servicio) }
        onPauseOrDispose {
            if (actividad != null) runCatching { emulacion?.unsetPreferredService(actividad) }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val activa = estado is EstadoCredencial.Activa || estado is EstadoCredencial.Leyendo
        TarjetaCredencial(estudiante, activa = activa)

        errorRegistro?.let {
            MensajeError("El acceso no se guardó en el servidor: $it")
            TextButton(onClick = viewModel::descartarErrorRegistro) { Text("Entendido") }
        }

        AnimatedContent(
            targetState = estado,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "estadoCredencial",
        ) { e ->
            when (e) {
                is EstadoCredencial.Inactiva -> PanelInactiva(
                    nfc = nfc,
                    biometria = biometria,
                    permisoUbicacion = permisoUbicacion,
                    ubicacionActivada = ubicacionActivada,
                    errorBiometria = errorBiometria,
                    esDepuracion = esDepuracion,
                    abrirAjustesNfc = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    abrirAjustesSeguridad = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    abrirAjustesUbicacion = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    pedirUbicacion = {
                        pedirPermisos.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    activar = {
                        errorBiometria = null
                        Biometria.autenticar(
                            context = actividad ?: context,
                            alExito = { viewModel.activar(estudiante) },
                            alError = { errorBiometria = it },
                        )
                    },
                    simular = { viewModel.simularLector(estudiante) },
                )
                is EstadoCredencial.Activa -> PanelActiva(e.expiraEn, leyendo = false, cancelar = viewModel::cancelar)
                is EstadoCredencial.Leyendo -> PanelActiva(e.expiraEn, leyendo = true, cancelar = viewModel::cancelar)
                is EstadoCredencial.Resultado -> PanelResultado(e, viewModel::descartarResultado)
            }
        }
    }
}

@Composable
private fun PanelInactiva(
    nfc: EstadoNfc,
    biometria: Biometria.Disponibilidad,
    permisoUbicacion: Boolean,
    ubicacionActivada: Boolean,
    errorBiometria: String?,
    esDepuracion: Boolean,
    abrirAjustesNfc: () -> Unit,
    abrirAjustesSeguridad: () -> Unit,
    abrirAjustesUbicacion: () -> Unit,
    pedirUbicacion: () -> Unit,
    activar: () -> Unit,
    simular: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Antes de usar tu credencial", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                when (nfc) {
                    EstadoNfc.LISTO -> Requisito(Icons.Outlined.Nfc, "NFC activado", ok = true)
                    EstadoNfc.APAGADO -> Requisito(Icons.Outlined.Nfc, "NFC apagado", ok = false, accion = "Activar", alAccion = abrirAjustesNfc)
                    EstadoNfc.SIN_HCE -> Requisito(Icons.Outlined.Nfc, "Este teléfono no puede emular tarjetas NFC", ok = false)
                    EstadoNfc.SIN_NFC -> Requisito(Icons.Outlined.Nfc, "Este dispositivo no tiene NFC", ok = false)
                }
                when (biometria) {
                    Biometria.Disponibilidad.DISPONIBLE -> Requisito(Icons.Outlined.Fingerprint, "Verificación de identidad lista", ok = true)
                    Biometria.Disponibilidad.SIN_BLOQUEO -> Requisito(
                        Icons.Outlined.Fingerprint, "Registra tu huella o un PIN en el teléfono", ok = false,
                        accion = "Ajustes", alAccion = abrirAjustesSeguridad,
                    )
                    else -> Requisito(Icons.Outlined.Fingerprint, "Biometría no disponible en este dispositivo", ok = false)
                }
                when {
                    !permisoUbicacion -> Requisito(
                        Icons.Outlined.LocationOn, "Permite la ubicación para tu historial", ok = null,
                        accion = "Permitir", alAccion = pedirUbicacion,
                    )
                    !ubicacionActivada -> Requisito(
                        Icons.Outlined.LocationOn, "Ubicación del teléfono apagada", ok = null,
                        accion = "Activar", alAccion = abrirAjustesUbicacion,
                    )
                    else -> Requisito(Icons.Outlined.LocationOn, "Ubicación permitida", ok = true)
                }
            }
        }

        errorBiometria?.let { MensajeError(it) }

        val puedeActivar = nfc == EstadoNfc.LISTO && biometria == Biometria.Disponibilidad.DISPONIBLE
        Button(
            onClick = activar,
            enabled = puedeActivar,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Outlined.Fingerprint, null, Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Verificar y activar credencial", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "Tras verificar tu identidad, tendrás ${ConfigCredencial.DURACION_ACTIVACION_MS / 1000} segundos para acercar tu teléfono al lector.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (esDepuracion) {
            TextButton(onClick = simular, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Science, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simular lectura (solo compilación de prueba)")
            }
        }
    }
}

@Composable
private fun Requisito(icono: ImageVector, texto: String, ok: Boolean?, accion: String? = null, alAccion: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (accion != null) {
            TextButton(onClick = alAccion) { Text(accion) }
        } else {
            Icon(
                when (ok) {
                    true -> Icons.Filled.CheckCircle
                    false -> Icons.Filled.Error
                    null -> Icons.Filled.Warning
                },
                null,
                tint = when (ok) {
                    true -> ExitoVerde
                    false -> ErrorRojo
                    null -> MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.padding(vertical = 12.dp).size(22.dp),
            )
        }
    }
}

@Composable
private fun PanelActiva(expiraEn: Long, leyendo: Boolean, cancelar: () -> Unit) {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiraEn) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(250)
        }
    }
    val segundos = ((expiraEn - ahora).coerceAtLeast(0) + 999) / 1000

    val transicion = rememberInfiniteTransition(label = "pulso")
    val escala by transicion.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "escala",
    )
    val alfa by transicion.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "alfa",
    )
    val verde = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(150.dp).scale(escala).background(verde.copy(alpha = alfa), CircleShape))
            Surface(shape = CircleShape, color = verde, modifier = Modifier.size(104.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Contactless, null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (leyendo) "Leyendo… no retires el teléfono" else "Acerca tu teléfono al lector",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "La credencial se desactiva en $segundos s",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        BotonSecundario("Cancelar", cancelar)
    }
}

@Composable
private fun PanelResultado(resultado: EstadoCredencial.Resultado, cerrar: () -> Unit) {
    val color = if (resultado.permitido) ExitoVerde else ErrorRojo
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (resultado.permitido) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                tint = color,
                modifier = Modifier.size(72.dp),
            )
            Text(
                if (resultado.permitido) "Acceso permitido" else "Acceso denegado",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
            )
            Text(resultado.lector, style = MaterialTheme.typography.titleMedium)
            Text(
                "Quedó registrado en tu historial con fecha, hora y ubicación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = cerrar) { Text("Listo") }
        }
    }
}
