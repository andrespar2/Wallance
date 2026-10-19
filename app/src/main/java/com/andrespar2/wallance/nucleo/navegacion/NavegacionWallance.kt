package com.andrespar2.wallance.nucleo.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andrespar2.wallance.funcionalidades.ahorros.PantallaAhorros
import com.andrespar2.wallance.funcionalidades.bienvenida.PantallaBienvenida
import com.andrespar2.wallance.funcionalidades.categorias.PantallaCategorias
import com.andrespar2.wallance.funcionalidades.comun.PantallaEnConstruccion
import com.andrespar2.wallance.funcionalidades.configuracion.PantallaConfiguracion
import com.andrespar2.wallance.funcionalidades.estadisticas.PantallaEstadisticas
import com.andrespar2.wallance.funcionalidades.cuentas.PantallaCuentas
import com.andrespar2.wallance.funcionalidades.divisas.PantallaDivisas
import com.andrespar2.wallance.funcionalidades.inicio.PantallaInicio
import com.andrespar2.wallance.funcionalidades.movimientos.PantallaFormularioMovimiento
import com.andrespar2.wallance.funcionalidades.movimientos.PantallaMovimientos
import com.andrespar2.wallance.funcionalidades.presupuestos.PantallaPresupuestos
import com.andrespar2.wallance.funcionalidades.prestamos.PantallaPrestamos
import com.andrespar2.wallance.funcionalidades.suscripciones.PantallaSuscripciones
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.publicidad.BannerPublicidad
import com.andrespar2.wallance.sesion.SesionViewModel

/** Elemento de la barra de navegación inferior. */
private data class DestinoBarra(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector
)

private val DestinosBarra = listOf(
    DestinoBarra(Rutas.INICIO, "Inicio", Icons.Default.Home),
    DestinoBarra(Rutas.MOVIMIENTOS, "Movimientos", Icons.Default.SwapVert),
    DestinoBarra(Rutas.PRESTAMOS, "Préstamos", Icons.Default.Handshake),
    DestinoBarra(Rutas.ESTADISTICAS, "Estadísticas", Icons.Default.BarChart),
    DestinoBarra(Rutas.CONFIGURACION, "Configuración", Icons.Default.Settings)
)

/** Rutas que se muestran a pantalla completa, sin la barra inferior. */
private val RutasSinBarra = setOf(
    Rutas.BIENVENIDA,
    Rutas.RUTA_MOVIMIENTO_FORM,
    Rutas.MOVIMIENTO_FORM,
    Rutas.CUENTAS,
    Rutas.CATEGORIAS,
    Rutas.PRESUPUESTOS,
    Rutas.AHORROS,
    Rutas.DIVISAS,
    Rutas.SUSCRIPCIONES
)

/**
 * Grafo de navegación completo de Wallance: pantalla de bienvenida, las cinco
 * secciones principales y el formulario de movimientos.
 */
@Composable
fun NavegacionWallance(
    sesion: SesionViewModel,
    navController: NavHostController = rememberNavController()
) {
    val configuracionCompletada by sesion.configuracionCompletada.collectAsStateWithLifecycle()
    val monedaPrincipal by sesion.monedaPrincipal.collectAsStateWithLifecycle()
    val ocultarMontos by sesion.ocultarMontos.collectAsStateWithLifecycle()

    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route
    val mostrarBarra = rutaActual != null && rutaActual !in RutasSinBarra

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        bottomBar = {
            if (mostrarBarra) {
                Column {
                    // Banner publicitario de la pantalla de inicio: siempre
                    // visible sobre la barra inferior. No se muestra en
                    // formularios ni en pantallas sensibles.
                    if (rutaActual == Rutas.INICIO) {
                        BannerPublicidad()
                    }
                    NavigationBar(containerColor = ColoresWallance.NegroSecundario) {
                        DestinosBarra.forEach { destino ->
                            NavigationBarItem(
                                selected = rutaActual == destino.ruta,
                                onClick = {
                                    if (rutaActual != destino.ruta) {
                                        navController.navigate(destino.ruta) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                                label = { Text(destino.etiqueta) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ColoresWallance.NegroAmoled,
                                    selectedTextColor = ColoresWallance.AmarilloPrincipal,
                                    indicatorColor = ColoresWallance.AmarilloPrincipal,
                                    unselectedIconColor = ColoresWallance.TextoSecundario,
                                    unselectedTextColor = ColoresWallance.TextoSecundario
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (rutaActual == Rutas.INICIO || rutaActual == Rutas.MOVIMIENTOS) {
                FloatingActionButton(
                    onClick = { navController.navigate(Rutas.movimientoForm()) },
                    containerColor = ColoresWallance.AmarilloPrincipal,
                    contentColor = ColoresWallance.NegroAmoled
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo movimiento")
                }
            }
        }
    ) { relleno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                // El Scaffold ya aplica el espacio de las barras del sistema;
                // se marca como consumido para que las pantallas con su propia
                // barra superior no lo vuelvan a sumar (dejaba un hueco arriba).
                .consumeWindowInsets(relleno)
                .background(ColoresWallance.NegroAmoled)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (configuracionCompletada) Rutas.INICIO else Rutas.BIENVENIDA
            ) {
                composable(Rutas.BIENVENIDA) {
                    PantallaBienvenida(
                        onTerminado = {
                            sesion.marcarConfiguracionCompletada()
                            navController.navigate(Rutas.INICIO) {
                                popUpTo(Rutas.BIENVENIDA) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Rutas.INICIO) {
                    PantallaInicio(
                        moneda = monedaPrincipal,
                        ocultarMontos = ocultarMontos,
                        onRegistrarMovimiento = { navController.navigate(Rutas.movimientoForm()) },
                        onVerMovimientos = { navController.navigate(Rutas.MOVIMIENTOS) },
                        onVerPrestamos = { navController.navigate(Rutas.PRESTAMOS) }
                    )
                }

                composable(Rutas.MOVIMIENTOS) {
                    PantallaMovimientos(
                        onCrearMovimiento = { navController.navigate(Rutas.movimientoForm()) },
                        onEditarMovimiento = { id -> navController.navigate(Rutas.movimientoForm(id)) },
                        onDuplicarMovimiento = { id ->
                            navController.navigate(Rutas.movimientoForm(id, duplicar = true))
                        }
                    )
                }

                composable(
                    route = Rutas.RUTA_MOVIMIENTO_FORM,
                    arguments = listOf(
                        navArgument(Rutas.ARG_MOVIMIENTO_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument(Rutas.ARG_DUPLICAR) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entrada ->
                    val id = entrada.arguments?.getLong(Rutas.ARG_MOVIMIENTO_ID) ?: -1L
                    val duplicar = entrada.arguments?.getBoolean(Rutas.ARG_DUPLICAR) ?: false
                    PantallaFormularioMovimiento(
                        movimientoId = id.takeIf { it > 0L },
                        duplicar = duplicar,
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Rutas.PRESTAMOS) {
                    PantallaPrestamos(
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Rutas.ESTADISTICAS) {
                    PantallaEstadisticas(
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Rutas.CONFIGURACION) {
                    PantallaConfiguracion(
                        onIrA = { ruta -> navController.navigate(ruta) },
                        monedaPrincipal = monedaPrincipal
                    )
                }

                composable(Rutas.CUENTAS) {
                    PantallaCuentas(onVolver = { navController.popBackStack() })
                }

                composable(Rutas.CATEGORIAS) {
                    PantallaCategorias(onVolver = { navController.popBackStack() })
                }

                composable(Rutas.PRESUPUESTOS) {
                    PantallaPresupuestos(onVolver = { navController.popBackStack() })
                }

                composable(Rutas.AHORROS) {
                    PantallaAhorros(onVolver = { navController.popBackStack() })
                }

                composable(Rutas.DIVISAS) {
                    PantallaDivisas(onVolver = { navController.popBackStack() })
                }

                composable(Rutas.SUSCRIPCIONES) {
                    PantallaSuscripciones(onVolver = { navController.popBackStack() })
                }
            }
        }
    }
}
