package mx.edu.utez.credencialnfc.ui.screens.bienvenida

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import mx.edu.utez.credencialnfc.ui.components.EmblemaUtez
import mx.edu.utez.credencialnfc.ui.theme.AzulUtez
import mx.edu.utez.credencialnfc.ui.theme.CredencialUtezTheme
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtez
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtezOscuro

@Composable
fun PantallaBienvenida(irAIniciarSesion: () -> Unit, irARegistro: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VerdeUtez, VerdeUtezOscuro, AzulUtez))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            EmblemaUtez(tamano = 112.dp)
            Spacer(Modifier.height(28.dp))
            Text(
                "Credencial Digital",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Universidad Tecnológica Emiliano Zapata",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caracteristica(Icons.Outlined.Fingerprint, "Verifica tu identidad con tu huella")
                Caracteristica(Icons.Outlined.Nfc, "Acerca tu celular al lector para entrar")
                Caracteristica(Icons.Outlined.LocationOn, "Consulta dónde usaste tu credencial")
            }
            Spacer(Modifier.weight(1.3f))
            Button(
                onClick = irAIniciarSesion,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VerdeUtezOscuro),
            ) { Text("Iniciar sesión", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = irARegistro,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Text("Crear cuenta", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun Caracteristica(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.92f))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VistaPreviaBienvenida() {
    CredencialUtezTheme { PantallaBienvenida({}, {}) }
}
