package mx.edu.utez.credencialnfc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmblemaUtez(
    modifier: Modifier = Modifier,
    tamano: Dp = 96.dp,
    colorFondo: Color = Color.White,
    colorTexto: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(colorFondo, CircleShape)
            .border(3.dp, colorTexto.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "UTEZ",
            color = colorTexto,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (tamano.value * 0.24f).sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = MaterialTheme.shapes.medium,
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = MaterialTheme.shapes.medium,
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun EnConstruccion(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accion != null) {
            Spacer(Modifier.height(24.dp))
            accion()
        }
    }
}
