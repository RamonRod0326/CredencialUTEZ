package mx.edu.utez.credencialnfc.ui.screens.principal

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.ui.components.AvatarEstudiante
import mx.edu.utez.credencialnfc.ui.components.EtiquetasLectura
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"))

@Composable
fun PestanaPerfil(
    estudiante: Estudiante,
    editar: () -> Unit,
    cerrarSesion: () -> Unit,
    eliminarFoto: () -> Unit,
    eliminarDescripcion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    var confirmarBorrarFoto by rememberSaveable { mutableStateOf(false) }
    var confirmarBorrarDescripcion by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarEstudiante(estudiante.fotoUri, estudiante.nombreCompleto, tamano = 120.dp)
        if (estudiante.fotoUri != null) {
            TextButton(
                onClick = { confirmarBorrarFoto = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Eliminar foto")
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(estudiante.nombreCompleto, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(estudiante.matricula, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        Tarjeta("Información") {
            Dato(Icons.Outlined.School, "Carrera", estudiante.carrera)
            HorizontalDivider()
            Dato(Icons.Outlined.Email, "Correo", estudiante.correo)
            HorizontalDivider()
            Dato(Icons.Outlined.Tag, "Matrícula", estudiante.matricula)
            HorizontalDivider()
            val fecha = Instant.ofEpochMilli(estudiante.fechaRegistro).atZone(ZoneId.systemDefault()).format(formatoFecha)
            Dato(Icons.Outlined.CalendarMonth, "Miembro desde", fecha)
        }

        Tarjeta(
            titulo = "Descripción",
            accion = if (estudiante.descripcion.isNotBlank()) {
                {
                    IconButton(onClick = { confirmarBorrarDescripcion = true }) {
                        Icon(Icons.Outlined.Delete, "Eliminar descripción", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else null,
        ) {
            Text(
                estudiante.descripcion.ifBlank { "Aún no has escrito una descripción." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Tarjeta("Gustos") {
            if (estudiante.gustos.isEmpty()) Text("Sin gustos registrados.", style = MaterialTheme.typography.bodyMedium)
            else EtiquetasLectura(estudiante.gustos)
        }

        Tarjeta("Intereses") {
            if (estudiante.intereses.isEmpty()) Text("Sin intereses registrados.", style = MaterialTheme.typography.bodyMedium)
            else EtiquetasLectura(estudiante.intereses)
        }

        Button(
            onClick = editar,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Editar perfil", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = { confirmarSalida = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que iniciar sesión de nuevo para usar tu credencial.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    cerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }

    if (confirmarBorrarFoto) {
        DialogoEliminar(
            titulo = "¿Eliminar tu foto?",
            texto = "Tu credencial mostrará tus iniciales hasta que agregues otra foto desde Editar perfil.",
            confirmar = {
                confirmarBorrarFoto = false
                eliminarFoto()
            },
            cancelar = { confirmarBorrarFoto = false },
        )
    }

    if (confirmarBorrarDescripcion) {
        DialogoEliminar(
            titulo = "¿Eliminar tu descripción?",
            texto = "Puedes escribir una nueva cuando quieras desde Editar perfil.",
            confirmar = {
                confirmarBorrarDescripcion = false
                eliminarDescripcion()
            },
            cancelar = { confirmarBorrarDescripcion = false },
        )
    }
}

@Composable
private fun DialogoEliminar(titulo: String, texto: String, confirmar: () -> Unit, cancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = cancelar,
        icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(
                onClick = confirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = cancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun Tarjeta(
    titulo: String,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                accion?.invoke()
            }
            contenido()
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
