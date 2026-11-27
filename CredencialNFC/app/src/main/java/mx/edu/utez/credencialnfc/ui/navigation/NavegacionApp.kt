package mx.edu.utez.credencialnfc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.edu.utez.credencialnfc.ui.screens.arranque.ArranqueViewModel
import mx.edu.utez.credencialnfc.ui.screens.arranque.PantallaArranque
import mx.edu.utez.credencialnfc.ui.screens.auth.IniciarSesionViewModel
import mx.edu.utez.credencialnfc.ui.screens.auth.PantallaIniciarSesion
import mx.edu.utez.credencialnfc.ui.screens.auth.PantallaRegistro
import mx.edu.utez.credencialnfc.ui.screens.auth.RegistroViewModel
import mx.edu.utez.credencialnfc.ui.screens.bienvenida.PantallaBienvenida
import mx.edu.utez.credencialnfc.ui.screens.perfil.EditarPerfilViewModel
import mx.edu.utez.credencialnfc.ui.screens.perfil.PantallaEditarPerfil
import mx.edu.utez.credencialnfc.ui.screens.principal.PantallaPrincipal
import mx.edu.utez.credencialnfc.ui.screens.principal.PrincipalViewModel
import mx.edu.utez.credencialnfc.ui.viewModelConContenedor

object Rutas {
    const val ARRANQUE = "arranque"
    const val BIENVENIDA = "bienvenida"
    const val INICIAR_SESION = "iniciar_sesion"
    const val REGISTRO = "registro"
    const val PRINCIPAL = "principal"
    const val EDITAR_PERFIL = "editar_perfil"
}

@Composable
fun NavegacionApp(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Rutas.ARRANQUE) {
        composable(Rutas.ARRANQUE) {
            PantallaArranque(
                viewModel = viewModelConContenedor { ArranqueViewModel(it.sesionRepository) },
                irABienvenida = { nav.reemplazarTodo(Rutas.BIENVENIDA) },
                irAPrincipal = { nav.reemplazarTodo(Rutas.PRINCIPAL) },
            )
        }
        composable(Rutas.BIENVENIDA) {
            PantallaBienvenida(
                irAIniciarSesion = { nav.navigate(Rutas.INICIAR_SESION) },
                irARegistro = { nav.navigate(Rutas.REGISTRO) },
            )
        }
        composable(Rutas.INICIAR_SESION) {
            PantallaIniciarSesion(
                viewModel = viewModelConContenedor { IniciarSesionViewModel(it.estudianteRepository, it.sesionRepository) },
                regresar = { nav.popBackStack() },
                irARegistro = {
                    nav.navigate(Rutas.REGISTRO) { popUpTo(Rutas.BIENVENIDA) }
                },
                alIniciarSesion = { nav.reemplazarTodo(Rutas.PRINCIPAL) },
            )
        }
        composable(Rutas.REGISTRO) {
            PantallaRegistro(
                viewModel = viewModelConContenedor {
                    RegistroViewModel(it.estudianteRepository, it.sesionRepository, it.gestorFotos)
                },
                regresar = { nav.popBackStack() },
                alRegistrarse = { nav.reemplazarTodo(Rutas.PRINCIPAL) },
            )
        }
        composable(Rutas.PRINCIPAL) {
            PantallaPrincipal(
                viewModel = viewModelConContenedor { PrincipalViewModel(it.sesionRepository, it.estudianteRepository, it.gestorFotos) },
                alCerrarSesion = { nav.reemplazarTodo(Rutas.BIENVENIDA) },
                irAEditarPerfil = { nav.navigate(Rutas.EDITAR_PERFIL) },
            )
        }
        composable(Rutas.EDITAR_PERFIL) {
            PantallaEditarPerfil(
                viewModel = viewModelConContenedor {
                    EditarPerfilViewModel(it.estudianteRepository, it.sesionRepository, it.gestorFotos)
                },
                regresar = { nav.popBackStack() },
            )
        }
    }
}

private fun NavHostController.reemplazarTodo(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
