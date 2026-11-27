package mx.edu.utez.credencialnfc.ui.screens.arranque

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mx.edu.utez.credencialnfc.domain.repository.SesionRepository
import mx.edu.utez.credencialnfc.ui.components.EmblemaUtez

sealed interface EstadoArranque {
    data object Cargando : EstadoArranque
    data object SinSesion : EstadoArranque
    data object ConSesion : EstadoArranque
}

class ArranqueViewModel(sesion: SesionRepository) : ViewModel() {
    val estado: StateFlow<EstadoArranque> = sesion.estudianteIdActual
        .map { id -> if (id == null) EstadoArranque.SinSesion else EstadoArranque.ConSesion }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoArranque.Cargando)
}

@Composable
fun PantallaArranque(
    viewModel: ArranqueViewModel,
    irABienvenida: () -> Unit,
    irAPrincipal: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado) {
        when (estado) {
            EstadoArranque.SinSesion -> irABienvenida()
            EstadoArranque.ConSesion -> irAPrincipal()
            EstadoArranque.Cargando -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        EmblemaUtez()
    }
}
