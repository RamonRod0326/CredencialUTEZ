package mx.edu.utez.credencialnfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = VerdeUtez,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF3E8),
    onPrimaryContainer = VerdeUtezOscuro,
    secondary = AzulUtez,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F3),
    onSecondaryContainer = AzulUtez,
    tertiary = DoradoUtez,
    onTertiary = Color.White,
    background = FondoClaro,
    onBackground = Color(0xFF17201D),
    surface = SuperficieClara,
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFE5ECE9),
    onSurfaceVariant = Color(0xFF55625D),
    outline = Color(0xFFB9C6C1),
    error = ErrorRojo,
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeUtezClaro,
    onPrimary = Color(0xFF00382A),
    primaryContainer = VerdeUtezOscuro,
    onPrimaryContainer = Color(0xFFCFF3E8),
    secondary = AzulUtezClaro,
    onSecondary = Color(0xFF0A2340),
    secondaryContainer = AzulUtez,
    onSecondaryContainer = Color(0xFFDCE6F3),
    tertiary = DoradoUtez,
    background = FondoOscuro,
    onBackground = Color(0xFFE1E8E5),
    surface = SuperficieOscura,
    onSurface = Color(0xFFE1E8E5),
    surfaceVariant = Color(0xFF26302C),
    onSurfaceVariant = Color(0xFFB4C2BC),
    outline = Color(0xFF4A5753),
    error = Color(0xFFFF8A80),
)

private val Formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun CredencialUtezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        shapes = Formas,
        content = content,
    )
}
