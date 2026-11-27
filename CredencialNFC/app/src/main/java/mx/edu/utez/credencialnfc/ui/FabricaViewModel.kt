package mx.edu.utez.credencialnfc.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mx.edu.utez.credencialnfc.CredencialApp
import mx.edu.utez.credencialnfc.di.ContenedorApp

@Composable
inline fun <reified VM : ViewModel> viewModelConContenedor(
    crossinline crear: (ContenedorApp) -> VM,
): VM {
    val contenedor = (LocalContext.current.applicationContext as CredencialApp).contenedor
    return viewModel(factory = viewModelFactory { initializer { crear(contenedor) } })
}
