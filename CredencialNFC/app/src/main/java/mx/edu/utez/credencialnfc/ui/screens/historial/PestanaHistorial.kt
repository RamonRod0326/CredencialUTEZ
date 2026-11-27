package mx.edu.utez.credencialnfc.ui.screens.historial

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import mx.edu.utez.credencialnfc.domain.model.RegistroAcceso
import mx.edu.utez.credencialnfc.domain.model.ResultadoAcceso
import mx.edu.utez.credencialnfc.domain.repository.AccesoRepository
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.ui.components.BotonPrincipal
import mx.edu.utez.credencialnfc.ui.components.EnConstruccion
import mx.edu.utez.credencialnfc.ui.theme.ErrorRojo
import mx.edu.utez.credencialnfc.ui.theme.ExitoVerde
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModel(sesion: SesionRepository, accesos: AccesoRepository) : ViewModel() {
    private val intento = MutableStateFlow(0)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val registros: StateFlow<List<RegistroAcceso>?> = combine(sesion.estudianteIdActual, intento) { id, _ -> id }
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                accesos.observarHistorial(id)
                    .onStart { _error.value = null }
                    .catch {
                        _error.value = it.message ?: "No se pudo cargar el historial."
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun reintentar() {
        intento.value++
    }
}

private val locale = Locale.forLanguageTag("es-MX")
private val formatoDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", locale)

private fun RegistroAcceso.fecha() = Instant.ofEpochMilli(fechaHora).atZone(ZoneId.systemDefault())

private fun tituloDia(dia: LocalDate): String {
    val hoy = LocalDate.now()
    return when (dia) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> dia.format(formatoDia).replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun PestanaHistorial(viewModel: HistorialViewModel, modifier: Modifier = Modifier) {
    val registros by viewModel.registros.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val lista = registros
    val mensajeError = error

    when {
        mensajeError != null -> EnConstruccion(
            Icons.Outlined.CloudOff,
            "No se pudo cargar",
            mensajeError,
            modifier,
            accion = { BotonPrincipal("Reintentar", viewModel::reintentar) },
        )
        lista == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        lista.isEmpty() -> EnConstruccion(
            Icons.Outlined.History,
            "Aún no hay registros",
            "Cada vez que uses tu credencial en un lector, aquí verás la fecha, la hora y el lugar.",
            modifier,
        )
        else -> {
            val porDia = lista.groupBy { it.fecha().toLocalDate() }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Resumen(lista) }
                porDia.forEach { (dia, registrosDia) ->
                    item(key = "dia-$dia") {
                        Text(
                            tituloDia(dia),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(registrosDia, key = { it.id }) { ElementoRegistro(it) }
                }
            }
        }
    }
}

@Composable
private fun Resumen(lista: List<RegistroAcceso>) {
    val permitidos = lista.count { it.resultado == ResultadoAcceso.PERMITIDO }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Cifra("Total", lista.size.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        Cifra("Permitidos", permitidos.toString(), ExitoVerde, Modifier.weight(1f))
        Cifra("Denegados", (lista.size - permitidos).toString(), ErrorRojo, Modifier.weight(1f))
    }
}

@Composable
private fun Cifra(titulo: String, valor: String, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ElementoRegistro(registro: RegistroAcceso) {
    val context = LocalContext.current
    val permitido = registro.resultado == ResultadoAcceso.PERMITIDO
    val color = if (permitido) ExitoVerde else ErrorRojo

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (permitido) Icons.Filled.Check else Icons.Filled.Close, null, tint = color)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        registro.lectorUbicacion ?: registro.lectorId ?: "Lector",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        registro.fecha().format(formatoHora),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (permitido) "Acceso permitido" else "Acceso denegado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
                Spacer(Modifier.size(6.dp))
                val lat = registro.latitud
                val lon = registro.longitud
                if (lat != null && lon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            buildString {
                                append("%.5f, %.5f".format(Locale.US, lat, lon))
                                registro.precisionMetros?.let { append("\n±${it.toInt()} m de precisión") }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val etiqueta = Uri.encode(registro.lectorUbicacion ?: "Acceso UTEZ")
                            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($etiqueta)")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) {
                            Icon(Icons.Outlined.Map, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mapa")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Sin ubicación GPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
