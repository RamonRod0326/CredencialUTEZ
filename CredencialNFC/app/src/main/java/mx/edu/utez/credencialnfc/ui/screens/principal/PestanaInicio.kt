package mx.edu.utez.credencialnfc.ui.screens.principal

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.ui.components.AvatarEstudiante
import mx.edu.utez.credencialnfc.ui.components.EtiquetasLectura
import mx.edu.utez.credencialnfc.ui.theme.AzulUtez
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtez

@Composable
fun PestanaInicio(estudiante: Estudiante, irA: (Seccion) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Encabezado(estudiante)

        Text("Accesos rápidos", style = MaterialTheme.typography.titleMedium)
        AccesoRapido(Icons.Outlined.Badge, "Usar mi credencial", "Verifica tu identidad y acércate al lector") {
            irA(Seccion.CREDENCIAL)
        }
        AccesoRapido(Icons.Outlined.History, "Mi historial", "Dónde y cuándo usaste tu credencial") {
            irA(Seccion.HISTORIAL)
        }
        AccesoRapido(Icons.Outlined.Person, "Mi perfil", "Consulta y actualiza tus datos") {
            irA(Seccion.PERFIL)
        }

        if (estudiante.descripcion.isNotBlank() || estudiante.intereses.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sobre mí", style = MaterialTheme.typography.titleMedium)
                    if (estudiante.descripcion.isNotBlank()) {
                        Text(
                            estudiante.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (estudiante.intereses.isNotEmpty()) EtiquetasLectura(estudiante.intereses.take(6))
                }
            }
        }
    }
}

@Composable
private fun Encabezado(estudiante: Estudiante) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(VerdeUtez, AzulUtez)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarEstudiante(estudiante.fotoUri, estudiante.nombreCompleto, tamano = 72.dp, colorBorde = Color.White)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Hola, ${estudiante.nombreCompleto.substringBefore(' ')}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(estudiante.matricula, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                Text(
                    estudiante.carrera,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccesoRapido(icono: ImageVector, titulo: String, descripcion: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
