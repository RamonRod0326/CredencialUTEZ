package mx.edu.utez.credencialnfc.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mx.edu.utez.credencialnfc.domain.model.Estudiante
import mx.edu.utez.credencialnfc.ui.theme.AzulUtez
import mx.edu.utez.credencialnfc.ui.theme.DoradoUtez
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtez
import mx.edu.utez.credencialnfc.ui.theme.VerdeUtezOscuro

@Composable
fun TarjetaCredencial(estudiante: Estudiante, activa: Boolean, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "brillo")
    val alfaBorde by transicion.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfaBorde",
    )
    val forma = RoundedCornerShape(24.dp)

    Surface(
        shape = forma,
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .then(if (activa) Modifier.border(3.dp, VerdeUtez.copy(alpha = alfaBorde), forma) else Modifier),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(VerdeUtezOscuro, VerdeUtez, AzulUtez)))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmblemaUtez(tamano = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Universidad Tecnológica Emiliano Zapata",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "CREDENCIAL DE ESTUDIANTE",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(width = 96.dp, height = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5ECE9)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (estudiante.fotoUri != null) {
                        AsyncImage(
                            model = estudiante.fotoUri,
                            contentDescription = "Foto de ${estudiante.nombreCompleto}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        Text(iniciales(estudiante.nombreCompleto), color = VerdeUtez, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        estudiante.nombreCompleto.uppercase(),
                        color = Color(0xFF17201D),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Etiqueta("MATRÍCULA", estudiante.matricula, destacado = true)
                    Etiqueta("CARRERA", estudiante.carrera)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F3))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (activa) VerdeUtez else DoradoUtez))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (activa) "Activa · lista para el lector" else "Vigente · verificación requerida",
                    color = Color(0xFF55625D),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.Contactless, null, tint = if (activa) VerdeUtez else Color(0xFF9AA8A3))
            }
        }
    }
}

@Composable
private fun Etiqueta(titulo: String, valor: String, destacado: Boolean = false) {
    Column {
        Text(titulo, fontSize = 10.sp, letterSpacing = 1.sp, color = Color(0xFF7A8782), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(1.dp))
        Text(
            valor,
            color = if (destacado) VerdeUtezOscuro else Color(0xFF2B3532),
            style = if (destacado) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
