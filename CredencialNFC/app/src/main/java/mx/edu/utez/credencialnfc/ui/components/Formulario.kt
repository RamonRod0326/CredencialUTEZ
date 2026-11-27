package mx.edu.utez.credencialnfc.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.None,
    accionIme: ImeAction = ImeAction.Next,
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    habilitado: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = unaLinea,
        minLines = lineasMinimas,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, capitalization = mayusculas, imeAction = accionIme),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun CampoPassword(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    accionIme: ImeAction = ImeAction.Next,
    alConfirmar: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = accionIme),
        keyboardActions = KeyboardActions(onDone = { alConfirmar() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AvatarEstudiante(
    fotoUri: String?,
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 96.dp,
    colorBorde: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(3.dp, colorBorde, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (fotoUri != null) {
            AsyncImage(
                model = fotoUri,
                contentDescription = "Foto de $nombre",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = iniciales(nombre),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = (tamano.value * 0.34f).sp,
            )
        }
    }
}

fun iniciales(nombre: String): String =
    nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
fun SelectorFoto(
    fotoUri: String?,
    nombre: String,
    procesando: Boolean,
    error: String?,
    crearUriCamara: () -> Uri,
    onFotoElegida: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uriPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = uriPendiente
        if (exito && uri != null) onFotoElegida(Uri.parse(uri))
        uriPendiente = null
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onFotoElegida(uri)
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            AvatarEstudiante(
                fotoUri = fotoUri,
                nombre = nombre,
                tamano = 132.dp,
                colorBorde = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (procesando) CircularProgressIndicator()
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val uri = crearUriCamara()
                    uriPendiente = uri.toString()
                    camara.launch(uri)
                },
                enabled = !procesando,
            ) {
                Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Tomar foto")
            }
            OutlinedButton(
                onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !procesando,
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Galería")
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SelectorCarrera(
    carrera: String,
    opciones: List<String>,
    onElegir: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = carrera,
            onValueChange = {},
            readOnly = true,
            label = { Text("Carrera") },
            placeholder = { Text("Selecciona tu carrera") },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = if (error != null) 24.dp else 0.dp)
                .clip(MaterialTheme.shapes.medium)
                .semantics {
                    contentDescription = "Carrera: " + carrera.ifEmpty { "sin seleccionar" }
                    role = Role.Button
                }
                .clickable { abierto = true },
        )
    }
    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text("Selecciona tu carrera") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(opciones) { opcion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onElegir(opcion)
                                    abierto = false
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(selected = opcion == carrera, onClick = null)
                            Spacer(Modifier.size(12.dp))
                            Text(opcion, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { abierto = false }) { Text("Cerrar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorEtiquetas(
    titulo: String,
    sugerencias: List<String>,
    seleccionadas: List<String>,
    onCambio: (List<String>) -> Unit,
    error: String?,
    maximo: Int,
    modifier: Modifier = Modifier,
) {
    var nueva by rememberSaveable { mutableStateOf("") }
    val opciones = (sugerencias + seleccionadas).distinct()
    val lleno = seleccionadas.size >= maximo

    fun agregar() {
        val texto = nueva.trim().replaceFirstChar { it.uppercase() }
        if (texto.isNotEmpty() && seleccionadas.none { it.equals(texto, ignoreCase = true) } && !lleno) {
            onCambio(seleccionadas + texto)
        }
        nueva = ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "${seleccionadas.size}/$maximo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val activa = opcion in seleccionadas
                FilterChip(
                    selected = activa,
                    onClick = {
                        if (activa) onCambio(seleccionadas - opcion)
                        else if (!lleno) onCambio(seleccionadas + opcion)
                    },
                    label = { Text(opcion) },
                    leadingIcon = if (activa) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = nueva,
            onValueChange = { if (it.length <= 30) nueva = it },
            label = { Text("Agregar otro") },
            singleLine = true,
            enabled = !lleno,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { agregar() }),
            trailingIcon = {
                IconButton(onClick = { agregar() }, enabled = nueva.isNotBlank() && !lleno) {
                    Icon(Icons.Filled.Add, "Agregar")
                }
            },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EtiquetasLectura(etiquetas: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        etiquetas.forEach {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
fun MensajeError(texto: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            texto,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
}
